@echo off
title CarRental - Start

echo ============================================
echo   Car Rental System
echo ============================================
echo.
echo [1/3] Kill port 8080 if occupied...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    echo   Killing PID %%a ...
    taskkill -PID %%a -F >nul 2>&1
)
echo   Done.
echo.
echo [2/3] Starting Backend (SpringBoot)...
start "Backend" cmd /k "cd /d D:\car-rental-system\backend && mvnw.cmd spring-boot:run"

echo [3/3] Starting Frontend (Vite)...
start "Frontend" cmd /k "cd /d D:\car-rental-system\frontend && npm run dev"

echo.
echo ============================================
echo   Backend  : http://localhost:8080
echo   Frontend : http://localhost:5173
echo ============================================
echo.
echo Close this window after both services are up.
pause
