@ECHO OFF
cd /d "%~dp0"
set "GAME_JAVA=java"
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "GAME_JAVA=C:\Program Files\Java\jdk-17\bin\java.exe"
powershell -NoProfile -Command "if(Get-NetTCPConnection -LocalPort 14445 -State Listen -ErrorAction SilentlyContinue){exit 1}else{exit 0}"
if errorlevel 1 (
  echo Game dang chay tren cong 14445. Khong can mo them server.
  pause
  exit /b 0
)
if exist Teamobi2026-panel-network.jar (
  "%GAME_JAVA%" -server -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar Teamobi2026-panel-network.jar
) else if exist Teamobi2026-panel-drops.jar (
  "%GAME_JAVA%" -server -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar Teamobi2026-panel-drops.jar
) else if exist Teamobi2026-panel-stars.jar (
  "%GAME_JAVA%" -server -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar Teamobi2026-panel-stars.jar
) else if exist Teamobi2026-panel-maps.jar (
  "%GAME_JAVA%" -server -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar Teamobi2026-panel-maps.jar
) else if exist Teamobi2026-panel.jar (
  "%GAME_JAVA%" -server -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar Teamobi2026-panel.jar
) else (
  "%GAME_JAVA%" -server -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -jar Teamobi2026.jar
)
PAUSE
