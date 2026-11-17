@echo off
call "%~dp0verify.cmd" %*
exit /b %ERRORLEVEL%
