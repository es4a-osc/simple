@ECHO OFF
REM logcat.ps1 entry xhwsd@qq.com 2026-10-30
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0logcat.ps1" %*
EXIT /B %ERRORLEVEL%
