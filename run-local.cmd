@echo off
cd /d "%~dp0"
echo Starting GroupBookingLA on http://localhost:8080
echo Sign in: Admin@rathin.com / R@t@2023
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
