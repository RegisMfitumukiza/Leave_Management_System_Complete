@echo off
echo 🧪 Testing Leave Management System Deployment...

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

echo ✅ Docker is running

REM Check if ports are available
netstat -an | findstr ":3000" >nul
if %errorlevel% equ 0 (
    echo ⚠️  Port 3000 is already in use. Please stop any services using this port.
    pause
    exit /b 1
)

netstat -an | findstr ":8081" >nul
if %errorlevel% equ 0 (
    echo ⚠️  Port 8081 is already in use. Please stop any services using this port.
    pause
    exit /b 1
)

netstat -an | findstr ":8082" >nul
if %errorlevel% equ 0 (
    echo ⚠️  Port 8082 is already in use. Please stop any services using this port.
    pause
    exit /b 1
)

netstat -an | findstr ":8761" >nul
if %errorlevel% equ 0 (
    echo ⚠️  Port 8761 is already in use. Please stop any services using this port.
    pause
    exit /b 1
)

echo ✅ All required ports are available

REM Create .env file if it doesn't exist
if not exist .env (
    echo 📝 Creating .env file from template...
    copy env.example .env
    echo ⚠️  Please edit .env file with your configuration before continuing.
    echo Press Enter when you're ready to continue...
    pause
)

echo ✅ Environment file ready

REM Build and start services
echo 🔨 Building Docker images...
docker-compose build --no-cache

if %errorlevel% neq 0 (
    echo ❌ Build failed. Please check the error messages above.
    pause
    exit /b 1
)

echo ✅ Docker images built successfully

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
timeout /t 45 /nobreak >nul

REM Test service endpoints
echo 🧪 Testing service endpoints...

REM Test Eureka Server
curl -f http://localhost:8761/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Eureka Server is running
) else (
    echo ⚠️  Eureka Server might still be starting up
)

REM Test Auth Service
curl -f http://localhost:8081/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Auth Service is running
) else (
    echo ⚠️  Auth Service might still be starting up
)

REM Test Leave Service
curl -f http://localhost:8082/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Leave Service is running
) else (
    echo ⚠️  Leave Service might still be starting up
)

REM Test Frontend
curl -f http://localhost:3000/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Frontend is running
) else (
    echo ⚠️  Frontend might still be starting up
)

echo.
echo 🎉 Deployment test completed!
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
echo 📊 To view service status: docker-compose ps
echo 📋 To view logs: docker-compose logs -f
echo 🛑 To stop services: docker-compose down
echo.
echo Press any key to open the application in your browser...
pause >nul

REM Open the application in default browser
start http://localhost:3000

echo.
echo 🌐 Application opened in your browser!
echo.
pause 