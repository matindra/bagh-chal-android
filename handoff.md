# Handoff

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
