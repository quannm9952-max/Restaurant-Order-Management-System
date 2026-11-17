@echo off
setlocal
pushd "%~dp0"
where docker >nul 2>&1
if errorlevel 1 (
  echo Docker Desktop is required. Install it and reopen the terminal.
  popd
  exit /b 1
)
docker compose version >nul 2>&1
if errorlevel 1 (
  echo Docker Compose is required. Update Docker Desktop.
  popd
  exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
  echo Start Docker Desktop and wait until its engine is running.
  popd
  exit /b 1
)
if not exist ".env" copy ".env.example" ".env" >nul
docker compose up --build --detach %*
set "APP_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %APP_EXIT_CODE%
