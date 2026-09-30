@echo off
setlocal EnableExtensions
title PayNotify - Build
cd /d "%~dp0"

echo ============================================================
echo   PayNotify - build (Minecraft 1.21.11 / Fabric)
echo ============================================================
echo.

rem ---------- 1. Check for Java 21 ----------
set "JAVACMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVACMD=%JAVA_HOME%\bin\java.exe"

"%JAVACMD%" -version >nul 2>nul
if errorlevel 1 goto :nojava

set "JAVA_VERSION="
set "JAVA_MAJOR="
"%JAVACMD%" -version >"%TEMP%\paynotify_java.txt" 2>&1
for /f "tokens=3" %%g in ('findstr /i /c:"version" "%TEMP%\paynotify_java.txt"') do if not defined JAVA_VERSION set "JAVA_VERSION=%%~g"
del "%TEMP%\paynotify_java.txt" >nul 2>nul
for /f "delims=.-_+ tokens=1" %%a in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%a"

if not defined JAVA_MAJOR goto :javaunknown
if %JAVA_MAJOR% LSS 21 goto :oldjava
echo Found Java %JAVA_VERSION%.
goto :build

:javaunknown
echo Could not detect the Java version - continuing anyway.
goto :build

:nojava
echo [ERROR] Java was not found.
echo.
echo Building this mod needs a JDK 21 (Java Development Kit, not just the Minecraft launcher's Java).
echo Install "Temurin JDK 21" from https://adoptium.net/temurin/releases/?version=21
echo then run build.bat again.
goto :failed

:oldjava
echo [ERROR] Java %JAVA_VERSION% was found, but Java 21 or newer is required.
echo.
echo Install "Temurin JDK 21" from https://adoptium.net/temurin/releases/?version=21
echo then run build.bat again. If you have several Java versions installed, set JAVA_HOME
echo to the JDK 21 folder first.
goto :failed

rem ---------- 2. Build with the included Gradle wrapper ----------
:build
echo.
echo Building with the included Gradle wrapper.
echo The first build downloads Minecraft and Fabric and can take several minutes. An internet connection is required.
echo.
call "%~dp0gradlew.bat" build
if errorlevel 1 goto :failed

rem ---------- 3. Find the jar ----------
set "JAR="
for %%f in ("%~dp0build\libs\paynotify-*.jar") do call :checkjar "%%~ff"
if not defined JAR goto :nojar

echo.
echo ============================================================
echo   BUILD SUCCEEDED
echo ============================================================
echo.
echo Your mod jar is here:
echo   %JAR%
echo.
echo To use it, copy that file into your Minecraft "mods" folder
echo (usually %%appdata%%\.minecraft\mods) together with Fabric API,
echo and start Minecraft 1.21.11 with the Fabric loader.
echo.
start "" explorer.exe "%~dp0build\libs"
goto :end

:checkjar
echo %~nx1| findstr /i /c:"-sources" /c:"-dev" >nul
if errorlevel 1 set "JAR=%~1"
goto :eof

:nojar
echo.
echo ============================================================
echo   BUILD FINISHED, BUT NO JAR WAS FOUND
echo ============================================================
echo Look inside this folder for the .jar file:
echo   %~dp0build\libs
goto :end

:failed
echo.
echo ============================================================
echo   BUILD FAILED
echo ============================================================
echo Scroll up to read the error message. Common causes:
echo   - Java 21 (JDK) is not installed
echo   - no internet connection during the first build
echo   - a firewall or antivirus blocking Gradle downloads
echo.
pause
exit /b 1

:end
pause
exit /b 0
