@echo off
echo 🚀 Starting Leave Management System Deployment...

REM Check if Docker is installed
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not installed. Please install Docker first.
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Compose is not installed. Please install Docker Compose first.
    pause
    exit /b 1
)

echo ✅ Docker and Docker Compose are installed

REM Check if .env file exists
if not exist .env (
    echo ⚠️  .env file not found. Creating from template...
    if exist env.example (
        copy env.example .env
        echo ⚠️  Please edit .env file with your configuration before continuing.
        echo Press Enter when you're ready to continue...
        pause
    ) else (
        echo ❌ env.example file not found. Please create .env file manually.
        pause
        exit /b 1
    )
)

echo ✅ Environment file found

REM Build Docker images
echo 🔨 Building Docker images...
docker-compose build --no-cache

REM Start services
echo 🚀 Starting services...
docker-compose up -d

REM Wait for services to be ready
echo ⏳ Waiting for services to be ready...
timeout /t 30 /nobreak >nul

echo 🎉 Deployment completed successfully!
echo.
echo 📋 Service URLs:
echo    Frontend: http://localhost:3000
echo    Auth Service: http://localhost:8081
echo    Leave Service: http://localhost:8082
echo    Eureka Server: http://localhost:8761
echo.
echo 🔑 Default Admin Credentials:
echo    Username: admin
echo    Password: admin123
echo.
pause 