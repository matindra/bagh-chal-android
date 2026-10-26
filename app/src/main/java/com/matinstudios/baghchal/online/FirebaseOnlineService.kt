package com.matinstudios.baghchal.online

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.matinstudios.baghchal.engine.Side
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * Online play on Firebase: anonymous sign-in plus the Realtime Database.
 *
 * Layout (security rules in `firebase/database.rules.json`):
 * - `rooms/{id}`: host, guest, hostSide, status ("waiting" | "playing"), code, createdAt,
 *   moves/{n} = {m, by}, resigned, online/{uid}, rematch = {room, by}. A room nobody joined is
 *   deleted when its host stops waiting; finished rooms are swept later by [RoomCleanup].
 * - Play again after a friend game: the offering player makes the next room already seated
 *   (status "rematch", guest = the friend, sides swapped) and points the old room's `rematch` at it;
 *   the friend accepts by setting it to "playing".
 * - `lobby/random`: the one random-match room waiting for an opponent {room, host, at}.
 * - `codes/{4 digits}`: the private room using that code {room, at}.
 *
 * Every claim (lobby seat, code, joining a room) is a transaction, so two players can never take the
 * same seat. A transaction may first run on an empty local cache; the handlers then commit "no change",
 * which the server rejects and re-runs with the real value.
 */
