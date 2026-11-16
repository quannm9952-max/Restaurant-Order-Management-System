@echo off
setlocal

pushd "%~dp0backend"
if errorlevel 1 exit /b 1

call ".\mvnw.cmd" --no-transfer-progress verify %*
set "TV5_VERIFY_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %TV5_VERIFY_EXIT_CODE%
