:: ------------------------------------------------------------------------------------------
:: Convenience script to invoke the Simple compiler
::
:: Before using this script, make sure the following two environment variables are defined:
::   ANDROID_HOME    root directory of Android SDK (must contain android-26)
::   SIMPLE_HOME     root directory of Simple distribution (same as directory of this script)
:: ------------------------------------------------------------------------------------------

java -classpath "%ANDROID_HOME%\tools\lib\apkbuilder.jar;%ANDROID_HOME%\platforms\android-26\tools\lib\d8.jar;%ANDROID_HOME%\tools\lib\androidprefs.jar;%ANDROID_HOME%\tools\lib\jarutils.jar;%SIMPLE_HOME%\SimpleCompiler.jar" simple.compiler.Main %*
