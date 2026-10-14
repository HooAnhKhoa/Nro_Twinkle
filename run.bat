@ECHO OFF
cd /d "%~dp0"
if exist Teamobi2026-panel-maps.jar (
  java -server -Dfile.encoding=UTF-8 -jar Teamobi2026-panel-maps.jar
) else if exist Teamobi2026-panel.jar (
  java -server -Dfile.encoding=UTF-8 -jar Teamobi2026-panel.jar
) else (
  java -server -Dfile.encoding=UTF-8 -jar Teamobi2026.jar
)
PAUSE
