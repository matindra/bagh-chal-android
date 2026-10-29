# Handoff

## Step 4 backlog (polish)
- ~~Animations~~, ~~haptics~~ and ~~Settings toggles~~: done in session 7 (4b).
- ~~Sound effects~~: done in session 12.
- ~~How to Play screen~~: done in session 11.
- ~~Save/resume~~ and ~~stats~~: done in session 6 (4a). Still open: persist last chosen side/difficulty on Home.
- ~~TalkBack board points~~ and ~~Compose UI tests~~: done in session 9.

## Session 2026-10-29 (45) — Home: rules link in the menu, coloured corner buttons, glass Play with AI
- "New to Bagh Chal? / Learn the rules in 2 minutes" moved from under the mode rows into Home's side menu, replacing its "How to play" entry (`AppMenuItem` gained an optional `detail` second line). `LearnLink` and string `home_learn_link` removed. Game screens' menus still say "How to play".
- Corner buttons: Shop gold, Stats cyan, Settings violet, menu frosted (`HomeIconColors`, colours in `HomeNight`).
- Play with AI is semi-transparent coral glass (`PLAY_AI_GLASS` = 0.5 over the mountain blue, coral border), like the mode rows but stronger.
- `AppFlowTest`: menu tests use `home_learn_title`; `learnLinkOpensHowToPlay` removed (covered by `homeToHowToPlayAndBack`). All 20 pass on the emulator. Music confirmed playing on the emulator (stereo MediaPlayer, USAGE_GAME).

## Session 2026-10-29 (44) — Background music
- Sources `design/sounds/Bagh Chal - Roll the Dice.mp3` (menus) and `Bagh Chal - Tavern Tables.mp3` (games) → `res/raw/music_home.ogg` (2.3 MB) and `music_game.ogg` (2.4 MB) via `scripts/convert-sounds.sh`. The script now takes channels, loudness and quality per file: music is stereo, -23 LUFS (under the -18 LUFS effects), Vorbis q3, cover art dropped, no fades so the loops are seamless. Effects regenerate byte-identical.
- `ui/sound/MusicPlayer.kt`: `MusicTrack` (HOME, GAME), `musicFor(enabled, inGame)`, `MediaMusicPlayer` (looping `MediaPlayer`, 0.5 s fade out / 1.2 s fade in between tracks, pauses in the background via `MainActivity.onStart/onStop`). Audio focus: stays quiet if another app is already playing music, pauses for calls/notifications, stops when another app takes over until the app comes back. `NoMusic` in instrumented tests.
- Track choice in `AppViewModel` (combines the setting with `onScreenChanged`); `BaghchalApp` reports whether a game route (local or online) is on the back stack, so Settings/Stats/How to Play opened from a game keep the game music. The local result screen is not a game screen (home music).
- New Settings toggle "Music" (`Settings.musicEnabled`, key `settings_music`, default on), separate from Sound.
- Tests: `MusicTrackTest`, `RepositoriesTest.music defaults to on and persists`. `./gradlew assembleDebug test lintDebug` pass. Not checked on a device (none connected).

## Session 2026-10-29 (43) — Home: "Himalaya Snow Night" (design C5)
- Design C5 from the canvas https://claude.ai/artifact/KRBx24sZHHbzo44Xud7Qx4 (with C3's cyan Play button).
- Home is always a night scene, whatever the app theme or board (fixed colours in `HomeNight`, `ui/screens/HomeScenery.kt`): navy gradient, two drifting aurora glows, snowy peaks, falling snow (all in `NightSkyBackdrop`, drawn above the banner only). Name on an ice plaque hanging from chains (`IceSign`, swings ±2°, twinkles). The existing chase scene sits in a frosty frame at 350:200; `ChaseBanner` gained an optional `scenery: ChaseSceneryColors` (snowy sky/hills here; other callers keep the theme colours).
- Buttons in `ui/screens/HomeButtons.kt`: `RaisedButton` (face on a darker lip, sinks when pressed), `PlayAiButton` (cyan, sweeping shine), `FrostModeButton` (Friend, Quick Match, Pass n Play), `HomeIconButton`, `GoldPillButton` (Resume).
- Bottom `NavigationBar` removed: menu button top-left; Shop (gold), Stats, Settings icon-only top-right (content descriptions = their labels). Dashed learn card replaced by one underlined link (`home_learn_link` = "%1$s %2$s").
- "Game in progress" card kept (frosted, gold Resume) between the scene and Play with AI.
- All looping motion runs off one frame clock (`rememberSceneClock`), read only in draw/layer blocks; it stays still when system animations are off.
- `AppFlowTest`: corner buttons found by content description; `bottomBarOpensStats…` → `cornerButtonOpensStatsAndBackReturnsHome`, `learnCardOpensHowToPlay` → `learnLinkOpensHowToPlay`.
- `./gradlew assembleDebug test` pass; all 21 `AppFlowTest` UI tests pass on the emulator; checked Home on the emulator (light theme, with a saved game).
- Known: font is the system font (Black weight); the mockup used Lilita One (would need a bundled font file).
- Leftovers deleted: `ModeTile.kt`, string `home_play`, drawables `ic_mode_vs_ai_line`, `ic_mode_{friend,online,vs_ai,vs_player}` (old colourful set), `ic_phone`, with their `design/` SVGs and `SvgConvert.java` mappings. Build + unit tests pass.
- Colours updated from the canvas: Play with AI is coral (`HomeNight.Coral`, C4) with white text and "3 difficulties" on a white pill, shine kept. Mode rows are tinted glass in their own colour (`ModeColors`; face = chip colour at 16% over the mountains, border at 50%): Friend sky blue, Quick Match pink, Pass n Play green. Build + unit tests pass; checked on the emulator.
- Status bar strip fixed: `MainActivity` passes the Scaffold's bar padding to `BaghchalApp(barPadding)`; every destination but Home is wrapped by `insetScreen` (padded as before). Home draws its sky behind the status bar and mountain blue behind the navigation bar, pads its content/banner itself, and shows white bar icons (`WhiteSystemBarIcons`), switching to the theme's icons while the side menu is open and when Home leaves. Before/after on the canvas ("Status bar strip fix"). Checked light + dark on the emulator; all 58 UI tests and unit tests pass.

