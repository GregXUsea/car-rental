@echo off
setlocal
set "JAVA_EXEC=java"
if defined JAVA_HOME (
  set "JAVA_EXEC=%JAVA_HOME%\bin\java.exe"
)
set "PROJECT_DIR=%~dp0"
set "WRAPPER_JAR=%PROJECT_DIR%.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
  echo Maven wrapper JAR not found: %WRAPPER_JAR%
  exit /B 1
)
"%JAVA_EXEC%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
