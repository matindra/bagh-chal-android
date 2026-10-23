# Handoff

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
