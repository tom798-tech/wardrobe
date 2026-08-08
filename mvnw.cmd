@echo off
setlocal enableextensions

set "MVN_VERSION=3.9.9"
set "SCRIPT_DIR=%~dp0"
set "CACHE_ROOT=%SCRIPT_DIR%.mvn\wrapper\cache"
set "MVN_CACHE_DIR=%CACHE_ROOT%\%MVN_VERSION%"
set "MVN_HOME=%MVN_CACHE_DIR%\apache-maven-%MVN_VERSION%"
set "MVN_ZIP=%MVN_CACHE_DIR%\apache-maven-%MVN_VERSION%-bin.zip"
set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip"
set "PROJECT_DIR=%CD%"
if not exist "%PROJECT_DIR%\pom.xml" if exist "%SCRIPT_DIR%wardrobe-backend\pom.xml" set "PROJECT_DIR=%SCRIPT_DIR%wardrobe-backend"

if exist "%MVN_HOME%\bin\mvn.cmd" goto run

if not exist "%MVN_CACHE_DIR%" mkdir "%MVN_CACHE_DIR%" >nul 2>nul

if exist "%MVN_ZIP%" for %%A in ("%MVN_ZIP%") do if %%~zA EQU 0 del /q "%MVN_ZIP%"

if not exist "%MVN_ZIP%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%MVN_ZIP%'"
    if errorlevel 1 (
        echo Failed to download Maven %MVN_VERSION%.
        exit /b 1
    )
)

if exist "%MVN_HOME%" rmdir /s /q "%MVN_HOME%"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MVN_ZIP%' -DestinationPath '%MVN_CACHE_DIR%' -Force"
if errorlevel 1 (
    echo Failed to extract Maven %MVN_VERSION%.
    exit /b 1
)

:run
pushd "%PROJECT_DIR%"
"%MVN_HOME%\bin\mvn.cmd" %*
set "MVN_EXIT=%ERRORLEVEL%"
popd
exit /b %MVN_EXIT%
