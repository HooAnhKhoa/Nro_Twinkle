# Admin panel

The dashboard now follows the supplied light blue panel reference. Boss editing opens a six-column portrait grid and a compact editor. Portraits resolve `BossData.outfit[0]` through the database `part` table to existing `/icon/<id>.png` files.

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
