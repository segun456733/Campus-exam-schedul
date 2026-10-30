@echo off
cd /d "%~dp0"
if not exist "target\campus-exam-scheduler.jar" (
  echo The application has not been built yet. Run build.bat first.
  pause
  exit /b 1
)
where javaw >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install JDK or JRE 17 or newer from https://adoptium.net
  pause
  exit /b 1
)
start "" javaw -jar "target\campus-exam-scheduler.jar"
