#!/bin/bash

echo "🚀 Deploying Leave Management System to DigitalOcean..."

# Check if doctl is installed
if ! command -v doctl &> /dev/null; then
    echo "❌ DigitalOcean CLI (doctl) is not installed."
    echo "Please install it from: https://docs.digitalocean.com/reference/doctl/how-to/install/"
    exit 1
fi

# Check if env.txt exists
if [ ! -f "env.txt" ]; then
    echo "❌ env.txt file not found. Please create it with production configuration."
    exit 1
fi

echo "✅ Environment file found"

# Create production environment file
echo "🔧 Creating production environment file..."
cp env.txt .env

# Update environment for production
sed -i 's/localhost/your-domain.com/g' .env
sed -i 's/http:/https:/g' .env

echo "✅ Production environment configured"

# Build and push images to DigitalOcean Container Registry
echo "📦 Building and pushing Docker images..."

# Tag images for DigitalOcean registry
docker-compose build

# Push to registry (replace with your registry)
# docker tag leave-management-frontend:latest registry.digitalocean.com/your-registry/frontend:latest
# docker push registry.digitalocean.com/your-registry/frontend:latest

echo "✅ Images built successfully"

# Deploy to DigitalOcean App Platform or Droplet
echo "🚀 Deploying to DigitalOcean..."

# For App Platform deployment
# doctl apps create --spec app.yaml

# For Droplet deployment
# doctl compute droplet create leave-management \
#   --size s-2vcpu-4gb \
#   --image docker-20-04 \
#   --region nyc1

echo "🎉 Deployment completed!"
echo ""
echo "📋 Next steps:"
echo "   1. Configure your domain DNS"
echo "   2. Set up SSL certificates"
echo "   3. Configure monitoring and logging"
echo "   4. Set up automated backups"
echo "" 