class FirebaseOnlineService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
) : OnlineService {

    override val isAvailable = true
    override val myUid: String? get() = auth.currentUser?.uid

    private val rooms get() = db.getReference("rooms")
    private val randomSeat get() = db.getReference("lobby/random")
    private val codes get() = db.getReference("codes")
    private fun codeRef(code: String) = codes.child(code)

    /** Server time of this run's last sweep of old rooms and codes. */
    @Volatile private var lastSweepAt: Long? = null

    override suspend fun findRandomMatch(): String = coroutineScope {
        val me = ready()
        val roomRef = rooms.push()
        createWaitingRoom(roomRef, me, Side.entries.random(), code = null)
        val presence = launch { stayOnline(roomRef, me) }
        try {
            matchRandom(roomRef, me)
        } finally {
            presence.cancel()
        }
    }

    private suspend fun matchRandom(roomRef: DatabaseReference, me: String): String {
        val roomId = roomRef.key!!
        var matched: String? = null
        try {
            while (true) {
                val other = takeOrRefreshRandomSeat(roomId, me)
                if (other == null) {
                    // Holding the seat: wait for a guest, then refresh the seat so it doesn't look stale.
                    if (withTimeoutOrNull(SEAT_REFRESH_MILLIS) { awaitGuest(roomRef) } != null) {
                        return roomId.also { matched = it }
                    }
                } else if (deleteIfUnjoined(roomRef)) {
                    if (join(other, me, needHostOnline = true) is JoinResult.Joined) return other.also { matched = it }
                    // That room was gone; offer ours again (it was deleted, so make a fresh one).
                    createWaitingRoom(roomRef, me, Side.entries.random(), code = null)
                } else {
                    return roomId.also { matched = it } // someone joined ours just in time
                }
            }
        } finally {
            if (matched != roomId) withContext(NonCancellable) {
                releaseRandomSeat(roomId)
                // Someone joined just as we stopped looking: concede, so they aren't left waiting.
                if (!deleteIfUnjoined(roomRef)) roomRef.child("resigned").setValue(me)
            }
        }
    }

    override fun hostPrivateGame(hostSide: Side): Flow<HostingUpdate> = channelFlow {
        val me = ready()
        val roomRef = rooms.push()
        val roomId = roomRef.key!!
        createWaitingRoom(roomRef, me, hostSide, code = null)
        // Sharing the code sends the host to another app, which can drop the connection.
        val presence = launch { stayOnline(roomRef, me) }
        var code: String? = null
        var started = false
        try {
            code = claimCode(roomId)
            roomRef.child("code").setValue(code)
            send(HostingUpdate.CodeReady(code))
            awaitGuest(roomRef)
            started = true
            send(HostingUpdate.Started(roomId))
        } finally {
            presence.cancel()
            withContext(NonCancellable) {
                code?.let { releaseCode(it, roomId) }
                if (!started) deleteIfUnjoined(roomRef)
            }
        }
    }

    override suspend fun joinPrivateGame(code: String): JoinResult {
        val me = ready()
        val roomId = codeRef(code).get().awaitOrThrow().child("room").getValue(String::class.java)
            ?: return JoinResult.NotFound
        // The host may still be in the app they shared the code with, so don't require them online.
        return join(roomId, me, needHostOnline = false).also { if (it is JoinResult.Joined) releaseCode(code, roomId) }
    }

    override fun room(roomId: String): Flow<RoomSnapshot?> = callbackFlow {
        val me = myUid
        val roomRef = rooms.child(roomId)
        val presence = me?.let { roomRef.child("online").child(it) }
        val roomListener = roomRef.addValueEventListener(listener { trySend(parseRoom(it)) })
        if (me != null) launch { stayOnline(roomRef, me) }
        awaitClose {
            roomRef.removeEventListener(roomListener)
            presence?.onDisconnect()?.cancel()
            presence?.removeValue()
        }
    }

    override fun sendMove(roomId: String, index: Int, move: String) {
        val me = myUid ?: return
        rooms.child(roomId).child("moves").child(index.toString()).setValue(mapOf("m" to move, "by" to me))
    }

    override fun resign(roomId: String) {
        val me = myUid ?: return
        rooms.child(roomId).child("resigned").setValue(me)
    }

    override suspend fun playAgain(roomId: String): RematchResult {
        val me = ready()
        val room = parseRoom(rooms.child(roomId).get().awaitOrThrow()) ?: return RematchResult.Unavailable
        val friend = room.opponentOf(me) ?: return RematchResult.Unavailable
        val mySide = room.sideOf(me) ?: return RematchResult.Unavailable
        room.rematch?.let { return answer(it, me) }

        // The next room exists before the offer points at it, so the friend can always accept.
        val nextRef = rooms.push()
        val nextId = nextRef.key!!
        nextRef.setValue(
            mapOf(
                "host" to me,
                "guest" to friend,
                "hostSide" to mySide.opponent.name,
                "status" to STATUS_REMATCH,
                "code" to room.code,
                "createdAt" to ServerValue.TIMESTAMP,
                "online" to mapOf(me to true),
            ),
        ).awaitOrThrow()
        var offer: RematchOffer? = null
        rooms.child(roomId).child("rematch").transact { data ->
            offer = readOffer(data.child("room").getValue(String::class.java), data.child("by").getValue(String::class.java))
            if (offer == null) data.value = mapOf("room" to nextId, "by" to me)
            true
        }
        val theirs = offer
        if (theirs == null) return RematchResult.Offered(nextId)
        // The friend offered at the same moment: use theirs.
        nextRef.removeValue()
        return answer(theirs, me)
    }

    /** Our own earlier offer is still ours to wait on; the friend's is accepted. */
    private suspend fun answer(offer: RematchOffer, me: String): RematchResult {
        if (offer.by == me) return RematchResult.Offered(offer.roomId)
        var started = false
        rooms.child(offer.roomId).transact { data ->
            val host = data.child("host").getValue(String::class.java)
            val status = data.child("status").getValue(String::class.java)
            started = data.child("guest").getValue(String::class.java) == me &&
                (status == STATUS_REMATCH || status == STATUS_PLAYING)
            if (started) {
                data.child("status").value = STATUS_PLAYING
                data.child("online").child(me).value = true
            }
            // An empty first run commits "no change" so the server re-runs it with the real room.
            host == null || started
        }
        return if (started) RematchResult.Started(offer.roomId) else RematchResult.Unavailable
    }

    override fun withdrawRematch(roomId: String, nextRoomId: String) {
        rooms.child(nextRoomId).runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val status = currentData.child("status").getValue(String::class.java)
                    if (status == STATUS_REMATCH) currentData.value = null
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    // Only clear the offer if the friend didn't accept it in the meantime.
                    if (error == null && committed && currentData?.exists() == false) {
                        rooms.child(roomId).child("rematch").removeValue()
                    }
                }
            },
        )
    }

    /**
     * Keeps [me] marked online in the room until cancelled. The server drops the mark when the
     * connection goes (e.g. the app is paused in the background), so it is set again on every reconnect.
     */
    private suspend fun stayOnline(roomRef: DatabaseReference, me: String) {
        val mine = roomRef.child("online").child(me)
        db.getReference(".info/connected").values { it.getValue(Boolean::class.java) == true }.collect { connected ->
            if (connected) {
                mine.onDisconnect().removeValue()
                mine.setValue(true)
            }
        }
    }

    /** Signs in (anonymously, once per install) and checks the server can be reached. */
    private suspend fun ready(): String {
        val uid = auth.currentUser?.uid
            ?: auth.signInAnonymously().awaitOrThrow().user?.uid
            ?: throw OnlineException("Sign-in failed")
        val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) {
            db.getReference(".info/connected").values { it.getValue(Boolean::class.java) }.first { it == true }
        }
        if (connected == null) throw OnlineException("No connection")
        sweepOldData()
        return uid
    }

    /** Deletes a batch of old rooms and codes (fire and forget; the next sweep retries failures). */
    private suspend fun sweepOldData() {
        val now = serverNow()
        if (!RoomCleanup.isDue(lastSweepAt, now)) return
        lastSweepAt = now
        deleteOlderThan(rooms, "createdAt", RoomCleanup.roomCutoff(now))
        deleteOlderThan(codes, "at", RoomCleanup.codeCutoff(now))
    }

    private fun deleteOlderThan(parent: DatabaseReference, timeField: String, cutoff: Long) {
        // startAt(1) skips entries without the field: the rules couldn't prove they're old.
        parent.orderByChild(timeField).startAt(1.0).endAt(cutoff.toDouble()).limitToFirst(RoomCleanup.BATCH).get()
            .addOnSuccessListener { snapshot -> snapshot.children.forEach { it.ref.removeValue() } }
    }

    private suspend fun createWaitingRoom(roomRef: DatabaseReference, me: String, hostSide: Side, code: String?) {
        roomRef.setValue(
            mapOf(
                "host" to me,
                "hostSide" to hostSide.name,
                "status" to STATUS_WAITING,
                "code" to code,
                "createdAt" to ServerValue.TIMESTAMP,
                "online" to mapOf(me to true),
            ),
        ).awaitOrThrow()
        roomRef.child("online").child(me).onDisconnect().removeValue()
    }

    /**
     * Takes the waiting random room if there is a fresh one from someone else (returns its id), or
     * puts ours in the seat / refreshes it (returns null).
     */
    private suspend fun takeOrRefreshRandomSeat(roomId: String, me: String): String? {
        val now = serverNow()
        var taken: String? = null
        randomSeat.transact { data ->
            val waiting = data.child("room").getValue(String::class.java)
            val host = data.child("host").getValue(String::class.java)
            val at = data.child("at").getValue(Long::class.java) ?: 0L
            if (waiting != null && waiting != roomId && host != me && now - at < SEAT_STALE_MILLIS) {
                taken = waiting
                data.value = null
            } else {
                taken = null
                data.value = mapOf("room" to roomId, "host" to me, "at" to now)
            }
            true
        }
        return taken
    }

    private suspend fun releaseRandomSeat(roomId: String) {
        randomSeat.transact { data ->
            if (data.child("room").getValue(String::class.java) == roomId) data.value = null
            true
        }
    }

    /** Claims a free 4-digit code for [roomId]; codes left behind for hours count as free. */
    private suspend fun claimCode(roomId: String): String {
        val now = serverNow()
        repeat(CODE_ATTEMPTS) {
            val code = RoomCode.random(Random.Default)
            var claimed = false
            codeRef(code).transact { data ->
                val at = data.child("at").getValue(Long::class.java)
                claimed = data.value == null || (at != null && now - at > CODE_STALE_MILLIS)
                if (claimed) data.value = mapOf("room" to roomId, "at" to now)
                true
            }
            if (claimed) return code
        }
        throw OnlineException("No free code")
    }

    private suspend fun releaseCode(code: String, roomId: String) {
        codeRef(code).transact { data ->
            if (data.child("room").getValue(String::class.java) == roomId) data.value = null
            true
        }
    }

    /** Takes the guest seat of a waiting room (whose host is connected, if [needHostOnline]). */
    private suspend fun join(roomId: String, me: String, needHostOnline: Boolean): JoinResult {
        var result: JoinResult = JoinResult.NotFound
        rooms.child(roomId).transact { data ->
            val host = data.child("host").getValue(String::class.java)
            val status = data.child("status").getValue(String::class.java)
            val guest = data.child("guest").getValue(String::class.java)
            val hostHere = host != null && data.child("online").hasChild(host)
            result = when {
                host == null || host == me || (needHostOnline && !hostHere) -> JoinResult.NotFound
                guest != null || status != STATUS_WAITING -> JoinResult.Full
                else -> JoinResult.Joined(roomId)
            }
            if (result is JoinResult.Joined) {
                data.child("guest").value = me
                data.child("status").value = STATUS_PLAYING
                data.child("online").child(me).value = true
            }
            // An empty first run commits "no change" so the server re-runs it with the real room.
            host == null || result is JoinResult.Joined
        }
        // Aborted runs never chose Joined, so the last result is the answer either way.
        return result
    }

    /** Deletes our waiting room unless a guest has joined; true if it was deleted (or is gone). */
    private suspend fun deleteIfUnjoined(roomRef: DatabaseReference): Boolean {
        var deleted = false
        roomRef.transact { data ->
            deleted = data.child("guest").value == null
            if (deleted && data.value != null) data.value = null
            true
        }
        return deleted
    }

    private suspend fun awaitGuest(roomRef: DatabaseReference) {
        roomRef.values { snapshot ->
            snapshot.takeIf {
                it.child("status").getValue(String::class.java) == STATUS_PLAYING && it.child("guest").exists()
            }
        }.filterNotNull().first()
    }

    private suspend fun serverNow(): Long {
        val offset = db.getReference(".info/serverTimeOffset").values { it.getValue(Long::class.java) ?: 0L }.first()
        return System.currentTimeMillis() + offset
    }

    private fun parseRoom(snapshot: DataSnapshot): RoomSnapshot? {
        if (!snapshot.exists()) return null
        val host = snapshot.child("host").getValue(String::class.java) ?: return null
        val hostSide = Side.entries.firstOrNull { it.name == snapshot.child("hostSide").getValue(String::class.java) }
            ?: return null
        val moves = snapshot.child("moves").children
            .mapNotNull { child -> child.key?.toIntOrNull()?.let { it to child } }
            .sortedBy { it.first }
            .map { (_, child) ->
                RoomMove(
                    move = child.child("m").getValue(String::class.java).orEmpty(),
                    by = child.child("by").getValue(String::class.java).orEmpty(),
                )
            }
        return RoomSnapshot(
            id = snapshot.key.orEmpty(),
            hostUid = host,
            guestUid = snapshot.child("guest").getValue(String::class.java),
            hostSide = hostSide,
            isPlaying = snapshot.child("status").getValue(String::class.java) == STATUS_PLAYING,
            code = snapshot.child("code").getValue(String::class.java),
            moves = moves,
            resignedBy = snapshot.child("resigned").getValue(String::class.java),
            online = snapshot.child("online").children.mapNotNull { it.key }.toSet(),
            rematch = readOffer(
                snapshot.child("rematch").child("room").getValue(String::class.java),
                snapshot.child("rematch").child("by").getValue(String::class.java),
            ),
        )
    }

    private companion object {
        const val STATUS_WAITING = "waiting"
        const val STATUS_PLAYING = "playing"
        const val STATUS_REMATCH = "rematch"
        const val CONNECT_TIMEOUT_MILLIS = 10_000L
        const val SEAT_REFRESH_MILLIS = 20_000L
        const val SEAT_STALE_MILLIS = 60_000L
        const val CODE_STALE_MILLIS = RoomCleanup.CODE_MAX_AGE_MILLIS
        const val CODE_ATTEMPTS = 20
    }
}

