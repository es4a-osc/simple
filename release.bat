@ECHO OFF
REM 发布Simple的编译器和运行库 xhwsd@qq.com 2026-10-24

REM --------------------------------------------------

:定义变量
REM Simple构建目录
FOR %%I IN ("%~dp0dist\windows") DO SET "SIMPLE_HOME=%%~fI"
REM SDK根目录
FOR %%I IN ("%~dp0..\sdk") DO SET "SDK_HOME=%%~fI"
REM SDK交付目录
SET "RELEASE_DIR=%SDK_HOME%\simple"

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

:成功退出
ECHO.
IF /I NOT "%~1" == "--no-pause" PAUSE
EXIT /B 0

REM --------------------------------------------------

:失败退出
ECHO.
IF /I NOT "%~1" == "--no-pause" PAUSE
EXIT /B 1
