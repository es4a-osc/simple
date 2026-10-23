@ECHO OFF
SETLOCAL

REM 查看运行库设备测试日志 xhwsd@qq.com 2026-10-23
REM 不带参数时实时监听；参数原样传给logcat，例如：-d -t 200（抓取后退出）。
REM 保存UTF-8日志到本机临时目录：logcat.bat -d -t 200 > "%TEMP%\device-tests.log"

REM --------------------------------------------------

:定义变量
REM ADB文件
FOR %%I IN ("%~dp0..\sdk\tools\android\tools\adb.exe") DO SET "ADB_FILE=%%~fI"
IF NOT EXIST "%ADB_FILE%" (
	ECHO 未找到ADB工具："%ADB_FILE%" 1>&2
	EXIT /B 1
)

REM 包名
SET "PACKAGE_NAME=simple.runtime.tests"

REM --------------------------------------------------

REM 自动化可通过ANDROID_SERIAL指定设备；否则选择最后一个在线模拟器。
REM 同时识别emulator编号和本机TCP连接，不把离线设备或USB真机当作候选。
SET "DEVICE_SERIAL=%ANDROID_SERIAL%"
IF NOT DEFINED DEVICE_SERIAL (
	FOR /F "skip=1 tokens=1,2" %%D IN ('CALL "%ADB_FILE%" devices') DO (
		IF "%%E" == "device" (
			FOR /F "tokens=1 delims=-:" %%S IN ("%%D") DO (
				IF "%%S" == "emulator" SET "DEVICE_SERIAL=%%D"
				IF "%%S" == "127.0.0.1" SET "DEVICE_SERIAL=%%D"
				IF "%%S" == "localhost" SET "DEVICE_SERIAL=%%D"
			)
		)
	)
)
IF NOT DEFINED DEVICE_SERIAL (
	ECHO 未找到在线模拟器，请先启动模拟器。 1>&2
	EXIT /B 1
)

REM 优先读取当前用户的完整UID；新版dumpsys的appId不等于多用户环境的UID。
SET "APP_UID="
FOR /F "tokens=4 delims=: " %%U IN ('CALL "%ADB_FILE%" -s "%DEVICE_SERIAL%" shell pm list packages -U --user current %PACKAGE_NAME% 2^>NUL ^| FINDSTR /B /C:"package:%PACKAGE_NAME% uid:"') DO (
	SET "APP_UID=%%U"
)
REM 旧系统不支持pm -U时，继续使用原有的userId字段。
IF NOT DEFINED APP_UID (
	FOR /F "tokens=2 delims== " %%U IN ('CALL "%ADB_FILE%" -s "%DEVICE_SERIAL%" shell dumpsys package %PACKAGE_NAME% ^| FINDSTR /R /C:"^[ ]*userId="') DO (
		SET "APP_UID=%%U"
	)
)
IF NOT DEFINED APP_UID (
	ECHO 未能取得应用UID，请确认设备 %DEVICE_SERIAL% 已安装：%PACKAGE_NAME% 1>&2
	EXIT /B 1
)

TITLE logcat - %DEVICE_SERIAL%

REM 根据设备实际支持的参数选择实现，两条路径均按UID筛选，支持应用重启。
"%ADB_FILE%" -s "%DEVICE_SERIAL%" shell logcat --help 2>NUL | FINDSTR /C:"--uid=" >NUL
IF NOT ERRORLEVEL 1 (
	"%ADB_FILE%" -s "%DEVICE_SERIAL%" logcat --uid=%APP_UID% -v threadtime %*
	GOTO 返回日志结果
)

REM Android 7没有--uid，用固定的threadtime和UID列在本机过滤，保持UTF-8原始字节。
REM PowerShell按UTF-8逐行读取并立即输出，避免管道缓冲延迟和长日志行截断。
REM 单独记录ADB成功或失败状态，不把过滤器未匹配到日志当成抓取失败。
SET "LOGCAT_EXIT_FILE=%TEMP%\es4a-logcat-%RANDOM%-%RANDOM%.exit"
("%ADB_FILE%" -s "%DEVICE_SERIAL%" logcat %* -v threadtime -v uid && ECHO 0 > "%LOGCAT_EXIT_FILE%" || ECHO 1 > "%LOGCAT_EXIT_FILE%") | powershell.exe -NoLogo -NoProfile -Command ^
	"$utf8 = New-Object System.Text.UTF8Encoding($false);" ^
	"$reader = New-Object System.IO.StreamReader([Console]::OpenStandardInput(), $utf8);" ^
	"$writer = New-Object System.IO.StreamWriter([Console]::OpenStandardOutput(), $utf8);" ^
	"$writer.AutoFlush = $true;" ^
	"$pattern = '^\d{2}-\d{2} \S+\s+' + [regex]::Escape($env:APP_UID) + '\s';" ^
	"try { while ($null -ne ($line = $reader.ReadLine())) { if ($line -match $pattern) { $writer.WriteLine($line); } } }" ^
	"finally { $writer.Dispose(); $reader.Dispose(); }"
SET "LOGCAT_FILTER_EXIT_CODE=%ERRORLEVEL%"
SET "LOGCAT_EXIT_CODE=1"
IF EXIST "%LOGCAT_EXIT_FILE%" (
	SET /P "LOGCAT_EXIT_CODE=" < "%LOGCAT_EXIT_FILE%"
	DEL /Q "%LOGCAT_EXIT_FILE%"
)
IF NOT "%LOGCAT_FILTER_EXIT_CODE%" == "0" EXIT /B %LOGCAT_FILTER_EXIT_CODE%
ENDLOCAL & EXIT /B %LOGCAT_EXIT_CODE%

:返回日志结果
ENDLOCAL & EXIT /B %ERRORLEVEL%
