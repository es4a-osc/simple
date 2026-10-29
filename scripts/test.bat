@ECHO OFF
REM 运行库设备测试，仅覆盖依赖Android环境的功能 xhwsd@qq.com 2026-10-23

REM --------------------------------------------------

:定义变量
REM Simple项目文件
FOR %%I IN ("%~dp0..\tests\simple\runtime\DeviceTests\project.properties") DO SET "PROJECT_FILE=%%~fI"

REM Simple项目文件
FOR %%I IN ("%~dp0..\tests\simple\runtime\DeviceTests\build\apks\signed.apk") DO SET "APK_FILE=%%~fI"

REM JavaSDK根目录
FOR %%I IN ("%~dp0..\..\sdk\tools\jdk-11.0.32.1") DO SET "JAVA_HOME=%%~fI"

REM AndroidSDK的API等级
SET API_LEVEL=30

REM AndroidSDK根目录，必须包含：
REM .\platform-tools\adb.exe
REM .\platforms\android-%API_LEVEL%\android.jar
REM .\platforms\android-%API_LEVEL%\tools\lib\d8.jar
REM .\platforms\android-%API_LEVEL%\tools\aapt2.exe
FOR %%I IN ("%~dp0..\..\sdk\tools\android-sdk") DO SET "ANDROID_HOME=%%~fI"

REM Simple根目录，必须包含：
REM .\SimpleAndroidRuntime.jar
REM .\SimpleCompiler.jar
REM .\libs\*.jar
FOR %%I IN ("%~dp0..\dist\windows") DO SET "SIMPLE_HOME=%%~fI"

REM 扩展库根目录，可选；目录不存在时编译器只加载Simple运行库
FOR %%I IN ("%~dp0..\..\sdk\libraries") DO SET "LIBRARIES_HOME=%%~fI"

REM --------------------------------------------------

:设置环境变量
REM 设置path环境变量
SET path=%JAVA_HOME%\bin;%path%
REM 设置classpath环境变量
SET classpath=.

REM --------------------------------------------------

:编译应用
ECHO [编译应用]
REM 通过 java.exe 使用编译器编译指定项目
"%JAVA_HOME%\bin\java.exe" -classpath "%ANDROID_HOME%\platforms\android-%API_LEVEL%\tools\lib\d8.jar;%SIMPLE_HOME%\SimpleCompiler.jar;%SIMPLE_HOME%\libs\*" simple.compiler.Main "%PROJECT_FILE%"
IF NOT %ERRORLEVEL% == 0 (
	ECHO - 编译应用失败！
	GOTO 失败退出
)

REM --------------------------------------------------

:安装应用
ECHO.
ECHO [安装应用]
REM 检验.apk文件
IF NOT EXIST "%APK_FILE%" (
	ECHO - 未找到应用文件: "%APK_FILE%"
	GOTO 失败退出
)

REM 检验adb.exe文件
SET "ADB_FILE=%ANDROID_HOME%\platform-tools\adb.exe"
IF NOT EXIST "%ADB_FILE%" (
	ECHO - 未找到ADB工具: "%ADB_FILE%"
	GOTO 失败退出
)

REM ADB安装应用，失败时显示原始输出
SET "OUTPUT_FILE=%TEMP%\es4a-debug-%RANDOM%-%RANDOM%.log"
"%ADB_FILE%" install -r "%APK_FILE%" > "%OUTPUT_FILE%" 2>&1
IF NOT %ERRORLEVEL% == 0 (
	REM 将输出内容显示到终端
	powershell.exe -NoProfile -Command "Get-Content -LiteralPath $env:OUTPUT_FILE -Encoding UTF8"
	ECHO - 应用安装失败！
	GOTO 失败退出
)
ECHO 安装应用成功。

REM --------------------------------------------------

:启动应用
ECHO.
ECHO [启动应用]
REM 默认使用原版AAPT2；存在UTF-8副本时优先使用，以兼容中文路径。
REM 仅在副本不存在时回退，执行失败仍按原流程报告。
SET "AAPT_FILE=%ANDROID_HOME%\platforms\android-%API_LEVEL%\tools\aapt2.exe"
IF EXIST "%ANDROID_HOME%\platforms\android-%API_LEVEL%\tools\aapt2_utf8.exe" (
	SET "AAPT_FILE=%ANDROID_HOME%\platforms\android-%API_LEVEL%\tools\aapt2_utf8.exe"
)
IF NOT EXIST "%AAPT_FILE%" (
	ECHO - 未找到AAPT2工具: "%AAPT_FILE%"
	GOTO 失败退出
)

REM 从APK取包名
SET "PACKAGE_NAME="
FOR /F "tokens=2 delims='" %%I IN ('CALL "%AAPT_FILE%" dump badging "%APK_FILE%" 2^>NUL ^| FINDSTR /B /C:"package: name="') DO SET "PACKAGE_NAME=%%I"
IF NOT DEFINED PACKAGE_NAME (
	ECHO - 未能从APK取得应用包名: "%APK_FILE%"
	GOTO 失败退出
)

REM ADB启动应用，并检查实际启动结果
"%ADB_FILE%" shell am start -W -n %PACKAGE_NAME%/simple.runtime.android.MainActivity > "%OUTPUT_FILE%" 2>&1
IF %ERRORLEVEL% == 0 (
	REM 在输出内容中查询文本
	FINDSTR /X /C:"Status: ok" "%OUTPUT_FILE%" >NUL
)
IF NOT %ERRORLEVEL% == 0 (
	REM 将输出内容显示到终端
	powershell.exe -NoProfile -Command "Get-Content -LiteralPath $env:OUTPUT_FILE -Encoding UTF8"
	ECHO - 应用启动失败！
	GOTO 失败退出
)
ECHO 启动应用成功。

REM --------------------------------------------------

:成功退出
REM 删除输出文件
IF DEFINED OUTPUT_FILE IF EXIST "%OUTPUT_FILE%" DEL /Q "%OUTPUT_FILE%"
ECHO.
IF /I NOT "%~1" == "--no-pause" PAUSE
EXIT /B 0

REM --------------------------------------------------

:失败退出
REM 删除输出文件
IF DEFINED OUTPUT_FILE IF EXIST "%OUTPUT_FILE%" DEL /Q "%OUTPUT_FILE%"
ECHO.
IF /I NOT "%~1" == "--no-pause" PAUSE
EXIT /B 1
