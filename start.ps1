# Tool Share 快速启动脚本
# 用于 Windows PowerShell 环境

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Tool Share 快速启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker 是否运行
Write-Host "[1/4] 检查 Docker 环境..." -ForegroundColor Yellow
try {
    $dockerVersion = docker version --format '{{.Server.Version}}' 2>$null
    Write-Host "  Docker 版本: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "  错误: Docker 未运行或未安装，请先启动 Docker Desktop" -ForegroundColor Red
    exit 1
}

# 检查 .env 文件
Write-Host "[2/4] 检查配置文件..." -ForegroundColor Yellow
if (Test-Path ".env") {
    Write-Host "  .env 文件存在" -ForegroundColor Green
} else {
    Write-Host "  警告: .env 文件不存在，正在从 .env.example 复制..." -ForegroundColor Yellow
    if (Test-Path ".env.example") {
        Copy-Item ".env.example" ".env"
        Write-Host "  已创建 .env 文件，请检查配置是否正确" -ForegroundColor Yellow
    } else {
        Write-Host "  错误: .env.example 文件也不存在" -ForegroundColor Red
        exit 1
    }
}

# 启动服务
Write-Host "[3/4] 启动服务..." -ForegroundColor Yellow
Write-Host "  正在启动 PostgreSQL、Backend 和 Frontend..." -ForegroundColor Gray
docker-compose up -d --build

if ($LASTEXITCODE -ne 0) {
    Write-Host "  错误: 服务启动失败" -ForegroundColor Red
    exit 1
}

Write-Host "  服务启动命令已执行，等待服务就绪..." -ForegroundColor Green

# 等待服务就绪
Write-Host "[4/4] 等待服务就绪..." -ForegroundColor Yellow

Write-Host "  等待 PostgreSQL 启动..." -ForegroundColor Gray
$timeout = 60
$elapsed = 0
while ($elapsed -lt $timeout) {
    $healthy = docker inspect --format='{{.State.Health.Status}}' toolshare-postgres 2>$null
    if ($healthy -eq "healthy") {
        Write-Host "  PostgreSQL 已就绪" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 2
    $elapsed += 2
}

Write-Host "  等待 Backend 启动..." -ForegroundColor Gray
$timeout = 120
$elapsed = 0
while ($elapsed -lt $timeout) {
    $healthy = docker inspect --format='{{.State.Health.Status}}' toolshare-backend 2>$null
    if ($healthy -eq "healthy") {
        Write-Host "  Backend 已就绪" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 2
    $elapsed += 2
}

# 检查 Frontend
Start-Sleep -Seconds 5
$frontendRunning = docker ps --filter "name=toolshare-frontend" --format '{{.Status}}' 2>$null
if ($frontendRunning) {
    Write-Host "  Frontend 已启动" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  启动完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "访问地址:" -ForegroundColor Yellow
Write-Host "  前端: http://localhost:3000" -ForegroundColor White
Write-Host "  后端: http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "默认管理员账号:" -ForegroundColor Yellow
Write-Host "  用户名: admin" -ForegroundColor White
Write-Host "  密码: Admin@123456" -ForegroundColor White
Write-Host ""
Write-Host "常用命令:" -ForegroundColor Yellow
Write-Host "  查看日志: docker-compose logs -f" -ForegroundColor Gray
Write-Host "  停止服务: docker-compose down" -ForegroundColor Gray
Write-Host "  重启服务: docker-compose restart" -ForegroundColor Gray
Write-Host ""
