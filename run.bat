@ECHO OFF
cd /d "%~dp0"
powershell -NoProfile -Command "if(Get-NetTCPConnection -LocalPort 14445 -State Listen -ErrorAction SilentlyContinue){exit 1}else{exit 0}"
if errorlevel 1 (
  echo Game server is already running on port 14445.
  exit /b 0
)
if exist Teamobi2026-panel-stars.jar (
  java -server -Dfile.encoding=UTF-8 -jar Teamobi2026-panel-stars.jar
) else if exist Teamobi2026-panel-maps.jar (
  java -server -Dfile.encoding=UTF-8 -jar Teamobi2026-panel-maps.jar
) else if exist Teamobi2026-panel.jar (
  java -server -Dfile.encoding=UTF-8 -jar Teamobi2026-panel.jar
) else (
  java -server -Dfile.encoding=UTF-8 -jar Teamobi2026.jar
)
PAUSE
