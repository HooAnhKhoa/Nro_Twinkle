# Admin panel

The dashboard now follows the supplied light blue panel reference. Boss editing opens a six-column portrait grid and a compact editor. Portraits resolve `BossData.outfit[0]` through the database `part` table to existing `/icon/<id>.png` files.

Boss image update (14 October 2026): `boss_image.php` reads the original game PNG files directly, preferring `data/icon/x4`, then x2/x1 and the website icon directory. Copy `web/game_paths.example.php` to `game_paths.php` in the deployed website root and set the game directory. Empty icon frames are skipped. Legacy Boss rows with head_id=0 resolve from the generated `boss_images.json` catalog by Boss key, then unambiguous name/map or name. The exporter reads Java metadata without starting the game or altering the database. The main-PC browser check confirmed all 237 configured Boss images load. Original boss.php is backed up in `C:\Downloads\NroFreeSetup\Backups\boss-images-20261014`.

Removed: Chế độ Admin, Đổi Sự Kiện, XNXX, Tài Nguyên, Vòng Quay, Link Share, Phúc Lợi, Tặng Item, Update Database mới. Administrator access checks remain required.

## Behavior

- **Tặng Cá:** select a player, resolve `player.account_id`, add to `account.vnd`. The character must be offline. Account balance is locked in a transaction; repeated request keys cannot add twice. The history allows undo if the account is offline and has enough VNĐ remaining.
- **Boss:** configure HP, damage, respawn delay and enabled state. Settings apply to the first form on the next spawn. Spawn runs on the boss update thread; chain, child and dungeon bosses retain their game rules. Queued spawn and admin commands expire after 30 seconds. HP/damage overrides also apply after specialized spawn logic such as Broly, before the map broadcasts the character.
- **EXP:** changes the existing `Manager.RATE_EXP_SERVER` multiplier.
- **Đập đồ:** controls upgrade levels +0 through +8 in `CombineSystem.getTileNangCapDo`; accessory upgrades, crafting and star fusion retain their separate rules.
- **Rơi đồ:** scales random chance checks in `Mob.getItemMobReward`. Map, task and equipment conditions, guaranteed rewards and boss rewards remain governed by existing code.
- **Dame Skill:** multiplies player skill damage, including the early-return calculations for Ma Kankō Sappō, Quả cầu kênh khí and the self-destruct calculation. Boss damage stays under Boss settings. Support skills without damage have no damage to multiply.
- **Kick:** uses the server's existing save/disconnect procedure.
- **Bảo trì:** invokes the existing 60-second maintenance countdown. The original server then saves players, exits and calls `restart_server.bat`, which starts it again after five minutes.
- **Chỉ số:** opens the existing player editor with character and pet stats and inventory. Account, player, item, shop, map, giftcode and transaction tools retain the existing implementations.
- **Nội tại:** edits the correct intrinsic array positions 0–2 and preserves the remaining state. Templates enforce race and parameter ranges. Offline edits are audited and can be undone while the current data still matches the saved revision.
- **Reset:** requires an offline server, no accounts marked online, InnoDB tables and typed confirmation. Backs up account, clan, player, history_transaction and super_rank before deleting their reset data in one transaction. Administrator accounts remain.

## Run locally

PHP files are deployed to `C:\xampp\htdocs\admin`; open `http://localhost/admin/panel.php` using your existing administrator login. The original website provides authentication and assets. A fresh checkout requires copying `web/config.example.php` to `web/config.php`, configuring the database, and keeping the existing root website and `/icon` directory.

Run `build-panel.ps1` to compile Java 17 and overlay compiled application classes onto the original bundled JAR. The original `Teamobi2026.jar` is retained. The resulting `Teamobi2026-panel.jar` is generated locally and ignored by Git. `run.bat` chooses the panel JAR when present. Launch it from the game directory after stopping any existing game instance through its normal maintenance procedure.

The Java bridge reads SQL schema files from `web/` relative to the game directory. New tables are additive and installed by the CLI-only `web/admin/install_panel.php`. Existing Boss tables receive the `head_id` column automatically.

## Undo

Original live files and JAR: `C:\Downloads\NroFreeSetup\Backups\panel-before-20261013`.

Run `restore-panel.ps1` to restore the original live panel and startup script. This does not alter player data. A running JVM needs a normal restart to load the original JAR.

Git checkpoints:

- `bd5bd4f`: original PHP admin panel.
- `1b3564e`: dashboard and audited VNĐ gifts.
- `a001a9e`: Boss and gameplay rate bridge.
- `8463129`: skill, intrinsic and guarded reset tools.

Reset backups have table names `panel_backup_YYYYmmdd_HHMMSS_<table>`. To restore a reset, stop the game and website writes, verify the five matching backup tables, and restore all five together in a database transaction. Do not restore just one table because player/account and clan relationships must remain consistent. VNĐ gifts and intrinsic edits have their own in-panel undo controls.

## Verification

