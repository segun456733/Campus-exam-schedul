@echo off
setlocal
cd /d "%~dp0"
rem Creates a Windows app-image (CampusExamScheduler.exe + bundled Java runtime) with jpackage.
rem Needs: JDK 17+ (jpackage is included) and a finished build (run build.bat first).
rem For a real installer (.exe) use:  package-exe.bat installer   (requires the free WiX Toolset 3.x on PATH)
where jpackage >nul 2>nul
if errorlevel 1 (
  echo [ERROR] jpackage was not found. It ships with JDK 17 or newer - check JAVA_HOME and PATH.
  exit /b 1
)
if not exist "target\campus-exam-scheduler.jar" (
  echo [ERROR] Run build.bat first.
  exit /b 1
)
if exist dist rmdir /s /q dist
set "TYPE=app-image"
if /i "%~1"=="installer" set "TYPE=exe"
jpackage --type %TYPE% --name CampusExamScheduler --app-version 1.0.0 --input target ^
  --main-jar campus-exam-scheduler.jar --main-class com.campus.examscheduler.Main --dest dist ^
  --vendor "Campus Examination Scheduler" --description "Offline examination timetable scheduler"
if errorlevel 1 (
  echo [ERROR] jpackage failed. For the installer type, install WiX Toolset 3.x first.
  exit /b 1
)
echo Done. Output is in the dist folder.
if "%TYPE%"=="app-image" echo Start it with: dist\CampusExamScheduler\CampusExamScheduler.exe
endlocal
