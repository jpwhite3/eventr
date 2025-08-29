@echo off
setlocal enabledelayedexpansion

REM Global variables for process tracking
set "BACKEND_PID="
set "FRONTEND_PID="

REM Enhanced cleanup function
:cleanup
echo.
echo Shutting down services...

REM Kill backend Java processes (more comprehensive)
echo Stopping backend processes...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq *spring-boot:run*" 2>nul
taskkill /F /IM java.exe /FI "COMMANDLINE eq *EventrApplication*" 2>nul
taskkill /F /IM javaw.exe 2>nul

REM Kill frontend Node.js processes 
echo Stopping frontend processes...
taskkill /F /IM node.exe /FI "COMMANDLINE eq *react-scripts*" 2>nul
taskkill /F /IM node.exe /FI "COMMANDLINE eq *PORT=3002*" 2>nul
FOR /F "tokens=2" %%i IN ('tasklist /FI "IMAGENAME eq node.exe" /FO CSV ^| findstr "react-scripts"') DO taskkill /F /PID %%i 2>nul

REM Stop and remove MailHog container if it exists
FOR /F "tokens=*" %%i IN ('docker ps -q --filter "name=eventr-mailhog" 2^>nul') DO (
    echo Stopping MailHog container...
    docker stop %%i 2>nul
    docker rm %%i 2>nul
)

REM Find and kill any Testcontainers that might be running
echo Cleaning up Testcontainers...
FOR /F "tokens=*" %%i IN ('docker ps -q --filter "label=org.testcontainers=true" 2^>nul') DO (
    docker stop %%i 2>nul
    docker rm %%i 2>nul
)

REM Kill any test processes
echo Stopping test processes...
taskkill /F /IM java.exe /FI "COMMANDLINE eq *test*" 2>nul

REM Clean up temporary files
if exist "%~dp0cleanup.bat" del "%~dp0cleanup.bat" 2>nul
if exist "%TEMP%\eventr_cleanup.vbs" del "%TEMP%\eventr_cleanup.vbs" 2>nul

echo Cleanup complete.
exit /b 0

REM Create enhanced cleanup script for Ctrl+C handling
echo Creating enhanced Ctrl+C handler...
echo Set WshShell = CreateObject("WScript.Shell") > "%TEMP%\eventr_cleanup.vbs"
echo WshShell.Run "taskkill /F /IM java.exe /FI ""COMMANDLINE eq *EventrApplication*""", 0, True >> "%TEMP%\eventr_cleanup.vbs"
echo WshShell.Run "taskkill /F /IM node.exe /FI ""COMMANDLINE eq *react-scripts*""", 0, True >> "%TEMP%\eventr_cleanup.vbs"
echo WshShell.Run "cmd /c call ""%~dp0start-dev.bat"" :cleanup", 0, False >> "%TEMP%\eventr_cleanup.vbs"

echo Starting development environment with Testcontainers...

REM Check if MailHog is already running
set "MAILHOG_RUNNING="
FOR /F "tokens=*" %%i IN ('docker ps -q --filter "name=eventr-mailhog" 2^>nul') DO (
    set "MAILHOG_RUNNING=true"
)

if defined MAILHOG_RUNNING (
    echo MailHog is already running.
) else (
    echo Starting MailHog for email testing...
    docker run -d --name eventr-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
    if errorlevel 1 (
        echo Warning: Could not start MailHog container
    )
)

REM Start backend server with error checking
echo Starting backend server with dev profile (skipping tests)...
start /B cmd /c "mvnw.cmd spring-boot:run -Pbackend -Dspring.profiles.active=dev -DskipTests"

REM Wait for backend to start with progress indication
echo Waiting for backend server to start...
for /L %%i in (1,1,15) do (
    timeout /t 1 /nobreak >nul
    echo Backend startup progress: %%i/15 seconds...
)

REM Verify backend is accessible
echo Checking backend health...
curl -s -o nul -w "Backend HTTP status: %%{http_code}" http://localhost:8080/api/events 2>nul
if errorlevel 1 (
    echo Warning: Backend may not be fully started yet
) else (
    echo Backend appears to be running
)

REM Check if frontend directory exists
if not exist "frontend\" (
    echo Error: Frontend directory not found
    echo Make sure you're running this script from the project root directory
    pause
    goto cleanup
)

REM Start frontend server with hot-reload support
echo Starting frontend server with hot-reload enabled...
cd frontend

REM Set environment variables for optimal development experience
set "PORT=3002"
set "BROWSER=none"
set "FAST_REFRESH=true"
set "CHOKIDAR_USEPOLLING=false"
set "GENERATE_SOURCEMAP=true"
set "REACT_APP_DEV_MODE=true"

echo.
echo ========================================
echo   Development Environment Started
echo ========================================
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:3002
echo   MailHog:  http://localhost:8025
echo.
echo Features enabled:
echo   - Hot reload for React components
echo   - Source maps for debugging  
echo   - Fast refresh for instant updates
echo   - CORS configured for all localhost ports
echo.
echo Press Ctrl+C to stop all services
echo ========================================
echo.

REM Start npm with hot-reload (this will block until Ctrl+C)
npm start

REM If we get here, npm start was interrupted
echo.
echo npm start was interrupted, cleaning up...
goto cleanup
