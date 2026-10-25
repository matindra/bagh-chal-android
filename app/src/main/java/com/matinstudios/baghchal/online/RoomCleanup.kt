package com.matinstudios.baghchal.online

/**
 * When old online data gets deleted. There is no server code, so the apps clean up after each other:
 * on an online action, a phone deletes a small batch of rooms and codes past their age limit.
 *
 * The ages must match `firebase/database.rules.json`, which only lets clients list and delete rooms
 * and codes older than these.
 */
object RoomCleanup {
    /** A room this old is long finished (a disconnected player loses after 45 s). */
    const val ROOM_MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L

    /** Same age after which a code counts as free to claim again. */
    const val CODE_MAX_AGE_MILLIS = 6 * 60 * 60 * 1000L

    /** Max entries deleted per sweep; the rules refuse bigger queries. */
    const val BATCH = 20

    /** Sweep at most this often per app run. */
    const val SWEEP_INTERVAL_MILLIS = 10 * 60 * 1000L

    /** Extra age so a phone whose server-time estimate runs a little fast still passes the rules. */
    const val CLOCK_MARGIN_MILLIS = 60 * 1000L

    fun isDue(lastSweepAt: Long?, now: Long): Boolean =
        lastSweepAt == null || now - lastSweepAt >= SWEEP_INTERVAL_MILLIS

    /** Rooms created at or before this server time get deleted. */
    fun roomCutoff(now: Long): Long = now - ROOM_MAX_AGE_MILLIS - CLOCK_MARGIN_MILLIS

    /** Codes claimed at or before this server time get deleted. */
    fun codeCutoff(now: Long): Long = now - CODE_MAX_AGE_MILLIS - CLOCK_MARGIN_MILLIS
}
