# Handoff

## Step 4 backlog (polish)
- ~~Animations~~, ~~haptics~~ and ~~Settings toggles~~: done in session 7 (4b).
- ~~Sound effects~~: done in session 12.
- ~~How to Play screen~~: done in session 11.
- ~~Save/resume~~ and ~~stats~~: done in session 6 (4a). Still open: persist last chosen side/difficulty on Home.
- ~~TalkBack board points~~ and ~~Compose UI tests~~: done in session 9.

## Session 2026-10-25 (15) — ad units per placement
- local.properties keys are now `ADMOB_APP_ID`, `ADMOB_BANNER_HOME_ID`, `ADMOB_BANNER_GAME_ID`,
  `ADMOB_INTERSTITIAL_GAMEEND_ID`, `ADMOB_REWARDED_UNDO_ID`, `ADMOB_REWARDED_HINT_ID`,
  `ADMOB_REWARDED_THEME_ID` (BuildConfig fields of the same names). Checked: release picks them up,
  debug keeps Google's test IDs. The real IDs are in the (git-ignored) local.properties already.
- Home and game banners use their own units; Undo, Hint and theme unlock each preload and show their
  own rewarded unit, and each button's "(ad)" label follows its own unit.
- Banner size switched to `getLargeAnchoredAdaptiveBannerAdSize` (the anchored-adaptive call is
  deprecated in Ads SDK 25). It is taller (~2×); on the test phone the board still fills the width,
  but on short phones the board shrinks a little.

## Session 2026-10-24 (14) — step 5: monetisation
### Done
- Libraries (all named in CLAUDE.md's stack): play-services-ads 25.5.0, UMP 4.0.0, billing-ktx 9.1.0.
- **AdMob IDs**: `app/build.gradle.kts` puts them in `BuildConfig` + the manifest placeholder `admobAppId`.
  Debug always uses Google's test IDs; release reads them from `local.properties` (keys: see session 15)
  and **falls back to the test IDs if any are missing** (no revenue, but no policy risk).
- **Consent** (`ads/ConsentManager`): UMP on every launch from `MainActivity.onCreate`; Mobile Ads is
  initialised (background thread, once per process) only after `canRequestAds`. Settings shows
  "Privacy options" when UMP says it's required. Debug-only testing: `ump.forceEea=true` and
  `ump.testDeviceId=<hashed id printed by UMP in logcat>` in local.properties force the EEA form.
- **Ads** (`ads/MobileAdsManager`, behind the `Ads` interface): anchored adaptive banner (`BannerAd`) at
  the bottom of Home and of the game screen (below Undo/Hint, never over the board). Interstitial only
  when leaving the result screen, capped by `InterstitialPolicy` (every 2nd finished game, ≥3 min apart,
  also ≥3 min after launch). Rewarded + interstitial preloaded; failures retried after 60 s, otherwise ignored.
- **Undo / Hint** (new): `GameUiState.history` (positions played on this screen) — vs computer, Undo
  goes back to the player's previous turn (drops the AI's reply or cancels its search); 2-player,
  one move. Hint = medium-depth search for the side to move, drawn as an arrow (a ring for placements),
  announced on the two points for TalkBack, cleared by the next move.
- **Rewarded rules** (`RewardOutcome`): free users see "Undo (ad)" / "Hint (ad)" only while a rewarded
  ad is loaded; then it's watched first. Skipping the ad withholds the action; **no ad available ⇒ the
  action is free** (offline-first). Theme unlock is permanent and needs a fully watched ad (no ad ⇒
  "No ad is available" message).
- **Entitlements** (`data/EntitlementRepository`): `removeAds` cache + rewarded-unlocked themes in
  DataStore. Wood free; Stone/Cloth locked (lock badge + "Watch ad to unlock") unless unlocked or Remove
  Ads. The drawn theme is `AppContainer.boardTheme` (falls back to Wood if the choice is locked).
