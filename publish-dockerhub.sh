#!/bin/bash

# Docker Hub Publishing Script for Leave Management System
# Usage: ./publish-dockerhub.sh [version] [username]

set -e

# Configuration
DOCKER_USERNAME=${2:-"your-dockerhub-username"}
VERSION=${1:-"latest"}
REGISTRY="docker.io"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🐳 Publishing Leave Management System to Docker Hub${NC}"
echo "Username: $DOCKER_USERNAME"
echo "Version: $VERSION"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# Check if user is logged in to Docker Hub
if ! docker info | grep -q "Username"; then
    echo -e "${YELLOW}⚠️  Not logged in to Docker Hub. Please run: docker login${NC}"
    exit 1
fi

# Build all images first
echo -e "${GREEN}🔨 Building Docker images...${NC}"
docker-compose build --no-cache

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed. Please check the error messages above.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Images built successfully${NC}"

# Function to tag and push image
tag_and_push() {
    local service_name=$1
    local image_name=$2
    
    echo -e "${YELLOW}📦 Tagging $service_name...${NC}"
    docker tag leave-management-$image_name:latest $DOCKER_USERNAME/leave-management-$image_name:$VERSION
    docker tag leave-management-$image_name:latest $DOCKER_USERNAME/leave-management-$image_name:latest
    
    echo -e "${YELLOW}🚀 Pushing $service_name...${NC}"
    docker push $DOCKER_USERNAME/leave-management-$image_name:$VERSION
    docker push $DOCKER_USERNAME/leave-management-$image_name:latest
    
    echo -e "${GREEN}✅ $service_name published successfully${NC}"
}

# Tag and push all services
echo ""
echo -e "${GREEN}📤 Publishing images to Docker Hub...${NC}"

tag_and_push "Frontend" "frontend"
tag_and_push "Auth Service" "auth"
tag_and_push "Leave Service" "leave"
tag_and_push "Eureka Server" "eureka"

echo ""
echo -e "${GREEN}🎉 All images published successfully!${NC}"
echo ""
echo -e "${YELLOW}📋 Published Images:${NC}"
echo "  - $DOCKER_USERNAME/leave-management-frontend:$VERSION"
echo "  - $DOCKER_USERNAME/leave-management-auth:$VERSION"
echo "  - $DOCKER_USERNAME/leave-management-leave:$VERSION"
echo "  - $DOCKER_USERNAME/leave-management-eureka:$VERSION"
echo ""
echo -e "${YELLOW}🔗 Docker Hub URLs:${NC}"
echo "  - https://hub.docker.com/r/$DOCKER_USERNAME/leave-management-frontend"
echo "  - https://hub.docker.com/r/$DOCKER_USERNAME/leave-management-auth"
echo "  - https://hub.docker.com/r/$DOCKER_USERNAME/leave-management-leave"
echo "  - https://hub.docker.com/r/$DOCKER_USERNAME/leave-management-eureka"
echo ""
echo -e "${YELLOW}📝 Usage Example:${NC}"
echo "docker run -p 3000:80 $DOCKER_USERNAME/leave-management-frontend:$VERSION"
echo "docker run -p 8081:8081 $DOCKER_USERNAME/leave-management-auth:$VERSION"
echo "docker run -p 8082:8082 $DOCKER_USERNAME/leave-management-leave:$VERSION"
echo "docker run -p 8761:8761 $DOCKER_USERNAME/leave-management-eureka:$VERSION" 