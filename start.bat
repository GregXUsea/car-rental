@echo off
chcp 65001 >nul
title 租车系统 - 启动

echo ========================================
echo   租车系统前后端一键启动
echo ========================================

echo.
echo [1/3] 清理 8080 端口...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    echo   发现进程 %%a 占用8080，正在终止...
    taskkill -PID %%a -F >nul 2>&1
)

echo.
echo [2/3] 启动后端 SpringBoot...
start "后端-Backend" cmd /k "cd /d D:\car-rental-system\backend && .\mvnw spring-boot:run"

echo [3/3] 启动前端 Vite...
start "前端-Frontend" cmd /k "cd /d D:\car-rental-system\frontend && npm run dev"

echo.
echo ========================================
echo   启动完成！等待两个新窗口加载完毕：
echo   后端 - http://localhost:8080
echo   前端 - http://localhost:5173
echo ========================================
echo.
echo 按任意键关闭本窗口（不会影响前后端运行）...
pause >nul
