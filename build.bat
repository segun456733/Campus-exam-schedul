@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
echo ==========================================================
echo  Campus Examination Scheduler - build script
echo ==========================================================

rem ---- 1. Java (JDK 17 or newer)
where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java was not found on this computer.
  echo         Install a JDK 17 or newer, for example Eclipse Temurin: https://adoptium.net
  echo         Then close and reopen this window and run build.bat again.
  exit /b 1
)
set "JAVAVER="
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVAVER=%%g"
set "JAVAVER=%JAVAVER:"=%"
for /f "delims=. tokens=1" %%a in ("%JAVAVER%") do set "JAVAMAJOR=%%a"
echo Java version found: %JAVAVER%
if %JAVAMAJOR% LSS 17 (
  echo [ERROR] Java %JAVAVER% is too old. This project needs JDK 17 or newer.
  exit /b 1
)

rem ---- 2. Maven
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Apache Maven was not found.
  echo         1. Download the "Binary zip archive" from https://maven.apache.org/download.cgi
  echo         2. Unzip it, e.g. to C:\Tools\apache-maven-3.9.9
  echo         3. Add C:\Tools\apache-maven-3.9.9\bin to your PATH environment variable
  echo         4. Make sure JAVA_HOME points to your JDK folder
  echo         5. Open a NEW command window and run build.bat again.
  exit /b 1
)

rem ---- 3. Build + tests (the first build needs internet to download dependencies; later builds work offline)
set "MVNARGS=-B clean package"
if /i "%~1"=="skiptests" set "MVNARGS=-B clean package -DskipTests"
if /i "%~1"=="offline" set "MVNARGS=-B -o clean package"
echo.
echo Running: mvn %MVNARGS%
call mvn %MVNARGS%
if errorlevel 1 (
  echo.
  echo [ERROR] BUILD FAILED. Read the messages above. Compilation errors or failed tests stop the build.
  exit /b 1
)

if not exist "target\campus-exam-scheduler.jar" (
  echo [ERROR] The build finished but target\campus-exam-scheduler.jar was not created.
  exit /b 1
)

echo.
echo ==========================================================
echo  BUILD SUCCESSFUL
echo  Runnable JAR : %CD%\target\campus-exam-scheduler.jar
echo.
echo  Launch it by double-clicking run.bat, or run:
echo      java -jar target\campus-exam-scheduler.jar
echo  First login   : username admin / password Admin@123
echo  The built JAR runs offline and does not need Maven.
echo ==========================================================
endlocal
