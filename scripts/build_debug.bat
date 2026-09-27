@echo off
setlocal

rem Resolve the repository from this script, including when called elsewhere.
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
set "SOCKET_DIRECTORY=%PROJECT_ROOT%\.gradle\java-sockets"
if not exist "%SOCKET_DIRECTORY%" mkdir "%SOCKET_DIRECTORY%"
if errorlevel 1 exit /b 1

rem Keep Java sockets outside redirected Windows application temp directories.
set JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS% "-Djdk.net.unixdomain.tmpdir=%SOCKET_DIRECTORY%"
pushd "%PROJECT_ROOT%"
if errorlevel 1 exit /b 1
call ".\gradlew.bat" :app:assembleDebug "-Precapflow.ffmpeg.enabled=true" --no-daemon --max-workers=2 %*
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %BUILD_EXIT_CODE%
