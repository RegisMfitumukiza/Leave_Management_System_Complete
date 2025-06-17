# Deployment Guide

This guide provides step-by-step instructions for deploying the Leave Management System to various hosting platforms.

## 🚀 Quick Start with Docker

### Prerequisites
- Docker and Docker Compose installed
- Git installed

### Local Deployment
1. **Clone the repository**
   ```bash
   git clone <your-repository-url>
   cd Leave-Management
   ```

2. **Set up environment variables**
   ```bash
   cp env.example .env
   # Edit .env file with your configuration
   ```

3. **Deploy using the provided script**
   ```bash
   # On Windows
   deploy.bat
   
   # On Linux/Mac
   chmod +x deploy.sh
   ./deploy.sh
   ```

4. **Access the application**
   - Frontend: http://localhost:3000
   - Auth Service: http://localhost:8081
   - Leave Service: http://localhost:8082
   - Eureka Server: http://localhost:8761

## 🌐 Production Deployment Options

### Option 1: DigitalOcean Droplet

#### Step 1: Create a Droplet
1. Sign up for DigitalOcean
2. Create a new Droplet
3. Choose Ubuntu 22.04 LTS
4. Select a plan (recommended: 2GB RAM, 1 CPU)
5. Choose a datacenter region close to your users
6. Add your SSH key or create a password
7. Create the Droplet

#### Step 2: Connect to Your Droplet
```bash
ssh root@your-droplet-ip
```

#### Step 3: Install Docker and Docker Compose
```bash
# Update system
apt update && apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Add user to docker group
usermod -aG docker $USER
```

#### Step 4: Deploy the Application
```bash
# Clone the repository
git clone <your-repository-url>
cd Leave-Management

# Set up environment variables
cp env.example .env
nano .env  # Edit with your production values

# Deploy
docker-compose up -d

# Check status
docker-compose ps
```

#### Step 5: Configure Domain and SSL (Optional)
1. Point your domain to the Droplet's IP
2. Install Nginx and Certbot
3. Configure SSL certificate

### Option 2: AWS EC2

#### Step 1: Launch EC2 Instance
1. Sign up for AWS
2. Launch a new EC2 instance
3. Choose Amazon Linux 2 or Ubuntu
4. Select t3.medium or larger
5. Configure security groups to allow ports 80, 443, 22
6. Launch the instance

#### Step 2: Connect and Deploy
```bash
# Connect to your instance
ssh -i your-key.pem ec2-user@your-instance-ip

# Install Docker
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Deploy application
git clone <your-repository-url>
cd Leave-Management
cp env.example .env
# Edit .env file
docker-compose up -d
```

### Option 3: Railway

#### Step 1: Connect Repository
1. Sign up for Railway
2. Connect your GitHub repository
3. Railway will automatically detect the docker-compose.yml

#### Step 2: Configure Environment Variables
1. Go to your project settings
2. Add all environment variables from your .env file
3. Railway will automatically deploy

#### Step 3: Access Your Application
Railway will provide you with a public URL for your application.

### Option 4: Render

#### Step 1: Create a New Web Service
1. Sign up for Render
2. Connect your GitHub repository
3. Choose "Web Service"

#### Step 2: Configure the Service
- **Build Command**: `docker-compose build`
- **Start Command**: `docker-compose up`
- **Environment**: Docker

#### Step 3: Set Environment Variables
Add all environment variables from your .env file in the Render dashboard.

### Option 5: Heroku

#### Step 1: Install Heroku CLI
```bash
# Install Heroku CLI
curl https://cli-assets.heroku.com/install.sh | sh

# Login to Heroku
heroku login
```

#### Step 2: Create Heroku App
```bash
# Create app
heroku create your-app-name

# Add Heroku container registry
heroku container:login
```

#### Step 3: Deploy
```bash
# Build and push images
docker-compose build
docker tag leave-management-frontend registry.heroku.com/your-app-name/web
docker push registry.heroku.com/your-app-name/web

# Set environment variables
heroku config:set DB_HOST=your-db-host
heroku config:set DB_PASSWORD=your-db-password
# ... set all other environment variables

# Release the app
heroku container:release web
```

## 🔧 Environment Configuration

### Required Environment Variables
```env
# Database
DB_HOST=your-database-host
DB_PORT=3306
DB_NAME=leave_management
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# JWT
JWT_SECRET=your-super-secret-jwt-key
JWT_EXPIRATION=86400000

# Email (Gmail recommended)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### Production Security Checklist
- [ ] Change default JWT secret
- [ ] Use strong database passwords
- [ ] Enable HTTPS/SSL
- [ ] Configure firewall rules
- [ ] Set up monitoring and logging
- [ ] Regular backups
- [ ] Update dependencies regularly

## 📊 Monitoring and Maintenance

### Health Checks
```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Check individual service health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8761/actuator/health
```

### Backup Strategy
```bash
# Database backup
docker-compose exec mysql mysqldump -u root -p leave_management > backup.sql

# Restore database
docker-compose exec -T mysql mysql -u root -p leave_management < backup.sql
```

### Updates and Maintenance
```bash
# Pull latest changes
git pull origin main

# Rebuild and restart services
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using the port
netstat -tulpn | grep :8081

# Kill the process or change ports in docker-compose.yml
```

#### 2. Database Connection Issues
```bash
# Check database container
docker-compose logs mysql

# Test database connection
docker-compose exec mysql mysql -u root -p
```

#### 3. Memory Issues
```bash
# Check memory usage
docker stats

# Increase memory limits in docker-compose.yml
```

#### 4. SSL/HTTPS Issues
- Ensure certificates are valid
- Check nginx configuration
- Verify domain DNS settings

## 📞 Support

For deployment issues:
1. Check the logs: `docker-compose logs -f`
2. Verify environment variables
3. Ensure all prerequisites are met
4. Check firewall and security group settings
5. Contact support with specific error messages

## 🔄 CI/CD Pipeline (Optional)

### GitHub Actions Example
```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    
    - name: Deploy to server
      uses: appleboy/ssh-action@v0.1.4
      with:
        host: ${{ secrets.HOST }}
        username: ${{ secrets.USERNAME }}
        key: ${{ secrets.KEY }}
        script: |
          cd /path/to/your/app
          git pull origin main
          docker-compose down
          docker-compose build --no-cache
          docker-compose up -d
```

This deployment guide covers the most common hosting scenarios. Choose the option that best fits your needs and budget. 