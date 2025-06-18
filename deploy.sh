#!/bin/bash

# Leave Management System Deployment Script
echo "🚀 Starting Leave Management System Deployment..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "✅ Docker and Docker Compose are installed"

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Creating from template..."
    if [ -f env.example ]; then
        cp env.example .env
        echo "⚠️  Please edit .env file with your configuration before continuing."
        echo "Press Enter when you're ready to continue..."
        read
    else
        echo "❌ env.example file not found. Please create .env file manually."
        exit 1
    fi
fi

echo "✅ Environment file found"

# Build Docker images
echo "🔨 Building Docker images..."
docker-compose build --no-cache

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo "✅ Docker images built successfully"

# Start services
echo "🚀 Starting services..."
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "❌ Failed to start services. Please check the error messages above."
    exit 1
fi

echo "✅ Services started successfully"

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."

# Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
while ! docker-compose exec postgres pg_isready -U postgres > /dev/null 2>&1; do
    sleep 2
done

# Wait for services
echo "Waiting for services to be ready..."
sleep 30

echo "🎉 Deployment completed successfully!"
echo ""
echo "📋 Service URLs:"
echo "   Frontend: http://localhost:3000"
echo "   Auth Service: http://localhost:8081"
echo "   Leave Service: http://localhost:8082"
echo "   Eureka Server: http://localhost:8761"
echo ""
echo "📊 To view service status: docker-compose ps"
echo "📋 To view logs: docker-compose logs -f"
echo "🛑 To stop services: docker-compose down"
echo ""
echo "🔑 Default Admin Credentials:"
echo "   Username: admin"
echo "   Password: admin123" 