## Session 2026-10-29 (42) — Fix: every goat animating on placement
- Bug: placing a goat made every goat on the board pop in (same for all tigers during a capture jump, and while dragging). Cause: all pieces shared one `VectorPainter`, which caches a single bitmap at the last size drawn; the moving piece redrew it at its animated size, and every earlier draw of that bitmap in the frame showed the final image.
- Fix in `BoardCanvas`: the moving piece and the carried piece use their own painters (`moving*Painter`, `carried*Painter`).
- `./gradlew assembleDebug test` pass. Not checked on the emulator.

## Session 2026-10-29 (41) — Tiger badge app icon, Line Art pieces
- Logo from the Home mockup canvas https://claude.ai/artifact/1iGHf4Hbd7rHnouXMZ7XVJ (not built in the app yet): cream board lines on tiger orange `#9C4A00`, stones on the 4 corners, tiger-head line art in a cream disc in the centre.
- New app icon `AppIcon.BADGE` ("Tiger badge"): `design/launcher_badge_{background,foreground}.svg` → `ic_launcher_badge_*`, alias `.LauncherBadge` (disabled by default), mipmap-anydpi-v26 XML + legacy WebPs. Board kept inside the 66 dp safe circle.
- Store export: `store/icon-badge-512.png` (512×512, RGB). `scripts/make-store-images.py` gained `render_badge_icon()`. `store/icon-512.png` (Classic) is still the Play listing icon; swap in the badge file in Play Console if wanted.
- New `PieceSet.LINE_ART` ("Line Art"), redrawn to the user's picture: plain tiger-orange `#9C4A00` disc with 3 cream whisker strokes per side; goat-green `#3B6934` disc with a cream smile curve and a dot below. No outline, no faces (the first face-drawing version was rejected and replaced). `design/tiger_line.svg`, `design/goat_line.svg` → `ic_tiger_line`, `ic_goat_line`. Unlocked like other sets.
- `./gradlew test assembleDebug` pass. Not checked on the emulator.

## Session 2026-10-29 (40) — Night Eyes piece set
- New `PieceSet.NIGHT_EYES` ("Night Eyes"), unlocked like the other sets. Art matches the Night eyes app icon: dark `#121821` faces, amber slit-pupil eyes + orange rim for the tiger, pale-green bar-pupil eyes + green rim for the goat.
- Sources `design/tiger_night_eyes.svg`, `design/goat_night_eyes.svg` → `ic_tiger_night_eyes`, `ic_goat_night_eyes` (mapped in `SvgConvert.java`). Checked on Wood and Night boards (headless Chrome render).
- `./gradlew test assembleDebug` pass. Not checked on the emulator.
- Goats of Bold, Glyph, Snow Tiger and Night Eyes reshaped from a round disc to a goat head (wide brow, narrow muzzle, like Classic), keeping each set's rim and colours.

## Session 2026-10-28 (39) — Home bottom bar
- Design D from the canvas https://claude.ai/artifact/7N8znt6qqEUE9o9cZcMCLY (with A's "Game in progress" card, which is the existing `ResumeCard`).
- Home has a Material 3 `NavigationBar` at the bottom: Home (selected) / Shop / Stats / Settings. The items open the existing screens (which still use `BackTopBar`); the bar shows on Home only, so these are not real tabs.
- Top-right Shop button removed. The menu button stays: the side menu still holds How to play, website, privacy policy, theme and version; Stats/Settings left it on Home (the game-screen menu is unchanged).
- Banner sits above the bar with an 8 dp gap on both sides.
- New dashed "New to Bagh Chal?" card under the modes opens How to Play (strings `home_learn_title`, `home_learn_detail`). It always shows; could be hidden once the player has finished a game.
- `AppFlowTest`: Stats/Settings now reached from the bar; new `bottomBarOpensStatsAndBackReturnsHome`, `learnCardOpensHowToPlay`; `menuOpensStatsAndBackClosesIt` → `menuOpensAndBackClosesIt`.
- `./gradlew test assembleDebug` and androidTest compile pass. **UI tests not run** (no device connected); not checked on the emulator.
- Text now matches the user's edited design D exactly: modes "Friend" / "Play with your friend online", "Quick Match" / "Play with random people online", "Play with AI" / "3 difficulties", "Pass n Play" / "One phone, two friends side by side"; learn card "Learn the rules in 2 minutes" with a chevron; first bar item "Play" (`home_play`, new `ic_play`). Tile details may wrap to 2 lines (`ModeTile`). New icons `design/play.svg` → `ic_play`, `design/chevron_right.svg` → `ic_chevron_right` (mapped in `SvgConvert.java`).