- **Remove Ads** (`billing/BillingManager`): product `remove_ads`, pending purchases supported,
  acknowledged, restored on every launch and by "Restore Purchase"; a full purchase list revokes it
  (refund), a single purchase update never does. Ownership rules in `RemoveAdsOwnership` (unit-tested).
  Remove Ads hides banners/interstitials, makes Undo/Hint free and unlocks all boards.
- `CurrentActivity` (activity lifecycle callbacks) gives ads/billing the resumed activity.
- Instrumented tests run on `TestBaghchalApplication` via `BaghchalTestRunner`: no consent, ads or
  billing (`AppContainer.servicesEnabled = false`, `NoAds`).
- Tests: +26 unit (UndoHintTest, EntitlementsTest, InterstitialPolicyTest, RewardOutcomeTest,
  RemoveAdsOwnershipTest, entitlement repository) → 143; +8 instrumented → 39. All green.
- Checked on the emulator with test ads: Home + game banners, rewarded Undo and Hint ("Reward granted",
  move taken back / hint shown), rewarded Stone unlock, EEA consent form on first launch and from
  Privacy options, Remove Ads/Restore messages without Play Billing.

### Not verified / to do
- The interstitial was not seen on a device (needs 2 finished games and 3 minutes); the cap is unit-tested.
- The real purchase flow needs the app in Play Console (internal testing track) with an active
  one-time product `remove_ads`, and a licence-tester account on the device. The emulator here has no
  working Play Billing ("API version 3 is not supported").
- Release: R8 is still off (Ads/Billing ship their own
  consumer rules); add the privacy policy URL; Data safety form must declare the Ads SDK's device IDs.
- Undo history isn't saved, so a resumed game starts with nothing to undo.
- Haptics still fire for moves while backgrounded (from session 12).

## Session 2026-10-24 (13) — drop the place sound
- Placing a goat now plays `move` like any step; `place.ogg` and `SoundEffect.PLACE` removed and
  `place.wav` dropped from `scripts/convert-sounds.sh` (`design/sounds/place.wav` kept as an unused source).

## Session 2026-10-24 (12) — sound effects

### Done
- `scripts/convert-sounds.sh`: design/sounds → `res/raw/*.ogg`: mono 44.1 kHz, silence trimmed
  (-50 dB), loudness normalised to -18 LUFS (measured with the clip looped, since EBU R128 needs
  ≥ 400 ms) with a -1 dBFS peak limiter, OGG Vorbis q4 via `oggenc` (this Homebrew ffmpeg has no
  libvorbis, so `brew install vorbis-tools`). Output is reproducible (fixed Ogg serial).
  `play_win.wav` is mapped to `player_win.ogg`.
- `ui/sound/SoundPlayer.kt`: `SoundEffect` enum + `SoundPoolPlayer` (USAGE_GAME, 4 streams), created
  in `AppContainer` at app start so all sounds preload. `MainActivity.onStart/onStop` set
  foreground; in the background nothing plays and current sounds are paused.
- `GameFeedback.kt`: one sound per move by priority: game end (vs computer: player_win if the player
  won, player_lose if not; 2-player: always player_win) > a tiger newly trapped (goat_capture) >
  tiger_capture / move. Draws vs the computer just play the move sound. Filtered by the
  Sound setting; haptics unchanged (now `HapticCue`). The ViewModel plays sounds directly.
- Tests: `GameFeedbackTest` rewritten (13). 117 unit + 31 instrumented, all green.
- Checked on emulator via `dumpsys audio`: SoundPool registered at startup, one `started` event per
  move, none while backgrounded (computer moved), none with Sound off, back on when re-enabled.

### Known issues
- Haptic cues are still delivered for moves made while backgrounded (sound is not).

## Session 2026-10-24 (11) — How to Play

### Done
- `ui/screens/HowToPlaySteps.kt`: the five rules examples as data (`RuleIllustration`: pieces before
  the move + the move; derives pieces after and trapped tigers). Kept sparse for clarity, so they are
  not necessarily reachable positions; `HowToPlayStepsTest` checks every move is legal geometry
  (right piece, along a line, capture over a goat onto an empty point) and that only the trap step
  locks a tiger, and only because of its move.
