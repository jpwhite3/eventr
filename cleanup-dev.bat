@echo off
echo Cleaning up Eventr development environment...

REM Kill any running frontend processes
echo Stopping frontend processes...
taskkill /F /IM node.exe /FI "COMMANDLINE eq *react-scripts*" 2>nul
taskkill /F /IM node.exe /FI "COMMANDLINE eq *PORT=3002*" 2>nul
FOR /F "tokens=2" %%i IN ('tasklist /FI "IMAGENAME eq node.exe" /FO CSV ^| findstr "react-scripts"') DO taskkill /F /PID %%i 2>nul

REM Kill any running backend processes
echo Stopping backend processes...
taskkill /F /IM java.exe /FI "COMMANDLINE eq *spring-boot:run*" 2>nul
taskkill /F /IM java.exe /FI "COMMANDLINE eq *EventrApplication*" 2>nul
taskkill /F /IM javaw.exe 2>nul

REM Stop and remove MailHog container
echo Stopping MailHog container...
FOR /F "tokens=*" %%i IN ('docker ps -q --filter "name=eventr-mailhog" 2^>nul') DO (
    docker stop %%i 2>nul
    docker rm %%i 2>nul
)

REM Clean up Testcontainers
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

echo Cleanup complete!
pause