All PHP files pass `php -l`; the full Java source compiles with JDK 17. Isolated QA database checks cover administrator access, CSRF, character search, VNĐ gifts, duplicate requests, undo, online protection, negative values, overflow, Boss persistence and image mapping, offline spawn rejection, rate revision conflicts, skill settings, intrinsic ranges and intrinsic undo.

`test/PanelBridgeIntegrationTest.java` checks the packaged JAR against the disposable `nro_panel_qa_20261013` database: Boss settings, specialized spawn stats, no repeated healing, EXP, upgrades, drop multipliers, skill damage, disabled Bosses, spawn acknowledgement and expired commands. It refuses other database names.

UI QA uses actual BossData templates exported by `test/ExportBossFixtures.java`, in a separate database and localhost PHP server. Screenshots and generated test files remain in ignored `build/` directories. Live player sessions, maintenance shutdown and production Reset have not been exercised.

2026-10-14: Publish actual live boss map/zone locations; preserve zero-second rest values. Group duplicate boss names with identical configured maps for display, retaining live zones and the current configuration key. Use Segoe UI/Tahoma/Arial for Vietnamese. Runtime packaging now uses a staging JAR; run.bat prefers the verified maps JAR. Verified 586 classes, 17 bridge integration checks, and 5 display grouping checks. Local backups: Backups/boss-maps-20261014 and Backups/boss-font-group-20261014 under NroFreeSetup.

2026-10-14 typography: shared web/typography.css applies Segoe UI/Tahoma/Arial to 26 valid public/admin pages, 15–16px body/form text and readable metadata. Boss dialog enlarged; mobile cards wrap into fewer columns. PHP syntax checked before deployment; desktop and 390px responsive views checked without horizontal overflow. deploy-typography.ps1 prepares, checks and backs up live pages. Existing broken images/avatar/zoom.php is skipped (unmatched brace predates this change). Backup: C:/Downloads/NroFreeSetup/Backups/web-typography-20261014-233140.

2026-10-14: Boss editor sorted by TaskService.checkDoneTaskKillBoss main/subtask progression, including Android 15 -> 14 -> 13 and Poc -> Pic -> King Kong. Mission headers separate stages; unrelated bosses follow afterward. Eight grouping/order checks passed. Backup: Backups/boss-task-order-20261014.

2026-10-14: Upgrade settings now have independent level (+0 to +8) and crystal star (0 to 9) chance arrays. Existing level settings preserved; star defaults use actual PhaLeHoaTrangBi chances. NPC preview now uses the actual star chance instead of getFakeRatio. Additive star columns, revision-protected API, full JAR staged as Teamobi2026-panel-stars.jar; run.bat prefers it. Verified 586 classes, 19 bridge assertions, four star API checks. Backup: Backups/star-rates-20261014.

2026-10-15: New drop_rates.php dashboard with global and per-category multipliers, actual base-roll previews and map conditions. Groups wired to Mob reward branches; x0 suppresses random drops, chance capped at 100%; task/free guaranteed rewards preserved. Shop-with-stars branches verified against ItemService. Upgrade stones have no existing mob reward branch and are shown unavailable. Revision-guarded transactional API: five QA checks passed; bridge: 24 checks passed.
Windows Selector initialization was failing despite a bound port. Network now uses a direct ServerSocket accept fallback if Selector.open fails, preserving existing session setup; real socket test and shutdown test passed. run.bat prefers verified Teamobi2026-panel-network.jar, uses Java 17/IPv4 and keeps the already-running notice visible. Verified live Network thread waiting in ServerSocket.accept. Maintenance countdown is 15s from panel/console/NPC/automatic paths; restart_server.bat waits 10s. A real maintenance/restart cycle completed with a new PID and healthy Network thread. Backup: Backups/drop-rates-20261015.

2026-10-15 upgrade stones: corrected material IDs 220–224 that had been misclassified as crystal-star drops. Ordinary maps now have one 2% stone roll; cold maps retain their single 20% roll. Each success drops one of five existing upgrade materials with existing item options. ID 225 is stone fragment and its existing 10% branches now belong to the stone category. True crystal stars 441–447 retain their own 10% branch and independent multiplier. Enabled upgrade_stone in schema/dashboard. Verified 587 runtime classes, 26 bridge checks and six API checks. Packaged separately as Teamobi2026-panel-stones.jar; rollback retains prior network JAR. Backup: Backups/upgrade-stones-20261015.

2026-10-15: Removed both mob reward branches for stone fragments (item 225). Five upgrade materials retain their 2% ordinary / 20% cold map roll. Panel previews now show only those two rates. No remaining direct item-225 world drop creation found in Java sources. JAR packaged separately, 587 classes verified; backup Backups/no-fragments-20261015.