## Session 2026-10-27 (38) — Home redesign
- Design from the canvas https://claude.ai/artifact/7TSrXt9yqa8iJUCZFEK9Er.
- Home order is now header → chase animation (138 dp, was 150) → **compact "Game in progress" row** (mini board, one summary line, 44 dp Resume button) → online/offline tiles.
- Mode tiles moved to `ui/screens/ModeTile.kt`: ~112 dp (was ~170), raised look (darker 5 dp lip, coloured shadow, face sinks into the lip when pressed), white icon disc that bobs with a shrinking ground shadow, a sheen that sweeps across every ~5.5 s, faint corner art per tile (`TileArt`: spinning globe, code digits, chip, swap arrows). `phase` keeps tiles out of step.
- Looping motion stops when system animations are off: new shared `ui/board/Motion.kt` `rememberAnimationsOn()` (ChaseBanner uses it too).
- Strings: `mode_online_detail` "Random opponent", `mode_friend_detail` "Share a 4-digit code", `mode_vs_ai_detail` "Easy, Medium or Hard", `home_resume_progress` "%1$d in hand · %2$d/%3$d captured", new `home_resume_summary`. `AppFlowTest` resume check now matches the progress text as a substring.
- Tiles shrunk again to ~88 dp (min face 84 dp + 5 dp lip): 40 dp icon disc / 32 dp icon, 14 sp title, corner art scaled 0.8.
- Icon style **F (line glyph)** picked from the canvas board "Card icon styles": each tile shows a single-colour icon on a 36 dp solid chip (tertiary/primary/secondary with its on-colour, darker lip) that bobs over a ground shadow. New sources `design/mode_{online,friend,vs_ai,vs_player}_line.svg` → `ic_mode_*_line` (mapped in `SvgConvert.java`). The colourful `ic_mode_*` drawables are now unused (kept; delete if not wanted).
- Checked on the emulator in light and dark; unit tests + 19 `AppFlowTest` UI tests pass.
- Then made compact (~56 dp): each tile is one row — 32 dp chip (20 dp icon) left, title + detail right (both one line, auto-shrinking: title 14→11 sp, detail 11→9 sp). Corner art (`TileArt`) removed — no room. `mode_vs_player_detail` → "Share one phone". Frees roughly 110 dp on Home for more elements. Unit tests + 19 `AppFlowTest` pass; checked on the emulator.
- "Play online"/"Play offline" headings removed (strings `home_section_*` deleted; `ic_phone` now unused). Tiles are one 2×2 grid (`ModeRow`): Play with a Friend first, then Quick match, Play with AI, Pass n Play. Each its own colour: Friend uses a new rhododendron pink (`GameColors.friend*`, `Rhododendron*` in `Color.kt`), Quick match tertiary, AI primary, Pass n Play secondary. Checked light + dark; 19 `AppFlowTest` pass.
- Corner art sits in the top-right of each tile (first try at bottom-right ran through the subtitles).
- Unit tests and all 19 `AppFlowTest` UI tests pass. Checked Home on the emulator in light and dark, with and without a saved game. Other UI test classes not re-run.

## Session 2026-10-27 (37) — Shop
- "Boards & Pieces" left the side menu. Home has a **Shop** button top-right, across from the menu button (tonal, bag icon `ic_shop` from `design/shop.svg`, string `home_shop`). The game screens no longer link to it; boards/pieces are picked from Home only.
- New Shop screen (`ui/screens/ShopScreen.kt`, route `shop`): three cards, each showing what is in use — Boards ("x of 11 unlocked"), Pieces ("x of 5 unlocked"), App icons ("All free"). Each opens its own picker (`BoardsScreen(section: ShopSection)`, route `shop/{section}`); the pickers work as before (rewarded ad to unlock).
- Removed strings `boards_title`, `menu_boards`, `boards_section_*`; new `home_shop`, `shop_*`.
- `AppFlowTest` locked-boards test now goes Home Shop button → Boards, then Back → Pieces. Unit tests and all UI tests pass; checked menu, Shop and Pieces on the emulator.
- Not done: translations for the new strings. Remove Ads is still in Settings (could also be offered in the Shop).

## Session 2026-10-26 (36) — closed-testing build vc6
- versionCode bumped to **6** (versionName 1.0). Unit tests, lint (0 errors) and an unsigned `bundleRelease -PtestAds=true` (R8) pass.
- Signed bundle: `scripts/build-release.sh --test-ads` → `dist/baghchal-1.0-vc6-testads.aab` (needs the keystore password, run by the user).
- Includes sessions 30–35 (rematch, buttons, menu, Home animation, drag and drop, new boards, piece sets, app icons, board-tinted colours). Testers may see their home-screen icon drop once (launcher activity-alias change).

## Session 2026-10-26 (35) — app colours follow the board
- Picking a board re-colours the whole app in a lighter shade of it (a darker one in dark mode): backgrounds, cards, outlines and body text take the board's hue, and the third colour (online cards, Home demo sky) becomes the board's accent. Tiger orange and goat green never change. Wood keeps the original palette.
- Code: `ui/theme/BoardTint.kt` (`BoardTheme.tint` hue/saturation + accent per board, `ColorScheme.tinted`), `BaghchalTheme(boardTheme = …)`, `ThemeViewModel.boardTheme`, passed in `MainActivity`.
- `BoardTintTest` checks text contrast (WCAG 7:1 body, 4.5:1 secondary/accents) for every board, light and dark. Unit tests + all 56 UI tests pass. Checked every board in light and dark with screenshots of Home and Settings.

