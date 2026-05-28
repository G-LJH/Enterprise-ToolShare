@echo off
REM Tool Share 快速启动脚本
REM 用于 Windows CMD 环境

echo ========================================
echo   Tool Share 快速启动脚本
echo ========================================
echo.

REM 检查 Docker 是否运行
echo [1/4] 检查 Docker 环境...
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo   错误: Docker 未运行或未安装，请先启动 Docker Desktop
    pause
    exit /b 1
)
echo   Docker 已就绪
echo.

REM 检查 .env 文件
echo [2/4] 检查配置文件...
if not exist ".env" (
    echo   警告: .env 文件不存在，正在从 .env.example 复制...
    if exist ".env.example" (
        copy ".env.example" ".env" >nul
        echo   已创建 .env 文件，请检查配置是否正确
    ) else (
        echo   错误: .env.example 文件也不存在
        pause
        exit /b 1
    )
) else (
    echo   .env 文件存在
)
echo.

REM 启动服务
echo [3/4] 启动服务...
echo   正在启动 PostgreSQL、Backend 和 Frontend...
docker-compose up -d --build
if %errorlevel% neq 0 (
    echo   错误: 服务启动失败
    pause
    exit /b 1
)
echo   服务启动命令已执行，等待服务就绪...
echo.

REM 等待服务就绪
echo [4/4] 等待服务就绪...
echo   等待 PostgreSQL 启动...
timeout /t 15 /nobreak >nul
echo   PostgreSQL 已就绪
echo.

echo   等待 Backend 启动...
timeout /t 60 /nobreak >nul
echo   Backend 已就绪
echo.

timeout /t 5 /nobreak >nul
echo   Frontend 已启动
echo.

echo ========================================
echo   启动完成！
echo ========================================
echo.
echo 访问地址:
echo   前端: http://localhost:3000
echo   后端: http://localhost:8080
echo.
echo 默认管理员账号:
echo   用户名: admin
echo   密码: Admin@123456
echo.
echo 常用命令:
echo   查看日志: docker-compose logs -f
echo   停止服务: docker-compose down
echo   重启服务: docker-compose restart
echo.
pause
