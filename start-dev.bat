@echo off
setlocal enabledelayedexpansion

REM Function to clean up resources
:cleanup
echo.
echo Shutting down services...

REM Stop and remove MailHog container if it exists
FOR /F "tokens=*" %%i IN ('docker ps -q --filter "name=eventr-mailhog"') DO (
    echo Stopping MailHog container...
    docker stop %%i 2>nul
    docker rm %%i 2>nul
)

REM Find and kill any Testcontainers that might be running
echo Cleaning up any Testcontainers...
FOR /F "tokens=*" %%i IN ('docker ps -q --filter "label=org.testcontainers=true"') DO (
    docker stop %%i 2>nul
    docker rm %%i 2>nul
)

echo Cleanup complete.
exit /b 0

REM Register cleanup for Ctrl+C
REM This is a bit tricky in batch files, but we'll use a workaround
REM by creating a temporary VBS script to handle Ctrl+C

echo Creating Ctrl+C handler...
echo Set WshShell = CreateObject("WScript.Shell") > %TEMP%\eventr_cleanup.vbs
echo WshShell.Run "taskkill /F /IM java.exe", 0, True >> %TEMP%\eventr_cleanup.vbs
echo WshShell.Run "cmd /c start /b %~dp0cleanup.bat", 0, False >> %TEMP%\eventr_cleanup.vbs

REM Create a cleanup batch file
echo @echo off > %~dp0cleanup.bat
echo docker stop eventr-mailhog 2^>nul >> %~dp0cleanup.bat
echo docker rm eventr-mailhog 2^>nul >> %~dp0cleanup.bat
echo FOR /F "tokens=*" %%%%i IN ('docker ps -q --filter "label=org.testcontainers=true"') DO docker stop %%%%i 2^>nul >> %~dp0cleanup.bat
echo FOR /F "tokens=*" %%%%i IN ('docker ps -q --filter "label=org.testcontainers=true"') DO docker rm %%%%i 2^>nul >> %~dp0cleanup.bat

echo Starting development environment with Testcontainers...

REM Check if MailHog is already running
FOR /F "tokens=*" %%i IN ('docker ps -q --filter "name=eventr-mailhog"') DO (
    set MAILHOG_RUNNING=true
)

if defined MAILHOG_RUNNING (
    echo MailHog is already running.
) else (
    echo Starting MailHog for email testing...
    docker run -d --name eventr-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
)

REM Start backend server in the background with dev profile
echo Starting backend server with Testcontainers...
start /B cmd /c "mvnw.cmd spring-boot:run -Pbackend -Dspring.profiles.active=dev"

REM Wait for backend to start
echo Waiting for backend server to start...
timeout /t 20 /nobreak

REM Start frontend server
echo Starting frontend server...
cd frontend
npm start

REM Note: In Windows batch scripts, we can't easily trap the exit signal
REM and kill the background process. Users will need to manually stop
REM the backend process when they're done.
