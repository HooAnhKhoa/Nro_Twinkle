@ECHO OFF
setlocal
cd /d "%~dp0"
set "GAME_JAVA=java"
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "GAME_JAVA=C:\Program Files\Java\jdk-17\bin\java.exe"
:launch
powershell -NoProfile -Command "if(Get-NetTCPConnection -LocalPort 14445 -State Listen -ErrorAction SilentlyContinue){exit 1}else{exit 0}"
if errorlevel 1 (
  echo Game dang chay tren cong 14445. Khong can mo them server.
  pause
  exit /b 0
)
set "GAME_JAR="
for %%J in (Teamobi2026-panel-launcher.jar Teamobi2026-panel-no-fragments.jar Teamobi2026-panel-stones.jar Teamobi2026-panel-network.jar Teamobi2026-panel-drops.jar Teamobi2026-panel-stars.jar Teamobi2026-panel-maps.jar Teamobi2026-panel.jar Teamobi2026.jar) do if not defined GAME_JAR if exist "%%J" set "GAME_JAR=%%J"
if not defined GAME_JAR (
  echo Khong tim thay file JAR cua game.
  pause
  exit /b 1
)
echo Dang chay %GAME_JAR%
"%GAME_JAVA%" -server -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar "%GAME_JAR%"
set "GAME_EXIT=%ERRORLEVEL%"
if "%GAME_EXIT%"=="42" (
  echo Da luu du lieu. Tu khoi dong lai sau 10 giay trong cua so nay...
  powershell -NoProfile -Command "Start-Sleep -Seconds 10"
  goto launch
)
echo Game da dung. Ma thoat: %GAME_EXIT%
pause
exit /b %GAME_EXIT%