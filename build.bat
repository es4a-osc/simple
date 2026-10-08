@ECHO OFF
REM 构建Simple的编译器和运行库 xhwsd@qq.com 2026-8-31


REM ---定义变量---
REM Ant根目录
FOR %%I IN ("%~dp0..\sdk\tools\apache-ant-1.9.15") DO SET "ANT_HOME=%%~fI"
REM JavaSDK根目录
FOR %%I IN ("%~dp0..\sdk\tools\jdk1.8.0_503") DO SET "JAVA_HOME=%%~fI"
REM AndroidSDK根目录（编译Simple运行库需要）
FOR %%I IN ("%~dp0..\sdk\tools\android") DO SET "ANDROID_HOME=%%~fI"
REM Simple根目录（编译器和所有运行库JAR包所在的根目录）
FOR %%I IN ("%~dp0dist\windows") DO SET "SIMPLE_HOME=%%~fI"
REM Ant编译脚本文件
FOR %%I IN ("%~dp0build.xml") DO SET "BUILD_FILE=%%~fI"


REM ---初始化---
REM 取build.xml所在目录
FOR /f %%a IN ("%BUILD_FILE%") DO (
	SET FOLDER=%%~dpa
)
REM 切换到build.xml所在目录
CD /D %FOLDER%
REM 设置path变量
SET path=%JAVA_HOME%\bin;%ANT_HOME%\bin;%path%
REM 设置classpath变量
SET classpath=.;%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\lib\dt.jar;%classpath%


REM ---执行ANT---
REM 调用ANT批处理
CALL ant.bat -buildfile %BUILD_FILE% clean all


:结束脚本
ECHO.
PAUSE