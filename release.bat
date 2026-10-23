@ECHO OFF
SETLOCAL
REM 发布Simple的编译器、运行库和示例 xhwsd@qq.com 2026-10-14

REM --------------------------------------------------

:定义变量
REM Simple构建目录
FOR %%I IN ("%~dp0dist\windows") DO SET "SIMPLE_HOME=%%~fI"
REM SDK根目录
FOR %%I IN ("%~dp0..\sdk") DO SET "SDK_HOME=%%~fI"
REM 示例源码目录
FOR %%I IN ("%~dp0examples") DO SET "EXAMPLES_HOME=%%~fI"
REM SDK交付目录
SET "RELEASE_DIR=%SDK_HOME%\simple"
SET "EXAMPLES_RELEASE_DIR=%RELEASE_DIR%\examples"

REM --------------------------------------------------

:检查文件
IF NOT EXIST "%SIMPLE_HOME%\SimpleCompiler.jar" (
	ECHO - 未找到编译器包: "%SIMPLE_HOME%\SimpleCompiler.jar"
	GOTO 失败退出
)
IF NOT EXIST "%SIMPLE_HOME%\SimpleAndroidRuntime.jar" (
	ECHO - 未找到运行库包: "%SIMPLE_HOME%\SimpleAndroidRuntime.jar"
	GOTO 失败退出
)

IF NOT EXIST "%EXAMPLES_HOME%\" (
	ECHO - 未找到示例目录: "%EXAMPLES_HOME%"
	GOTO 失败退出
)

REM 拒绝空示例目录，避免误清空SDK中的现有示例。
SET "EXAMPLES_FOUND="
FOR /D %%I IN ("%EXAMPLES_HOME%\*") DO IF EXIST "%%~fI\project.properties" SET "EXAMPLES_FOUND=1"
IF NOT DEFINED EXAMPLES_FOUND (
	ECHO - 示例目录中没有项目。
	GOTO 失败退出
)

REM 检验simple目录
IF NOT EXIST "%RELEASE_DIR%" (
	ECHO - 未找到文件目录: "%RELEASE_DIR%"
	GOTO 失败退出
)

REM --------------------------------------------------

:复制文件
REM 复制编译器和运行库
FOR %%F IN (SimpleCompiler.jar SimpleAndroidRuntime.jar) DO (
	COPY /Y "%SIMPLE_HOME%\%%F" "%RELEASE_DIR%\%%F" >NUL
	IF ERRORLEVEL 1 (
		ECHO - 复制文件失败: %%F
		GOTO 失败退出
	)
)
ECHO - 复制编译器和运行库成功！

REM --------------------------------------------------

:发布示例
REM 仅替换SDK的examples目录，保留同级清单、图标和其它交付文件。
FOR %%I IN ("%EXAMPLES_RELEASE_DIR%\..") DO IF /I NOT "%%~fI" == "%RELEASE_DIR%" (
	ECHO - 示例发布目录不在SDK的simple目录内。
	GOTO 失败退出
)
IF EXIST "%EXAMPLES_RELEASE_DIR%" (
	RD /Q /S "%EXAMPLES_RELEASE_DIR%"
	IF EXIST "%EXAMPLES_RELEASE_DIR%" (
		ECHO - 清理旧示例失败: "%EXAMPLES_RELEASE_DIR%"
		GOTO 失败退出
	)
)
MD "%EXAMPLES_RELEASE_DIR%"
IF ERRORLEVEL 1 GOTO 失败退出

REM 复制examples根目录文件；ROBOCOPY的0至7均为成功状态，8及以上才是失败。
ROBOCOPY "%EXAMPLES_HOME%" "%EXAMPLES_RELEASE_DIR%" /R:0 /W:0 >NUL
IF ERRORLEVEL 8 (
	ECHO - 复制示例目录文件失败。
	GOTO 失败退出
)
REM 按项目复制，只排除项目根目录的build，不误排除资源中同名的目录。
FOR /D %%I IN ("%EXAMPLES_HOME%\*") DO (
	ROBOCOPY "%%~fI" "%EXAMPLES_RELEASE_DIR%\%%~nxI" /E /XD "%%~fI\build" /R:0 /W:0 >NUL
	IF ERRORLEVEL 8 (
		ECHO - 复制示例失败: %%~nxI
		GOTO 失败退出
	)
)
ECHO - 复制示例成功！

REM --------------------------------------------------

:成功退出
ECHO.
IF /I NOT "%~1" == "--no-pause" PAUSE
EXIT /B 0

REM --------------------------------------------------

:失败退出
ECHO.
IF /I NOT "%~1" == "--no-pause" PAUSE
EXIT /B 1