- `ui/board/RuleBoard.kt`: static mini board in the selected theme: position after the move, moving
  piece faded at its origin (and the captured goat faded), lock badge on trapped tigers, and a
  yellow arrow with a dark outline (readable on all themes). Placement arrows come in from the
  board's bottom edge; capture arrows arc over the jumped goat.
- `ui/board/MoveArrow.kt` (pure arrow geometry) + `MoveArrowTest`.
- `HowToPlayScreen` (goal line, 5 numbered cards with headings, described pictures, draw rule,
  Back) via `HowToPlayViewModel` for the board theme; "How to play" button on Home.
- Tests: 110 unit + 31 instrumented, all green. Checked on emulator with Wood and Cloth.

### Note
- Commit `beb78d6` (sessions 6–10) is still not pushed: GitHub was unreachable from this network.

## Session 2026-10-24 (10) — result screen + lock badge

### Done
- Converted `result_bg_tiger`, `result_bg_goat`, `lock_badge` (mapping added to `scripts/SvgConvert.java`;
  existing drawables regenerated byte-identical).
- Engine: `GameState.moveCount` (every turn, either side) and `Rules.trappedTigers()` (tigers with no
  step and no capture, regardless of whose turn). Saved-game codec bumped to **v2** (adds move count);
  pre-release v1 saves are no longer read.
- Result screen (`ui/screens/ResultScreen.kt`, route `result?winner=…&moves=…&ai=…&difficulty=…`):
  opens 1.2 s after a win (`GameViewModel.results`), replacing the game in the back stack, so Back
  and Home go to the start screen. Play Again starts a new game with the same setup.
  - Tigers Win: `result_bg_tiger` + large `ic_tiger` with a springy pop-in.
  - Goats Win: `result_bg_goat`, small `ic_tiger` at the back with `lock_badge` top-right, three
    `ic_goat` in front rising from below one after another (left, right, middle).
  - Title, "N moves" (plurals), Home / Play Again. Art starts 350 ms in so the screen fade doesn't hide it.
- **Draws** don't use the result screen (no art specified): they keep the on-board result + Menu /
  New game. Wins no longer show those buttons on the game screen.
- Lock badge drawn top-right on every trapped tiger during play; TalkBack says "trapped tiger".
- Tests: engine (move count, trapped tigers), codec v2, `GameResultTest` (5); instrumented
  `ResultScreenTest` (4), updated `GameContentTest`, 3 new `AppFlowTest` result flows (seed a saved
  game one move from a win → Resume → win → result → Play Again / Home / Back).
  100 unit + 27 instrumented tests, all green.
- Checked on emulator (incl. 10× animation scale): badges on trapped tigers, both result screens,
  staggered goats, tiger overshoot.

### Open questions
- Should draws get a result screen too (needs art)?
- Result screen shows no "You win / You lose" line vs the computer; the game screen does briefly.

## Session 2026-10-23 (9) — TalkBack board + Compose UI tests

### Done
- `BoardCanvas`: 25 invisible accessibility nodes over the points, in reading order: description
  "Row r, column c, goat/tiger/empty", `selected` for the picked-up piece, state "possible move" for
  targets, and an accessibility click action that plays the point. They have no pointer input, so
  normal taps still go to the canvas. Test tags: `boardPointTestTag(point)`.
- **Gotcha:** the nodes must be drawn *above* the canvas. Compose drops accessibility nodes that a
  later sibling fully covers; the Compose test tree still shows them, so only a real
  accessibility-API check catches this.
- `androidTest/.../ui/GameContentTest.kt` (12): the screen rendered from hand-built states: point
  descriptions/selection/possible moves, accessibility click and touch both reach `onPointTapped`,
  status + counters, thinking indicator on/off, game-over result text (win/lose/2-player) and
  Menu/New game buttons, no buttons mid-game.