2026-10-15 same-terminal restart: ServerManager.close saves as before, then exits with code 42 instead of launching cmd/start. run.bat restarts only for that code, waits 10 seconds and loops in the current console; other exits stop for inspection. Legacy restart_server.bat remains a 10-second compatibility entry for old JARs. Live verification: Java PID 10060 changed to 21588 while terminal parent PID remained 17752. Closed idle old restart terminal. Latest runtime: Teamobi2026-panel-launcher.jar; 587 application classes checked. Database backup: Backups/same-terminal-restart-20261015.

2026-10-15 dashboard redesign: replaced tile overview with light sidebar layout, live runtime metrics, grouped existing tools and quick actions. Overview displays top gold-bar players (item 457, bag + box, saved data) instead of bosses. No periodic-maintenance controls. Real activity rows come from panel_command and VNĐ audit; no synthetic server logs. Manual maintenance remains 15s with 10s same-terminal restart information. Gold leaderboard refreshes every 30s, metrics every 5s. PHP, JS checks passed; five inventory/ranking checks and three isolated API checks passed. Desktop and 390px mobile checked without horizontal overflow; existing Boss and VNĐ dialogs checked read-only. Backup: Backups/dashboard-redesign-20261015. No game restart needed.

2026-10-15 shared admin dashboard theme: added subtle pastel metric/action cards and shared admin-ui.css, admin-ui.js, admin_ui.php across 16 functional pages: accounts, players, giftcodes, giftcode creation, item/shop catalogs, gold/gem shops, maps, trades, intrinsic, reset, upgrades/EXP, skill damage, bosses and drops. Removed decorative GIF/banner clutter; retained form fields, IDs and action handlers. Unified readable Vietnamese titles, compact toolbars, scrollable tables, soft save/delete/cancel styles and responsive editors. Password fields are masked with an explicit reveal toggle; player-name fields remain text. Shop edit/delete controls stay visible without hover. Overview links close embedded dialogs rather than nesting dashboards. Verified PHP syntax, JS syntax, all 16 live page loads, desktop and mobile layouts, mobile shop add/cancel and embedded return navigation without saving/deleting production data. Live backup: C:/Downloads/NroFreeSetup/Backups/admin-theme-20261015. No game restart needed; screenshots in ignored build/admin-theme-dashboard.jpg and build/admin-theme-shop.jpg. Git push remains subject to user approval.
2026-10-15 inline dashboard navigation: all 14 functional menu destinations now open in the dashboard workspace beside the persistent sidebar instead of the modal dialog. Boss uses its full page layout. Quick links and giftcode chips use the same navigation; shop subpages retain the shell. Hash addresses restore the selected page on reload and support browser Back/Forward. Active menu, expanded group and page heading follow navigation. The workspace fits the viewport and scrolls long data pages internally. Existing operational confirmations (maintenance, VNĐ gifts, kick) and record editors remain intact. Shared child pages suppress duplicate outer headers and return-to-panel links use the parent overview. Verified every menu destination, shop subnavigation, overview return, browser Back, reload, canceling the maintenance confirmation and 390px mobile menu without horizontal overflow. No production data was saved/deleted and the game was not restarted. PHP and JS syntax checks passed. Backup: C:/Downloads/NroFreeSetup/Backups/admin-inline-pages-20261015; screenshot build/dashboard-inline-boss.jpg. Push requires user approval.
2026-10-15 player inventory layout: scoped inventory grids separately from shop item cards, fixing the shared product-grid override that made previews vertical and detail views only three columns wide. Bag and box previews now flow left-to-right in seven 36px slots per row, clip cleanly after three rows without fading, and include explicit view buttons. Detail grids use compact 40px slots and available width (18 columns in the checked desktop workspace, six on 390px mobile), retaining quantities, names on hover, empty slots and existing delete confirmations. Verified preview coordinates/124px limit, actual live bag detail contents, mobile modal sizing and close action without saving or deleting player data. PHP syntax and diff checks passed. Backup: C:/Downloads/NroFreeSetup/Backups/player-inventory-grid-20261015; screenshots build/inventory-three-rows.jpg and build/inventory-compact-detail.jpg. No game restart.

2026-10-15 inventory detail sizing: increased detail slots from 40px to 48px, icons from 28px to 36px and quantity labels to 12px; preview remains seven compact columns/three rows. Live desktop verified 15 columns with no horizontal overflow; PHP syntax passed. Backup: Backups/inventory-size-20261015; screenshot build/inventory-detail-larger.jpg.


2026-10-15 Giftcode item names: the content column renders actual template names, icons and quantities as wrapping chips. Raw JSON remains in the existing tracked input for unchanged save handling; clicking the preview opens the existing editor, and confirming edits refreshes the names. Search supports displayed names as well as raw IDs. Template names are inserted with textContent and PHP JSON uses HTML-safe flags. Checked live multi-item and single-item codes, name search (one result for Thỏi vàng), editor open/cancel with no changed fields, PHP syntax and diff checks. No Giftcode data was saved/deleted. Backup: Backups/giftcode-item-names-20261015; screenshot build/giftcode-item-names.jpg.
