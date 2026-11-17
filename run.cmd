@echo off
setlocal
pushd "%~dp0backend"
if errorlevel 1 exit /b 1
if not defined SPRING_PROFILES_ACTIVE set "SPRING_PROFILES_ACTIVE=demo"
call ".\mvnw.cmd" --no-transfer-progress -DskipTests package
if errorlevel 1 (
  popd
  exit /b 1
)
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java.exe" -jar ".\target\restaurant-order-management-0.0.1-SNAPSHOT.jar" %*
) else (
  java -jar ".\target\restaurant-order-management-0.0.1-SNAPSHOT.jar" %*
)
set "APP_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %APP_EXIT_CODE%