- `androidTest/.../ui/AppFlowTest.kt` (5): the real app with stored data reset before each test:
  Home → 2-player, tap-to-move, vs computer as Tigers on Hard (thinking indicator, then the AI's goat
  and the player's turn), Resume after Back, and the board through Android's accessibility API
  (`UiAutomation`: 25 nodes, ACTION_CLICK places a goat).
- Run with `./gradlew connectedDebugAndroidTest` (needs a running emulator/device; it uninstalls the
  app afterwards). 18 instrumented + 89 unit tests, all green; the flaky cases were run repeatedly.

### Test-writing notes
- While "Thinking…" shows, the spinner animates forever and Compose never goes idle: drive the clock
  manually (`mainClock.autoAdvance = false` + `advanceTimeByFrame()`), as `AppFlowTest` does.
- Accessibility-tree reads must poll: the tree is built lazily and updated asynchronously.
- Not tested with the TalkBack app itself, only through the same accessibility API it uses.

## Session 2026-10-23 (8) — step 4c: board themes

### Done
- `BoardTheme` (WOOD, STONE, CLOTH) in `data/SettingsRepository.kt`; stored as `settings_board_theme`,
  default Wood, unknown values fall back to Wood.
- `BoardCanvas` takes a `theme`; `BoardTheme.drawable` maps to `board_wood/stone/cloth`.
  `GameViewModel.boardTheme` is null until settings load, so the board never flashes Wood first.
- Settings: "Board" card with three thumbnails (selectable group, radio-button semantics,
  highlighted border + bold label for the selection). Settings screen now scrolls.
- All themes unlocked for now. **Step 5:** gate Stone and Cloth (rewarded ad or Remove Ads);
  the picker is the place to show a lock and the unlock action.
- Tests: board theme default/persist/unknown-value test. 89 unit tests, all green.
- Checked on emulator: picker, Cloth and Stone in game (pieces on points), targets visible on Stone.

## Session 2026-10-23 (7) — step 4b: animations + feedback

### Done
- `GameUiState.lastMove` + `moveNumber` (each move animated exactly once; selection keeps them).
- `ui/board/PieceMotion.kt` (pure): steps slide 250 ms; jumps arc 420 ms with the tiger growing
  at the top of the arc (arc capped so it never leaves the board on the top row); captured goat
  fades out; placed goats pop in over 200 ms. `BoardCanvas` drives one `Animatable` per move and
  hides target dots until the move lands. Respects the system animation-scale setting.
- Feedback: `GameFeedback.kt` maps moves to `GOAT_PLACED` / `GOAT_CAPTURED` (moves by either
  side), filtered by settings in the ViewModel and sent through a `Channel`; `GameScreen` performs
  Compose haptics: ContextClick (light tick) for placement, LongPress (heavy click) for capture.
- `data/SettingsRepository` (sound, haptics; both default on) + Settings screen with two switches
  (whole row toggles, exposed as a Switch to accessibility), linked from Home next to Stats.
- Tests: `PieceMotionTest` (6), `GameFeedbackTest` (5), settings repository test. 88 unit tests, all green.
- Checked on emulator at 10× animation scale: mid-jump arc + goat fade, mid-step slide; vibrator
  log shows TICK on placement and HEAVY_CLICK on capture, nothing after turning Haptics off.

## Session 2026-10-23 (6) — step 4a: save/resume + stats

### Done
- `data/`: `SavedGameCodec` (versioned pipe-separated text, no new libraries), `SavedGameRepository`,
  `StatsRepository` (+ `Outcome`, `Record`, `Stats`), `SerialWriter` (ordered writes on an app-wide
  scope), `AppContainer`; one Preferences DataStore file `baghchal`.
- `BaghchalApplication` registered in the manifest; ViewModels get repositories through factories.
- Game auto-saves after every move. A game with no moves yet clears the save (so starting a new
  game replaces the old one). Finishing a game clears the save; vs computer the result is recorded
  once, from the player's side. 2-player games are saved/resumable but never counted in stats.
- After process death the game screen resumes from storage (`resume` flag set in SavedStateHandle).
- Home: "Game in progress" card with mode, difficulty, progress and Resume; Stats button.
- Stats screen: Won/Lost/Drawn per difficulty for Goats and Tigers, totals, Reset with confirmation.
- Tests: `SavedGameCodecTest` (3), `RepositoriesTest` (5, real DataStore in a temp dir),
  `GamePersistenceTest` (6). 76 unit tests, all green.
- Checked on emulator: Resume card, force-stop + resume, background process kill + reopen, Stats screen.

### Known issues / follow-ups
- Starting a new game from Home silently replaces an in-progress save (no confirmation).
- Abandoned games (leaving mid-game and starting another) are not counted as losses.
- Result recording to stats was verified by unit tests, not by finishing a game on device.

## Session 2026-10-23 (5) — orientation
- Phones (smallest width < 600dp) are locked to portrait in `MainActivity.lockPhonesToPortrait()`;
  tablets and unfolded foldables can rotate.

## Session 2026-10-23 (4) — roadmap step 3: AI + start screen

### Done
- `engine/ai/` (pure Kotlin):
  - `SearchBoard`: mutable board with make/unmake and an incrementally updated position key;
    move generation mirrors `Rules` (tested against it on 200 random positions).
  - `Evaluator`: tiger-view score: captured ×100, tiger mobility ×3, capturable goats ×25,
    trapped tigers −25, goats on edge −2.
  - `Search`: negamax + alpha-beta, iterative deepening, transposition table, TT-move then
    captures-first ordering, one-ply capture extension at leaves, any repetition scored as a draw,
    time budget + `shouldStop` callback.
  - `Difficulty`: EASY (depth 2, 300 ms, random among moves within 60 of best),
    MEDIUM (depth 4, 800 ms), HARD (iterative deepening to 1500 ms).
  - `AiPlayer`: suspend wrapper; stops when its coroutine is cancelled.
- UI: `HomeScreen` (play as Goats/Tigers, difficulty, or 2-player), Navigation Compose
  (`ui/BaghchalApp.kt`, string routes via `GameSetup`), AI turns on `Dispatchers.Default` with a
  "Thinking…" spinner (minimum 400 ms so replies are visible), "You win!/You lose", Menu + New game.
- Tests: `SearchBoardTest` (2), `AiTest` (11), 4 new `GameInteractionTest` cases. 62 tests, all green.
- Checked on emulator: home screen, AI as goats on Hard, thinking indicator, Back mid-search.
- Quick self-play check: Medium beats Easy from both sides; Hard vs Hard draws.

### Decisions worth knowing
- **No Zobrist keys:** CLAUDE.md says Zobrist, but the exact 56-bit position key (2 bits per point +
  side + goats in hand) fits in a Long, is updated incrementally just as cheaply, and can't collide.
  It's used for both the transposition table and repetition checks.
- Transposition table: 2^17 slots (~2.5 MB), always-replace, kept across moves.
- Selected side/difficulty survive rotation (`rememberSaveable`) but aren't persisted yet (DataStore, step 4).

### Known issues / follow-ups
- Transposition-table entries can carry draw-by-repetition scores into other paths (standard caveat).
- Easy is quite weak as goats (can lose in ~12 plies); may want tuning after playtesting.
- Hint button (medium-depth AI) not added yet; it's listed under UI/UX and monetisation.

## Session 2026-10-23 (3) — roadmap step 2: basic UI, local 2-player

### Done
- `ui/board/BoardGeometry.kt`: point ↔ screen mapping from the 600-unit artwork coordinates
  (75 + n × 112.5), pieces at 90 units, tap radius 0.45 × spacing.
- `ui/board/BoardCanvas.kt`: Canvas drawing `board_wood`, target highlights (green dots),
  selected ring (yellow), `ic_tiger` / `ic_goat` pieces; taps → point index.
- `ui/screens/GameInteraction.kt`: pure tap-to-select / tap-to-move reducer (`GameUiState`);
  goat placement highlights every empty point, tapping a selected piece deselects.
- `ui/screens/GameViewModel.kt` (StateFlow) + `GameScreen.kt`: turn + phase line, board,
  goats in hand, captured x/5, result text and "New game" button when the game ends.
- `MainActivity` shows `GameScreen`. All UI strings in `strings.xml`.
- Tests: `BoardGeometryTest` (4), `GameInteractionTest` (9). 45 unit tests total, all green.
- Checked on the `Medium_Phone` emulator: placement, tiger select/highlight, capture, landscape.

### Known issues / follow-ups
- No animations, sound or haptics yet (step 4).
- Game survives rotation (ViewModel) but not process death; save/resume is step 4.
- Landscape works but the board is small; layout is portrait-first.
- Board taps are not exposed to TalkBack per point (only a board content description).
- No Compose UI tests yet (CLAUDE.md wants them for key screens).

### Next
- Roadmap step 3: AI (minimax + alpha-beta, iterative deepening, Zobrist/TT, difficulties).

## Session 2026-10-23 (2) — roadmap step 1: engine

### Done
- `engine/Board.kt`: point indexing, precomputed `adjacency` and `jumps` (`JumpLine(over, landing)`),
  diagonals only from even-parity points.
- `engine/Move.kt`: `Place`, `Step`, `Jump`.
- `engine/GameState.kt`: immutable state, `Side`, `Phase` (derived from goats in hand), `RulesConfig`
  (`repetitionsForDraw = 3`, `maxPliesWithoutCapture = 100`), constants `TOTAL_GOATS`, `GOATS_TO_WIN`,
  exact 56-bit `positionKey`, `GameState.create(...)` factory for arbitrary positions.
- `engine/GameStatus.kt`: `InProgress` / `Won(winner, WinReason)` / `Drawn(DrawReason)`.
- `engine/Rules.kt`: `initialState`, `legalMoves` (empty once the game is over), `isLegal`, `apply`
  (throws on illegal move), `status`.
- Tests: `BoardTest` (6) and `RulesTest` (25): all green with `./gradlew test`.

### Decisions worth knowing
- Move limit counts **plies** (single turns), default 100, reset by captures.
- Repetition history resets on placements and captures (those positions can never recur).
- Win checks take priority over draws (5th capture / trapped before repetition or move limit).
- Goats can never be blocked during placement (at least one point is always empty).

### Next
- Roadmap step 2: basic UI (Canvas board using `board_*` drawables, tap-to-move, local 2-player).
- AI tests from the CLAUDE.md list ("legal move in every position", "respects time budget")
  belong to step 3.

## Session 2026-10-23 — project setup + assets

### Done
- Build setup (version catalog): added lifecycle-runtime-compose, lifecycle-viewmodel-compose,
  navigation-compose 2.9.5, datastore-preferences 1.1.7, kotlinx-coroutines-android 1.10.2.
- Unit tests switched to JUnit 5 + kotlin.test (`useJUnitPlatform()`), plus kotlinx-coroutines-test.
- `app_name` → "Bagh Chal".
- `scripts/convert-svgs.sh` (+ `scripts/SvgConvert.java`): converts every `design/*.svg` to
  `res/drawable/*.xml` with Android Studio's Svg2Vector, and renders the legacy (API 24–25)
  launcher icons into `mipmap-*/ic_launcher{,_round}.webp`.
  Generated: `board_wood`, `board_stone`, `board_cloth`, `ic_tiger`, `ic_goat`,
  `ic_launcher_background`, `ic_launcher_foreground`.
- Adaptive icon (`mipmap-anydpi-v26`) now uses the converted tiger foreground + board-pattern background.
- `./gradlew assembleDebug test` green.

### Not done / deliberately deferred
- Ads, UMP, Billing deps: deferred to roadmap step 5 (Ads SDK needs an App ID in the manifest
  or the app crashes on launch).
- R8 still disabled for release (release checklist item).
- Not a git repository yet.

### Known issues
- Themed (monochrome) icon reuses the full-colour foreground, so Android 13+ themed icons show
  a solid tiger-head silhouette with no face detail. Needs a dedicated
  `design/ic_launcher_monochrome.svg`.
- `board_stone` has a light "highlight" line layer offset by +2.5 units; main lines/points are on
  the standard 75…525 grid, so geometry matches the other boards.

### Next
- Roadmap step 1: engine (`Board`, `Move`, `GameState`, `Rules`) + full unit tests.