## Session 2026-10-26 (35) — piece sets and app icons
- Designs from the canvas https://claude.ai/artifact/4uKGyVPCMPo77NLNhE1Lrg are now in the app.
- **Piece sets** (`PieceSet`: Classic, Bold, Glyph, Festival, Snow Tiger): Classic is free; the others unlock like boards (rewarded ad, THEME placement, or Remove Ads). Saved in DataStore (`settings_piece_set`, `entitlement_unlocked_piece_sets`). Drawn everywhere through `LocalPieceArt` (`ui/board/PieceArt.kt`), provided in `MainActivity`; no screen uses `R.drawable.ic_tiger/ic_goat` directly any more (ChaseBanner switched by the Home-animation session).
- **App icons** (`AppIcon`: Classic, Face-off, Glyph, On the board, Night eyes, Festival mask), free. The LAUNCHER filter moved off `MainActivity` onto six `<activity-alias>`es (`.LauncherClassic` enabled by default); `data/LauncherIcons.kt` enables one and disables the rest (`DONT_KILL_APP`). Current icon is read from PackageManager, not stored. Closed-testing testers may see their home-screen shortcut drop once on update (component name changed).
- The Boards screen is now "Boards & Pieces" (menu item too) with Board / Pieces / App icon sections; piece tiles preview on the selected board.
- Assets: `design/{tiger,goat}_*.svg`, `design/launcher_*_{background,foreground}.svg`; `SvgConvert.java` maps them and renders legacy mipmaps for every launcher icon (`LAUNCHER_ICONS`). Adaptive XML in `mipmap-anydpi-v26/ic_launcher_*.xml` (hand-written, monochrome = foreground).
- Tests: `EntitlementsTest` + `RepositoriesTest` cover piece sets; `AppFlowTest` locked test counts boards + piece sets and tries a locked piece set. Unit tests + AppFlowTest pass; checked the screen and an icon switch (Night) on the emulator. The emulator is left on the Night icon (shell can't reset it; pick Classic in the app).
- Full check after: unit tests, all 56 UI tests and lint pass (0 errors, 40 warnings, none from this change; removed MainActivity's now-redundant label).
- Not done: translations for the new strings.

## Session 2026-10-26 (34) — eight new board themes
- New boards (each unlocked by a rewarded ad, or with Remove Ads): Night (starry navy, glowing lines, mountains), Thangka (red/gold, lotus at the centre), Jungle (grass, dirt paths, leafy frame), Mandala (cream/saffron/teal rings), Dhaka (dhaka-topi woven border), Brass (hammered plate, engraved lines), Ink (rice paper, brush lines, ink-wash hills, red seal), Snow (ice-blue, snowflakes).
- SVGs in `design/board_*.svg` → `res/drawable/board_*.xml` via `scripts/convert-svgs.sh` (mappings added to `SvgConvert.java`). `BoardTheme` enum, `drawable` and `label` mappings, strings `board_*`.
- Board picker moved out of Settings into its own **Boards** screen (`ui/screens/BoardsScreen.kt` + `BoardsViewModel.kt`, route `boards`), opened from a new "Boards" item (icon `ic_boards` from `design/boards.svg`) in the side menu on Home, the game screen and the online game screen. 3-column grid; locked boards still offer a rewarded ad. Settings now has only sound/haptics and Remove Ads.
- Unit tests and all 56 UI tests pass (`AppFlowTest` locked-boards test now goes through the menu). Checked the menu and Boards screen on the emulator. Not checked yet: whether the move bubbles stand out on Night and Brass. Hindi/Odia/Nepali names still to translate.

## Session 2026-10-26 (33) — drag and drop pieces
- Tigers and goats can be dragged: press a piece and move (= tapping it, so it selects and the bubbles show), carry it (follows the finger, 15% bigger, soft shadow; the bubble under it swells), drop on a bubble (= tapping that point). The move lands where dropped without replaying the slide; a jumped goat still fades. Dropping anywhere else puts the piece back, still selected. Taps work as before, and only presses that start on a piece become drags, so a shaky tap on an empty point still places a goat. Placement is still by tap.
- Goes through `onPointTapped`, so it works vs AI, Pass n Play and online with no logic changes. Code: `ui/board/PieceDrag.kt` (gesture), `BoardCanvas.kt` (carry/drop drawing).
- Tests: `GameContentTest` drag move, drag capture, drop on a non-move. Unit + all UI tests pass; checked on the emulator (horizontal drags don't open the side menu).

## Session 2026-10-26 (32) — Home animation
- Home has a wordless animated scene above the Online section (150 dp, rounded, not clickable, hidden from TalkBack): on a stretch of board line in front of soft hills, goats hop in point to point, the tiger crouches and leaps over a goat (it vanishes in a puff with sparks), two more goats hop in from the right and box the tiger in, it shakes, a gold ring pulses and the lock pops on, and the goats bounce for joy. Fades out and loops (~8.6 s).
- First version was a mini board with captions and a "How to play" link; the user didn't want a tutorial, so it was replaced.
- Code: `ui/board/ChaseScene.kt` (pure: `frameAt(millis)` → sprites, burst, lock), `ui/board/ChaseBanner.kt` (Canvas). One infinite transition, so it only runs while Home is on screen. With system animations off it shows a still of the trapped tiger.
- Tests: `ChaseSceneTest` (loop fades in/out, leap + capture, tiger boxed in on both sides at the end, no pieces overlapping on screen). Unit tests pass.
- Legal-move markers: the water bubbles are now plain flat green dots (no outline, shine or shadow), radius 17 instead of 26 art units, with a gentle size breathe. Same function names in `ui/board/BubbleDrawing.kt`.

## Session 2026-10-26 (31) — menu button in one place
- The ☰ button was pinned top-left on Home but sat lower on the game screens (it was inside the vertically centred content). New shared `MenuTopBar` (`ui/screens/AppMenu.kt`) pins it at the same spot (4 dp from the top-left, 56 dp bar) on Home, the AI / Pass n Play game screen and the online game screen. The bar grows for taller content (game status + phase line); the button stays put.
- Checked on the emulator (Home vs Pass n Play: same position). Unit tests + all UI tests pass. Not yet released.
- **One button system** (design canvas "Bagh Chal button system", https://claude.ai/artifact/Yb9tuQqjjSrXRbAxMDwkTZ): new `ui/screens/AppButtons.kt` with `PrimaryButton`, `SecondaryButton`, `ButtonPair`, `BackTopBar`. Every main button is now filled orange, 56 dp, Title Medium (Create/Join/Find/Share in the lobby, Start, Next, Resume, Remove Ads, Play again, New game); Home/Menu/Cancel are outlined 56 dp in pairs; Undo/Hint 48 dp tonal; small actions stay text buttons. Create and Join are orange now (sky only for online cards/icons).
- Settings, Stats, How to Play and AI setup lost their bottom Back buttons; `BackTopBar`'s arrow sits exactly where the menu button does (also used by the lobby top bar, now 56 dp instead of 64). UI tests tap the arrow by content description.
- Unit tests, all UI tests and lint pass (lint warnings unchanged). Checked Friend lobby, Settings and AI setup on the emulator.

- Side menu shows "Version 1.0 (5)" under the theme picker (from `BuildConfig.VERSION_NAME` / `VERSION_CODE`, string `menu_version`).
- Side menu has a "Website" item (globe icon, above Privacy Policy) that opens https://matinstudios.com/bagh-chal-tiger-goat-game/ (`WEBSITE_URL` in `AppMenu.kt`).
- Legal-move markers are now green water bubbles (clear middle, coloured edge, white shine, soft shadow) that bob gently; `ui/board/BubbleDrawing.kt`. The bob only runs while bubbles are shown.
## Session 2026-10-26 (30) — Play again with the same friend
- After a **friend** game, Play again now offers a rematch instead of going back to the join screen. The friend's dialog says "Your friend wants to play again!"; when they tap Play again, both phones open the next game. Sides swap every game. Quick matches still go back to the lobby.
- Data: the offering player creates the next room already seated (`status: "rematch"`, guest = friend) and sets `rooms/{old}/rematch = {room, by}`; the friend accepts by setting it to "playing". Same-moment offers: the transaction keeps the first, the other is deleted. Leaving with an unanswered offer withdraws it (next room deleted, `rematch` cleared).
- Code: `OnlineService.playAgain` / `withdrawRematch`, `RoomSnapshot.rematch`, `ui/screens/OnlineRematch.kt` (`rematchUiFor`, tested in `OnlineRematchTest`), end dialog in `OnlineGameScreen`.
- Rules: new `rematch` validation in `firebase/database.rules.json`. **Works with the published rules too** (host/guest may already write their room); publish the new file to tighten it.
- Checked on the emulator against real Firebase (second player via REST): friend offers → accept → next game; I offer → friend accepts → next game; offer then Home → withdrawn. Unit tests + lint pass.
- versionCode bumped to **5** (versionName 1.0) for closed testing with test ads: `scripts/build-release.sh --test-ads` → `dist/baghchal-1.0-vc5-testads.aab`. Tests, lint and an unsigned `bundleRelease` (R8) pass.
- 2026-10-26: `dist/baghchal-1.0-vc5-testads.aab` uploaded to Play Console (closed testing).

## Session 2026-10-26 (29) — Home: four game modes
- "Choose a game" now lists, in order: **Play Online** (random opponent), **Play with a Friend** (4-digit code), **Play with AI**, **Pass n Play** (renamed from Player vs AI / Player vs Player).
- One lobby screen, two modes (`OnlineLobbyMode` RANDOM/FRIEND, route `online/{mode}`): Online shows only Quick match; Friend shows only Create + Join. "Play again" after an online game returns to the same lobby.
- New icon `design/mode_friend.svg` → `ic_mode_friend`. Both online cards use the sky (tertiary) colour. Share text now says "Open Play with a Friend".
- Home layout then changed to **grouped tiles** (design B, picked from a 3-option mockup): header = tiger · "Bagh Chal" · goat + "Tiger & Goat Game"; "ONLINE" (Online, Friend) and "ON THIS PHONE" (AI, Pass n Play) sections, 2 equal-height tiles each. Home no longer shows the prayer flags, mountains or "Choose a game" (Welcome and AI setup still use the decorations). New heading icons `design/globe.svg`, `design/phone.svg` → `ic_globe`, `ic_phone`. Pass n Play detail is now "Take turns on one phone".
- Tests: lobby tests split per mode, + friend-mode flow test. Unit tests, lint and all UI tests pass on the emulator. Not yet released (needs a new versionCode).

## Session 2026-10-25 (28) — fix: "no game with that code" for friends
- Cause (reproduced on the emulator + Firebase REST): the host marked itself in `rooms/{id}/online` only once. Sharing the code sends the host to another app, Android pauses Bagh Chal, the server drops the mark, and nothing set it again, so `join` (which needed the host online) said NotFound forever.
- Fix in `FirebaseOnlineService`: `stayOnline()` re-marks the player on every reconnect while waiting (private and quick match; `room()` uses it too). Joining by code no longer needs the host online (`join(needHostOnline = false)`); random rooms still do.
- Game screen: an opponent not yet seen online in this game gets 3 min to arrive (`OPPONENT_ARRIVAL_GRACE_MILLIS`, `opponentGraceMillis` in `OnlineSession.kt`) before you win; after that the usual 45 s.
- Checked on the emulator: host in background 2 min → mark gone; back in app → mark back. Friend joined (via REST, as a second client) while the host was away → host's app opened the game on return. Test: `OnlineSessionTest` +1; all unit tests pass.
- The closed-testing build (vc3) has this bug; it needs a new upload (versionCode 4).
- versionCode bumped to **4** (versionName 1.0); tests + lint pass (27 warnings, 0 errors). New `scripts/build-release.sh [--test-ads]` asks for the key passwords and writes `dist/baghchal-<name>-vc<code>[-testads].aab` + mapping.

## Session 2026-10-25 (27) — Firebase room cleanup
- No Cloud Functions (would need the Blaze plan); the apps clean up after each other.
- A room nobody joined is now **deleted** when its host stops waiting (cancel, or quick match found another room), instead of being marked "closed". The "closed" status is gone.
- `online/RoomCleanup.kt`: on an online action (`ready()`), at most once per 10 min per app run, the phone deletes up to 20 rooms older than 24 h (`createdAt`) and up to 20 codes older than 6 h (`at`). Fire and forget; failures are ignored.
- Rules: `/rooms` and `/codes` can be listed only with that exact old-entries query (orderByChild + endAt ≤ now − age + limitToFirst ≤ 20); anyone signed in may delete a room older than 24 h. Added `.indexOn` for `createdAt` and `at`. **Needs publishing in the Firebase console** (until then the sweep is just refused; deleting unjoined rooms works with the old rules too).
- Tests: `RoomCleanupTest` (3), incl. a check that the rules file uses the same ages and batch size. All unit tests pass. Not run against real Firebase.
- Note: a game stalled for over 24 h (both still connected) would lose its room; the game screen ignores a missing room, so it would just stop updating.

## 2026-10-25 — closed testing approved
- Play Console approved the closed-testing release (`dist/baghchal-1.0-vc3-testads.aab`, versionCode 3, Google test ad IDs).
- Before production:
  1. Add the **Play App Signing** SHA-1/SHA-256 (Play Console → Setup → App signing) to the Firebase Android app, re-download `google-services.json` if asked, then test online play from a Play-installed copy.
  2. Personal developer accounts need **≥12 opted-in testers for 14 continuous days** before Play allows production access.
  3. Data safety form: include Firebase (anonymous user ID, game data) as well as AdMob.
  4. ~~Room cleanup~~: done (below). **Paste the new `firebase/database.rules.json` into Firebase console → Realtime Database → Rules → Publish.**
  5. Production build: versionCode 4+, **without** `-PtestAds` (real ad IDs), `./gradlew test lint` first.

## Session 2026-10-25 (26) — store listing images
- `store/screenshots/01…08-*.png` (1080×1920, captioned, phone frame) and `store/feature-graphic.png` (1024×500); all 24-bit PNG, no alpha.
- Made by `scripts/make-store-images.py` (HTML → headless Chrome → ffmpeg) from emulator captures in `store/raw/` (debug build, demo-mode status bar). Only the top 1960 rows of each capture are shown, so the test-ad banner is cut off. Captions live in `SHOTS` in the script.
- Also `store/icon-512.png` (from the launcher icon SVGs, by the same script) and `store/listing.md` (name, short + full description, category, tags).

## Session 2026-10-25 (25) — first closed-testing build
- Signed release bundle: `dist/baghchal-1.0-vc3.aab` (versionCode 3, versionName 1.0), R8 mapping in `dist/mapping-1.0-vc3.txt` (dist/ is git-ignored). Real AdMob IDs confirmed in the release build.
- Signed with the upload key `~/keystores/baghchal-upload.jks` (alias `upload`, SHA-256 F3:45:18:…:18:C8) via `-Pandroid.injected.signing.*` on the command line; no signing config or password in the repo. Command: `./gradlew bundleRelease --no-configuration-cache -Pandroid.injected.signing.store.file=$HOME/keystores/baghchal-upload.jks -Pandroid.injected.signing.store.password=… -Pandroid.injected.signing.key.alias=upload -Pandroid.injected.signing.key.password=…`
- Lint fixes: `CueVibrator` effect helpers marked `@RequiresApi(O)` (one-shot vibration needs API 26); `OnlineLobbyScreen` reads strings via `LocalResources`. Tests + lint pass.
- Side menu (every screen) has **Privacy Policy** at the end of the list → opens https://matinstudios.com/bagh-chal-tiger-goat-game/privacy-policy in the browser (`PRIVACY_POLICY_URL` in `AppMenu.kt`, added inside `AppMenuDrawer`). Icon `ic_privacy` from `design/privacy.svg`. UI test `AppFlowTest.menuListsThePrivacyPolicy`; AppFlowTest 18/18 pass; checked on the emulator (opens Chrome). AAB above rebuilt with it.
- **Closed-testing upload uses Google test ad IDs:** `dist/baghchal-1.0-vc3-testads.aab` (+ `mapping-1.0-vc3-testads.txt`), built with `-PtestAds=true` (new switch in `app/build.gradle.kts`; release still uses real IDs without it). Before production: rebuild without `-PtestAds`.
- Next: next upload needs versionCode 4. After Play App Signing is on, add the Play signing SHA-1 to Firebase if Auth needs it.

## Session 2026-10-25 (24) — light/dark theme choice in the side menu
- Side menu (Home, game, online game) has a "Theme" picker at the bottom: System / Light / Dark (segmented buttons). Default System.
- Stored as `Settings.themeMode` (`ThemeMode` enum, key `settings_theme_mode`). `ui/ThemeViewModel` reads/writes it; `MainActivity` passes it to `BaghchalTheme` and re-styles the system bar icons to match.
- Test: `RepositoriesTest` "theme mode defaults to system and persists". All unit tests pass.
- Launch flash fixed on Android 12+: `MainActivity.applyToLaunchWindow` calls `UiModeManager.setApplicationNightMode` (YES/NO/AUTO), so the system draws the launch window in the chosen theme (remembered by the system). Manifest now has `configChanges="uiMode"` so changing theme recomposes instead of recreating the activity. Compose shows the window background until the saved theme is read. Android 7–11 can't change the launch window without AppCompat, so a brief flash remains there.
- Checked on the emulator (API 37, phone light, app Dark): cold start dark from the first frame; switching back to System returns to light.

## Session 2026-10-25 (24) — haptics fix + trapped vibration
- Cause: haptics went through Compose `performHapticFeedback` (ContextClick / LongPress). That is "touch feedback": muted when the phone's system Touch feedback is off, and ContextClick does nothing on many phones.
- Fix: `ui/haptics/CueVibrator.kt` calls the vibrator directly (predefined TICK / CLICK / HEAVY_CLICK on Android 10+, short one-shots before), marked as media/game vibration so only the in-app Haptics switch controls it. Added `VIBRATE` permission (normal, no prompt). Used by `GameScreen` and `OnlineGameScreen`.
- New cue `HapticCue.PIECE_TRAPPED` (light click): a tiger newly trapped, or in the movement phase a goat newly boxed in (`Rules.trappedGoats`: no empty neighbour). Priority: capture > trapped > placement. Settings text updated.
- Tests: RulesTest +1, GameFeedbackTest +2; all unit tests pass. On the emulator the vibrator log shows the cues with usage MEDIA. UI tests not re-run.

## Session 2026-10-25 (23) — quick match falls back to a hidden bot
- Quick match waits 3–5 s (random) for a real opponent; if none turns up, the game is played against the computer in a local room. Nothing on screen says it's a bot: same "Online game" screen, "Opponent" bar, thinking spinner, always connected.
- `online/BotFallbackOnline.kt` wraps `FirebaseOnlineService` (wired in `AppContainer`). Bot rooms have ids `bot-…` and live in memory only; `sendMove`/`resign` for them never reach Firebase. Bot: random side, `SearchLimits(depth 3, 600 ms, randomMargin 30)`, waits 0.7–2.5 s before each move. Private games (codes) are unchanged.
- `FirebaseOnlineService.findRandomMatch`: if someone joins our room just as the search is cancelled, we now resign it, so they aren't left facing an idle host.
- Lobby: searching card shows an elapsed m:ss timer in its top-right corner (`SearchTimer`, string `online_search_time`).
- Tests: `BotFallbackOnlineTest` (6). All unit tests pass. Checked on the emulator: timer, fallback after ~4 s, bot replies to moves.
- Note: bot games count as online games (no stats, no undo/hint). Sign-in failures still show the connection error rather than a bot.

## Session 2026-10-25 (22) — online lobby + game screen redesign
Built from the design canvas (claude.ai artifact "Bagh Chal Online Screens", boards Online lobby / Waiting for a friend / Online game).
- **Lobby** (`OnlineLobbyScreen.kt`, rewritten layout, same view model): top bar with back arrow; Quick match hero card (tiger VS goat, primaryContainer, Find opponent); "Play with a friend" divider; Create game card (Goats/Tigers side chips + Create); Join card with a 4-box code field (one `BasicTextField` drawn as boxes). Searching/creating/joining reuse the hero card with a spinner. While setting up, the top back button cancels.
- **Waiting for a friend**: code in 4 big tiles, Share code + Copy (clipboard; toast on API < 33), host's piece vs empty seat, Cancel game. `LobbyState.WaitingForFriend` now carries the host's `side`.
- **Online game**: new `OnlineGameContent.kt` — opponent bar above the board, turn banner + your bar below. The side to move gets its colours; tigers' bar shows captures as 5 pips, goats' bar shows in hand (placement) or on board (movement); opponent shows "thinking…" / "disconnected". `GameContent` lost its online-only `localControls` / `statusNote` params.
- New icons from `design/`: back, share, copy, add, join, search, person → `ic_*` (mapping in `SvgConvert.java`). ~20 new `online_*` strings.
- Tests: all unit + all UI pass; `OnlineScreensTest` updated for the new board/lobby. Lobby, busy and waiting screens checked on the emulator; the in-game screen only via UI test (needs two devices for a real match).
- Not built yet from the canvas: online result screen and profile/stats (need Rematch, online stats, player name).

## Session 2026-10-25 (21) — small fixes
- Home title now reads "Bagh Chal: Tiger & Goat Game" (new `home_title` string, one line: auto-shrinks from displaySmall down to 16sp). `app_name` (launcher label, side menu) stays "Bagh Chal".
- Home mode order is now Player vs AI, Play online, Player vs Player.
- "No sounds" report: not an app bug. The emulator's audio service shows the app's SoundPool starting on every move; the long-running `Medium_Phone` emulator had lost host audio. Fix = cold boot the emulator / check Mac output device.

## Session 2026-10-25 (20) — online play (Firebase)

### Done
- **Home** has a third card, **Play online** (sky blue, `ic_mode_online` from `design/mode_online.svg`). Home content now starts 52dp down so the ☰ button never sits on the prayer flags.
- **Lobby** (`OnlineLobbyScreen` + `OnlineLobbyViewModel`): Quick match (random opponent, random sides), Create game (host picks Goats/Tigers, gets a 4-digit code, Share code via the Android share sheet, waits), Join a friend's game (4-digit field). Errors show as snackbars (no connection / no game with that code / already started / need 4 digits).
- **Online game** (`OnlineGameScreen` + `OnlineGameViewModel`): same board UI (`GameContent` with `localControls = false`, so no Undo/Hint), turn note "You play Goats · Your turn", same side menu. Back/Home before the end asks "Leave the game?" (leaving = resign). End dialog after 1.2 s: You won / You lost / Draw + reason, Play again (back to lobby) or Home; leaving it calls `ads.onGameFinished()` (interstitial cap as usual). Opponent disconnected for 45 s ⇒ you win.
- **Backend** (`online/`): `OnlineService` interface; `FirebaseOnlineService` (anonymous Auth + Realtime Database); `NoOnline` when Firebase isn't configured or in instrumented tests. Pure, unit-tested parts: `MoveCodec` ("P12", "S0,1", "J0,1,2"), `RoomCode`, `RoomSnapshot`, `OnlineReplay` (each phone checks every move with the engine: legal, in turn, from the right player), `OnlineSession` + `onlineEndFor` in `ui/screens/OnlineSession.kt`.
- Matchmaking is all transactions: `lobby/random` holds the one waiting random room (refreshed every 20 s, stale after 60 s); `codes/{4 digits}` → private room (stale after 6 h); joining = transaction on the room (only if waiting, no guest, host connected). Security rules: `firebase/database.rules.json`.
- Libraries (approved: Firebase): firebase-bom **34.19.0** (35.x needs Kotlin 2.4 metadata; project is on Kotlin 2.2.10), firebase-auth, firebase-database, google-services plugin 4.5.0 — applied **only if `app/google-services.json` exists**, so the build stays green without it.
- Tests: +16 unit (OnlineRulesTest, OnlineSessionTest, OnlineLobbyViewModelTest), +6 UI (OnlineScreensTest, AppFlowTest unavailable case). All unit + 50 UI tests pass. Lobby/board checked visually; **real online play has not been run yet** (no Firebase project).

### To turn it on (needs you)
1. Firebase console → create project → add Android app `com.matinstudios.baghchal` (add debug + release SHA-1 later if needed) → download `google-services.json` into `app/`.
2. Authentication → Sign-in method → enable **Anonymous**.
3. Realtime Database → create (pick a region) → Rules → paste `firebase/database.rules.json` → Publish.
4. Build, install on two devices/emulators, try Quick match and a private code.

### Known gaps / next
- Rooms are never deleted (closed/finished ones pile up); add a cleanup (scheduled Cloud Function or TTL sweep) before launch.
- No turn timer: a connected opponent can stall forever (only disconnects are timed out).
- Online games don't count in Stats and aren't saved/resumed (leaving the app for long = disconnected = loss after 45 s).
- No server-side referee: both phones validate, so a cheat can only make the cheater lose on the honest phone.
- Play Data safety form: add Firebase (app instance/user IDs for anonymous auth, game data).
- New strings need Hindi/Odia/Nepali.

## Session 2026-10-25 (19) — hamburger menu (Home + game)

- **Game screen** has the same menu (button left of the turn badge) with an extra **Home** item (`menu_home`, `ic_home` from `design/home.svg`); Stats/Settings/How to play open on top of the game and Back returns to it (game results are a buffered channel, so a win while away still shows on return). The game banner moved out of `GameContent` into `GameScreen`, below the drawer, so the menu never covers it. `GameContent` lost its `banner` param and gained `onOpenMenu`. Test: `gameMenuOpensSettingsAndKeepsTheGame`.
- Home mode cards have their own icons instead of the tiger/goat heads: `design/mode_vs_ai.svg` → `ic_mode_vs_ai` (person + friendly robot), `design/mode_vs_player.svg` → `ic_mode_vs_player` (two people in tiger orange and goat green). Same outline colour/width as the pieces.
- Menu code renamed to `ui/screens/AppMenu.kt` (`AppMenuDrawer`, `AppMenuButton`, `AppMenuItem`).

- Home has a hamburger button (top-left) that opens a side drawer (`ui/screens/HomeMenu.kt`: `HomeMenuDrawer`, `HomeMenuButton`) with How to play, Stats and Settings. It replaces the row of text links at the bottom of Home. The drawer covers only the Home content, never the banner below it. Back closes it; picking an item closes it, then navigates.
- New icons from Material icon paths (Apache 2.0): `design/menu.svg`, `help.svg`, `stats.svg`, `settings.svg` → `ic_menu`, `ic_help`, `ic_stats`, `ic_settings`. New string `home_menu` ("Menu", the button's TalkBack label).
- The closed drawer stays in the semantics tree off-screen, so UI tests check visibility rather than existence for its labels. AppFlowTest goes through the menu (`openMenuItem`) and has a new `menuOpensStatsAndBackClosesIt`. All unit + UI tests pass on Medium_Phone.

## Session 2026-10-25 (18) — colour and decorations

- Root cause of the grey look: template theme with dynamic (wallpaper) colour. Replaced with a Bagh Chal palette in `ui/theme/Color.kt` + `Theme.kt` (light and dark), dynamic colour removed; `GameColors` (via `BaghchalThemeExtras.colors`) adds difficulty accents and decoration alphas. Window background matches (`values/colors.xml`, `values-night/colors.xml`), so no white flash at launch.
- New art in `design/`: `prayer_flags.svg` → `deco_prayer_flags`, `mountains.svg` → `deco_mountains`. `SvgConvert.java` name table moved to `Map.ofEntries` (`Map.of` caps at 10 entries).
- `ui/theme/Scenery.kt`: `MountainBackdrop` (gradient + mountains, used on Welcome and Home, banner kept outside it) and `PrayerFlags` (header on Welcome intro, Home, AI setup).
- Home: orange "Player vs AI" card with tiger, green "Player vs Player" card with goat, sky-blue Resume card, title in primary. AI setup: selected side card fills with that side's colour; difficulty rows have green/amber/red accents and 1–3 strength bars. Game: turn shown as a coloured badge with the side's piece; counters in tinted pills. Board, pieces and ad slots unchanged.
- Undo / Hint are tinted icon buttons (`AssistButton` in GameScreen): Undo sky blue (tertiaryContainer) with `ic_undo`, Hint amber (`GameColors.hintContainer`) with `ic_hint`; icons from `design/undo.svg`, `design/hint.svg` (Material icon paths, Apache 2.0). "(ad)" label kept for AdMob.
- Checked light + dark on the Medium_Phone emulator; all unit + UI tests pass.

## Session 2026-10-25 (17) — welcome tutorial + new start flow

- New flow: **Welcome tutorial** (first launch only) → **Home / mode select** (Player vs AI, Player vs Player) → **AI setup** (play as Goats/Tigers + Easy/Medium/Hard, Start) → game. Player vs Player goes straight to the game.
- `WelcomeScreen`: `HorizontalPager` with an intro page + one page per `HOW_TO_PLAY_STEPS` rule (reuses `RulePicture`, now shared with How to Play), page dots, Skip / Next / Let's play. Finishing or skipping stores `tutorial_seen` (`SettingsRepository.tutorialSeen`, written via `SerialWriter`).
- `AppViewModel` reads `tutorialSeen` once per launch to pick the nav start destination (welcome vs home); app shows blank until it's read.
- `AiSetupScreen` (no view model; choices are `rememberSaveable`, default Goats/Medium). Starting a game pops back to Home, so Back from the game shows Resume.
- Home lost the inline side/difficulty pickers. Removed strings `home_vs_computer`, `home_play`, `home_two_player`.
- Tests: `WelcomeFlowTest` (Next through all pages; Skip), new AppFlowTest case for AI setup, `tutorialSeen` repository test. `AppFlowTest` marks the tutorial seen in its reset. All unit + 43 UI tests pass on the Medium_Phone emulator.
- New strings need Hindi/Odia/Nepali translations when those locales are added.

## Session 2026-10-25 (16) — piece size balance

- Goat looked bigger than the tiger (goat filled ~90% of its 100×100 canvas, tiger only ~62%). Rescaled in `design/`: tiger group `scale(0.82)` → `0.9`, goat wrapped in a `scale(0.85)` group; both centred on (50, 54). Regenerated `ic_tiger` / `ic_goat` with `scripts/convert-svgs.sh`.
- Not yet checked on a device.

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
