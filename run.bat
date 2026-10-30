@echo off
cd /d "%~dp0"
if not exist "target\campus-exam-scheduler.jar" (
  echo The application has not been built yet. Run build.bat first.
  pause
  exit /b 1
)
where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install JDK 17 or newer from https://adoptium.net
  pause
  exit /b 1
)
rem Started with "java" (not javaw) on purpose: if anything goes wrong the error stays visible in this window.
java -jar "target\campus-exam-scheduler.jar"
if errorlevel 1 (
  echo.
  echo The application stopped with an error. Read the message above.
  pause
)
