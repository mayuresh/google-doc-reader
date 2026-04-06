@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off

@rem Set local scope for variables
setlocal

set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve JAVA_HOME
set JAVA_EXE=java.exe
set JAVA_CMD="%JAVA_HOME%/bin/%JAVA_EXE%"

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set MAIN_CLASS=org.gradle.wrapper.GradleWrapperMain

"%JAVA_CMD%" -Xmx64m -Xms64m -Dorg.gradle.appname="%APP_BASE_NAME%" -classpath "%CLASSPATH%" %MAIN_CLASS% %*

:end
endlocal