private fun readOffer(room: String?, by: String?): RematchOffer? =
    if (room != null && by != null) RematchOffer(room, by) else null

private fun listener(onData: (DataSnapshot) -> Unit) = object : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) = onData(snapshot)
    override fun onCancelled(error: DatabaseError) = Unit
}

/** Every value at this location, mapped by [read]; ends with an [OnlineException] if access is refused. */
private fun <T> DatabaseReference.values(read: (DataSnapshot) -> T): Flow<T> = callbackFlow {
    val listener = addValueEventListener(
        object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(read(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                close(OnlineException(error.message, error.toException()))
            }
        },
    )
    awaitClose { removeEventListener(listener) }
}

/**
 * Runs a transaction; [update] edits the data and returns whether to commit. Returns whether it
 * committed. The values [update] captures are those of its last run, i.e. the committed one.
 */
private suspend fun DatabaseReference.transact(update: (MutableData) -> Boolean): Boolean =
    suspendCancellableCoroutine { continuation ->
        runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result =
                    if (update(currentData)) Transaction.success(currentData) else Transaction.abort()

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(OnlineException(error.message, error.toException()))
                    } else {
                        continuation.resume(committed)
                    }
                }
            },
        )
    }

private suspend fun <T> Task<T>.awaitOrThrow(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            continuation.resume(task.result as T)
        } else {
            continuation.resumeWithException(OnlineException("Request failed", task.exception))
        }
    }
}
