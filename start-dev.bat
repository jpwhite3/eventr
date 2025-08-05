@echo off
echo Starting development environment with Testcontainers...

REM Start MailHog only (PostgreSQL and LocalStack will be managed by Testcontainers)
echo Starting MailHog for email testing...
docker run -d --name eventr-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog

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
