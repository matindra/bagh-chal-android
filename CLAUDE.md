# CLAUDE.md — Bagh Chal (Tiger Move)

Project guide for Claude Code. Read this file fully before making changes. Keep it updated when conventions change.

## Project overview

- **App:** Bagh Chal — traditional Nepali tiger-and-goat strategy game for Android
- **Publisher:** Matin Studios (solo developer)
- **Package name:** `com.matinstudios.baghchal`
- **Goal:** A polished, offline-first single-player game with AI opponent (play as Tiger or Goat), local 2-player pass-and-play, monetised with AdMob + a one-time "Remove Ads" in-app purchase.
- **Handoff:** Keep `handoff.md` in the repo root up to date at the end of every session (what was done, what's next, known issues).

## Tech stack

- Kotlin, Jetpack Compose (Material 3), single-activity architecture
- MVVM: `ViewModel` + `StateFlow`, unidirectional data flow
- Kotlin Coroutines (AI search runs on `Dispatchers.Default`, never on Main)
- Navigation Compose
- DataStore (Preferences) for settings, stats and saved game
- Google Mobile Ads SDK (AdMob directly, no mediation for now)
- Google Play Billing Library (latest) for "Remove Ads"
- Google UMP SDK for consent (GDPR/EEA) before any ad request
- Build: Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`)
- minSdk 24, target/compile SDK = latest stable required by Play
- Tests: JUnit 5 / kotlin.test for the game engine, Compose UI tests for key screens

## Module / package layout

```
app/src/main/java/com/matinstudios/baghchal/
├── engine/        # PURE Kotlin game logic — no Android imports
│   ├── Board.kt         # 25 points, adjacency, jump tables
│   ├── GameState.kt     # immutable state
│   ├── Move.kt          # Place, Step, Jump
│   ├── Rules.kt         # legal move generation, apply move, win/draw detection
│   └── ai/
│       ├── Evaluator.kt
│       ├── Search.kt    # minimax + alpha-beta + iterative deepening
│       └── Difficulty.kt
├── ui/
│   ├── board/      # Canvas-drawn board, pieces, animations
│   ├── screens/    # Home, Game, Settings, HowToPlay, Stats
│   └── theme/
├── ads/            # AdMob wrappers (banner, interstitial, rewarded), consent
├── billing/        # Remove Ads purchase + entitlement
├── data/           # DataStore repositories
└── MainActivity.kt
```

**Rule:** `engine/` must stay free of Android dependencies so it is fully unit-testable and fast.

## Game rules (source of truth)

### Board
- 5×5 grid of points, indexed 0..24 as `index = row * 5 + col`.
- Orthogonal connections between all neighbouring points.
- Diagonal connections only from points where `(row + col) % 2 == 0`.
- Precompute `adjacency[25]` and `jumps[25]` (list of `(over, landing)` pairs) once at startup.

### Pieces & setup
- 4 Tigers start on the corners: 0, 4, 20, 24.
- 20 Goats start off-board ("in hand").
- **Goats move first.**

### Phase 1 — Placement
- Goat turn: place one goat from hand on any empty point (goats cannot move in this phase).
- Tiger turn: step or capture (see below).
- Phase ends when all 20 goats have been placed.

### Phase 2 — Movement
- Goat turn: move one goat one step along a line to an adjacent empty point.
- Tiger turn: step or capture.

### Tiger moves
- **Step:** one point along a line to an adjacent empty point.
- **Capture:** jump in a straight line over an adjacent goat to the empty point directly beyond. The goat is removed.
- Only **one** capture per turn (no multi-jumps).
- Capturing is optional, not mandatory.

### Win / draw conditions
- **Tigers win:** 5 goats captured (make this a constant `GOATS_TO_WIN = 5`).
- **Goats win:** it is the tigers' turn and no tiger has any legal move.
- **Goat with no moves:** if it is the goats' turn in movement phase and no goat can move, tigers win.
- **Draw:** threefold repetition of the same position (track position hashes), or a configurable move limit (e.g. 100 moves without a capture). Keep these configurable.

## AI

- Minimax with alpha-beta pruning, iterative deepening, move ordering (captures first).
- Zobrist hashing + transposition table.
- Respect a time budget per move (e.g. 300 ms easy → 1500 ms hard) and support cancellation via coroutine `isActive`.
- Difficulty levels:
  - **Easy:** depth 1–2 plus some randomness among good moves
  - **Medium:** depth ~4
  - **Hard:** iterative deepening up to time budget
- Evaluation ideas (score from Tiger's view; negate for Goat):
  - goats captured (heavy weight)
  - tiger mobility (number of legal tiger moves)
  - number of goats currently capturable
  - trapped tigers (strongly negative for tigers)
  - goat structure: goats on edges/corners and adjacent support
- The AI must never block the UI thread. Show a subtle "thinking" indicator.

## UI / UX

- Board drawn with Compose `Canvas`; scale to screen, portrait-first, support tablets.
- Tap to select a piece, highlight legal destinations; tap destination to move.
- Animate steps and jumps; fade out captured goats.
- HUD: goats in hand, goats captured (x/5), whose turn, phase indicator.
- Undo (1 move vs AI; can be gated behind a rewarded ad for free users).
- Hint button (uses AI at medium depth; rewarded ad for free users).
- Sound effects + haptics, toggleable in Settings.
- "How to Play" screen with illustrated rules.
- Save/resume the in-progress game automatically.
- Stats: wins/losses per side and difficulty.
- Localisation-ready: all strings in `strings.xml` (plan for English + Hindi + Odia + Nepali).
- Portrait only on phones.

## Monetisation

### Ads (AdMob)
- **Always use Google test ad unit IDs in debug builds.** Real IDs only in release via `BuildConfig` fields / `local.properties` — never hard-code real IDs in source and never commit them.
- Request consent with UMP before initialising ads; initialise Mobile Ads SDK once in `Application`.
- **Banner:** home screen and below the board (never overlapping the board or controls).
- **Interstitial:** only between games (after the result screen), with a frequency cap (e.g. max once every 2 games and at least 3 minutes apart). Never mid-game, never on app launch.
- **Rewarded:** optional, user-initiated, for Undo and Hint.
- Preload ads; handle failures silently — the game must work fully offline with no ads.
- Follow AdMob policy: no accidental-click layouts, no ads near interactive game pieces.

### Remove Ads (Play Billing)
- One-time non-consumable product: `remove_ads`.
- Acknowledge purchases; restore on launch via `queryPurchasesAsync`.
- Cache entitlement in DataStore; when owned, hide banners/interstitials. Rewarded features become free.
- Settings screen has "Remove Ads" and "Restore Purchase".

## Coding conventions

- Kotlin official style; no wildcard imports.
- Immutable game state (`data class` + copy); engine functions are pure.
- No business logic in Composables — Composables render state and send events.
- Prefer small files and clear names over comments; document non-obvious rules logic.
- No new third-party libraries without asking me first.
- Don't change package name, applicationId, or signing config.

## Testing requirements

- Engine changes must include unit tests. Cover at minimum:
  - adjacency/diagonal correctness for all 25 points
  - placement phase rules and transition to movement phase
  - capture legality (straight-line, landing empty, one per turn)
  - tiger win at 5 captures, goat win when all tigers trapped
  - repetition/draw detection
  - AI returns a legal move in every tested position and respects time budget
- Run `./gradlew test` before saying a task is done. Run `./gradlew lint` before release builds.

## Common commands

```
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
./gradlew lint
./gradlew bundleRelease      # AAB for Play Console
scripts/convert-svgs.sh      # regenerate res/drawable + legacy launcher icons from design/*.svg
```

## Release checklist

- [ ] versionCode incremented, versionName updated
- [ ] Real AdMob IDs set for release only; test IDs in debug
- [ ] R8/minify enabled, ProGuard rules for Ads/Billing verified
- [ ] Privacy policy URL (hosted on matinstudios.com) in app + Play Console
- [ ] Data safety form updated (Ads SDK collects device identifiers)
- [ ] App content rating + target audience set (do **not** target children unless switching to Families-compliant ads)
- [ ] Tested on a low-end device and a tablet

## Development roadmap

1. **Engine:** board, rules, win/draw detection + full unit tests
2. **Basic UI:** board rendering, tap-to-move, local 2-player
3. **AI:** minimax/alpha-beta, difficulty levels, play as either side
4. **Polish:** animations, sound, haptics, How to Play, save/resume, stats
5. **Monetisation:** UMP consent, banner, interstitial (capped), rewarded undo/hint, Remove Ads IAP
6. **Release:** icon, store listing, screenshots, internal → closed → production testing

## Working agreement for Claude

- Work one roadmap step at a time; propose a short plan before large changes.
- Keep the build green; don't leave the project in a non-compiling state.
- Ask before deleting files or changing architecture.
- Update `handoff.md` at the end of each session.


## Assets

- Source SVGs live in `design/`. Convert them to VectorDrawables in `res/drawable/`; never edit the generated XML by hand.
- Run `scripts/convert-svgs.sh` after changing any SVG (uses Android Studio's Svg2Vector; needs Android Studio installed and `cwebp`). Name mapping lives in `scripts/SvgConvert.java` (`board.svg` → `board_wood`, `tiger_piece.svg` → `ic_tiger`, `goat_piece.svg` → `ic_goat`).
- Pieces: `ic_tiger`, `ic_goat`.
- Boards: `board_wood`, `board_stone`, `board_cloth`, all 600×600 units with identical geometry.
- The 25 board points are at x/y = 75, 187.5, 300, 412.5, 525 (spacing 112.5). Scale these to the on-screen board size.
- Pieces are drawn at ~90 units (80% of spacing), centered on points.
- Board themes: Wood is free; Stone and Cloth are unlockable (rewarded ad, or included with Remove Ads). Store the selected theme in DataStore.