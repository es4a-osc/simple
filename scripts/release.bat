@ECHO OFF
REM release.ps1 entry xhwsd@qq.com 2026-10-29
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0release.ps1" %*
EXIT /B %ERRORLEVEL%
