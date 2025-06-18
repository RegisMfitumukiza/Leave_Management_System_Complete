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

if %errorlevel% neq 0 (
    echo ❌ Build failed. Please check the error messages above.
    pause
    exit /b 1
)

echo ✅ Docker images built successfully

REM Start services
echo 🚀 Starting services...
docker-compose up -d

if %errorlevel% neq 0 (
    echo ❌ Failed to start services. Please check the error messages above.
    pause
    exit /b 1
)

echo ✅ Services started successfully

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
echo 📊 To view service status: docker-compose ps
echo 📋 To view logs: docker-compose logs -f
echo 🛑 To stop services: docker-compose down
echo.
pause 