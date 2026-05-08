@echo off
echo Starting user export...

REM Set the working directory to the backend folder
cd /d %~dp0backend

REM Run the Spring Boot application with export command
REM This will start the app, export users, and exit
java -cp target/classes -Dloader.main=com.ipl.util.UserCsvExporter org.springframework.boot.loader.JarLauncher

echo Export completed.