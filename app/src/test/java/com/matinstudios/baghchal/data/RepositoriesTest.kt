package com.matinstudios.baghchal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.matinstudios.baghchal.engine.DrawReason
import com.matinstudios.baghchal.engine.GameStatus
import com.matinstudios.baghchal.engine.Move
import com.matinstudios.baghchal.engine.Rules
import com.matinstudios.baghchal.engine.Side
import com.matinstudios.baghchal.engine.WinReason
import com.matinstudios.baghchal.engine.ai.Difficulty
import com.matinstudios.baghchal.engine.ai.randomPositions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositoriesTest {

    @TempDir
    lateinit var dir: File

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @AfterEach
    fun tearDown() = scope.cancel()

    private fun dataStore(name: String = "test"): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(dir, "$name.preferences_pb") })

    @Test
    fun `saved game is stored, replaced and cleared`() = runBlocking {
        val repository = SavedGameRepository(dataStore())
        assertNull(repository.load())

        val first = SavedGame(Rules.apply(Rules.initialState(), Move.Place(12)), Side.TIGER, Difficulty.HARD)
        repository.save(first)
        assertEquals(first, repository.load())

        val second = SavedGame(Rules.apply(first.game, Move.Step(0, 5)), Side.TIGER, Difficulty.HARD)
        repository.save(second)
        assertEquals(second, repository.load())

        repository.clear()
        assertNull(repository.load())
    }

    @Test
    fun `stats count results per side and difficulty`() = runBlocking {
        val repository = StatsRepository(dataStore())
        repository.record(Side.GOAT, Difficulty.EASY, Outcome.WIN)
        repository.record(Side.GOAT, Difficulty.EASY, Outcome.WIN)
        repository.record(Side.GOAT, Difficulty.HARD, Outcome.LOSS)
        repository.record(Side.TIGER, Difficulty.EASY, Outcome.DRAW)

        val stats = repository.stats.first()
        assertEquals(Record(wins = 2), stats.record(Side.GOAT, Difficulty.EASY))
        assertEquals(Record(losses = 1), stats.record(Side.GOAT, Difficulty.HARD))
        assertEquals(Record(), stats.record(Side.GOAT, Difficulty.MEDIUM))
        assertEquals(Record(draws = 1), stats.record(Side.TIGER, Difficulty.EASY))
        assertEquals(Record(wins = 2, losses = 1), stats.total(Side.GOAT))
        assertEquals(3, stats.total(Side.GOAT).played)

        repository.reset()
        val reset = repository.stats.first()
        assertEquals(Record(), reset.total(Side.GOAT))
        assertEquals(Record(), reset.total(Side.TIGER))
    }

    @Test
    fun `saved game and stats share a store without clobbering each other`() = runBlocking {
        val store = dataStore()
        val games = SavedGameRepository(store)
        val stats = StatsRepository(store)
        val saved = SavedGame(Rules.apply(Rules.initialState(), Move.Place(6)), null, Difficulty.MEDIUM)
        games.save(saved)
        stats.record(Side.TIGER, Difficulty.MEDIUM, Outcome.WIN)
        stats.reset()
        assertEquals(saved, games.load())
        games.clear()
        stats.record(Side.TIGER, Difficulty.MEDIUM, Outcome.WIN)
        assertEquals(Record(wins = 1), stats.stats.first().record(Side.TIGER, Difficulty.MEDIUM))
    }

    @Test
    fun `serial writer applies writes in submission order`() = runBlocking {
        val repository = SavedGameRepository(dataStore())
        val writer = SerialWriter(scope)
        val saves = randomPositions(10).map { SavedGame(it, Side.TIGER, Difficulty.EASY) }
        for (saved in saves) writer.submit { repository.save(saved) }
        writer.submit { repository.clear() }
        writer.submit { repository.save(saves.first()) }
        val done = CompletableDeferred<Unit>()
        writer.submit { done.complete(Unit) }

        withTimeout(5_000) { done.await() }
        assertEquals(saves.first(), repository.load(), "last submitted write wins")
    }

    @Test
    fun `tutorial starts unseen and remembers being seen`() = runBlocking {
        val store = dataStore()
        assertEquals(false, SettingsRepository(store).tutorialSeen.first())
        SettingsRepository(store).setTutorialSeen(true)
        assertEquals(true, SettingsRepository(store).tutorialSeen.first())
    }

    @Test
    fun `settings default to on and persist changes`() = runBlocking {
        val repository = SettingsRepository(dataStore())
        assertEquals(Settings(soundEnabled = true, hapticsEnabled = true), repository.settings.first())
        repository.setHapticsEnabled(false)
        assertEquals(Settings(soundEnabled = true, hapticsEnabled = false), repository.settings.first())
        repository.setSoundEnabled(false)
        repository.setHapticsEnabled(true)
        assertEquals(Settings(soundEnabled = false, hapticsEnabled = true), repository.settings.first())
    }

    @Test
    fun `music defaults to on and persists`() = runBlocking {
        val store = dataStore()
        assertEquals(true, SettingsRepository(store).settings.first().musicEnabled)
        SettingsRepository(store).setMusicEnabled(false)
        val settings = SettingsRepository(store).settings.first()
        assertEquals(false, settings.musicEnabled)
        assertEquals(true, settings.soundEnabled, "music and sound effects are separate switches")
    }

    @Test
    fun `theme mode defaults to system and persists`() = runBlocking {
        val store = dataStore()
        assertEquals(ThemeMode.SYSTEM, SettingsRepository(store).settings.first().themeMode)
        for (mode in ThemeMode.entries) {
            SettingsRepository(store).setThemeMode(mode)
            assertEquals(mode, SettingsRepository(store).settings.first().themeMode)
        }
    }

    @Test
    fun `board theme defaults to wood, persists, and ignores unknown values`() = runBlocking {
        val store = dataStore()
        val repository = SettingsRepository(store)
        assertEquals(BoardTheme.WOOD, repository.settings.first().boardTheme)
        for (theme in BoardTheme.entries) {
            repository.setBoardTheme(theme)
            assertEquals(theme, repository.settings.first().boardTheme)
        }
        store.edit { it[stringPreferencesKey("settings_board_theme")] = "MARBLE" }
        assertEquals(BoardTheme.WOOD, repository.settings.first().boardTheme)
    }

    @Test
    fun `piece set defaults to classic, persists, and ignores unknown values`() = runBlocking {
        val store = dataStore()
        val repository = SettingsRepository(store)
        assertEquals(PieceSet.CLASSIC, repository.settings.first().pieceSet)
        for (set in PieceSet.entries) {
            repository.setPieceSet(set)
            assertEquals(set, repository.settings.first().pieceSet)
        }
        store.edit { it[stringPreferencesKey("settings_piece_set")] = "PLASTIC" }
        assertEquals(PieceSet.CLASSIC, repository.settings.first().pieceSet)
    }

    @Test
    fun `outcome is from the player's point of view`() {
        val tigersWin = GameStatus.Won(Side.TIGER, WinReason.CAPTURES)
        assertEquals(Outcome.WIN, Outcome.of(tigersWin, Side.TIGER))
        assertEquals(Outcome.LOSS, Outcome.of(tigersWin, Side.GOAT))
        assertEquals(Outcome.DRAW, Outcome.of(GameStatus.Drawn(DrawReason.REPETITION), Side.GOAT))
        assertNull(Outcome.of(GameStatus.InProgress, Side.GOAT))
    }

    @Test
    fun `entitlements are stored and reset`() = runBlocking {
        val repository = EntitlementRepository(dataStore())
        assertEquals(Entitlements(), repository.entitlements.first())

        repository.unlockTheme(BoardTheme.STONE)
        repository.unlockTheme(BoardTheme.CLOTH)
        repository.unlockPieceSet(PieceSet.GLYPH)
        repository.setRemoveAds(true)
        val rewarded = Entitlements(
            unlockedThemes = setOf(BoardTheme.STONE, BoardTheme.CLOTH),
            unlockedPieceSets = setOf(PieceSet.GLYPH),
        )
        assertEquals(rewarded.copy(removeAds = true), repository.entitlements.first())

        repository.setRemoveAds(false)
        assertEquals(rewarded, repository.entitlements.first(), "a refund keeps rewarded unlocks")

        repository.reset()
        assertEquals(Entitlements(), repository.entitlements.first())
    }
}
