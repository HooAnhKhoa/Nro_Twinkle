@ECHO OFF
cd /d "%~dp0"
rem Legacy entry point for old JARs; new JARs restart inside run.bat.
echo Server se tu khoi dong lai sau 10 giay...
timeout /t 10 /nobreak
call run.bat
