@echo off
setlocal

pushd "%~dp0backend"
if errorlevel 1 exit /b 1

call ".\mvnw.cmd" --no-transfer-progress -DskipTests package
if errorlevel 1 (
  popd
  exit /b 1
)

rem The frontend is packaged by Maven; the JAR path stays relative on Windows.
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java.exe" -jar ".\target\restaurant-order-management-0.0.1-SNAPSHOT.jar" %*
) else (
  java -jar ".\target\restaurant-order-management-0.0.1-SNAPSHOT.jar" %*
)
set "TV5_APP_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %TV5_APP_EXIT_CODE%
