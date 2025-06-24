# Leave Management System

A comprehensive microservices-based leave management system built with Spring Boot, React, and Docker.

## 🏗️ Architecture

- **Eureka Server**: Service discovery and registry (Port 8761)
- **Auth Service**: Authentication, authorization, and user management (Port 8081)
- **Leave Service**: Leave management, approvals, and reporting (Port 8082)
- **Frontend**: React + Vite + Material UI application (Port 3000)
- **PostgreSQL**: Database for all services

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17 (for local development)
- Node.js 18+ (for frontend development)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd leave-management
   ```

2. **Configure environment**
   ```bash
   # Copy and edit the environment file
   cp env.txt .env
   # Edit .env with your configuration
   ```

3. **Start the application**
   ```bash
   # Using Docker Compose
   docker-compose up -d
   
   # Or use the deployment script
   ./deploy.sh  # Linux/Mac
   deploy.bat   # Windows
   ```

4. **Access the application**
   - Frontend: http://localhost:3000
   - Auth Service: http://localhost:8081
   - Leave Service: http://localhost:8082
   - Eureka Server: http://localhost:8761

### Default Credentials

- **Admin User**: admin@africahr.com / admin123
- **Sample Users**: 
  - john.doe@africahr.com / admin123
  - jane.smith@africahr.com / admin123

## 🧪 Testing

### API Testing

1. **Auth Service Endpoints**
   ```bash
   # Login
   curl -X POST http://localhost:8081/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@africahr.com","password":"admin123"}'
   
   # Get user profile
   curl -X GET http://localhost:8081/api/auth/profile \
     -H "Authorization: Bearer <token>"
   ```

2. **Leave Service Endpoints**
   ```bash
   # Get leave types
   curl -X GET http://localhost:8082/api/leave-types \
     -H "Authorization: Bearer <token>"
   
   # Apply for leave
   curl -X POST http://localhost:8082/api/leaves \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"leaveTypeId":1,"startDate":"2024-01-15","endDate":"2024-01-17","reason":"Vacation"}'
   ```

### Frontend Testing

1. Open http://localhost:3000
2. Login with admin credentials
3. Test different user roles (Staff, Manager, Admin)
4. Verify leave application and approval workflows

## 🐳 Docker Deployment

### Production Deployment

1. **Build images**
   ```bash
   docker-compose build --no-cache
   ```

2. **Deploy to production**
   ```bash
   # Update env.txt with production values
   docker-compose -f docker-compose.yml up -d
   ```

### Docker Hub Publishing

```bash
# Tag images for Docker Hub
docker tag leave-management-frontend:latest your-username/leave-management-frontend:latest
docker tag leave-management-auth:latest your-username/leave-management-auth:latest
docker tag leave-management-leave:latest your-username/leave-management-leave:latest
docker tag leave-management-eureka:latest your-username/leave-management-eureka:latest

# Push to Docker Hub
docker push your-username/leave-management-frontend:latest
docker push your-username/leave-management-auth:latest
docker push your-username/leave-management-leave:latest
docker push your-username/leave-management-eureka:latest
```

## 🌐 Cloud Deployment

### DigitalOcean

1. **Install DigitalOcean CLI**
   ```bash
   # macOS
   brew install doctl
   
   # Linux
   snap install doctl
   ```

2. **Deploy using the provided script**
   ```bash
   ./deploy-digitalocean.sh
   ```

### Railway

1. **Connect your repository to Railway**
2. **Set environment variables in Railway dashboard**
3. **Deploy automatically on push**

## 📊 Monitoring and Health Checks

### Service Health Endpoints

- Eureka Server: http://localhost:8761/actuator/health
- Auth Service: http://localhost:8081/actuator/health
- Leave Service: http://localhost:8082/actuator/health
- Frontend: http://localhost:3000/health

### Logs

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f auth-service
docker-compose logs -f leave-service
docker-compose logs -f frontend
```

## 🔧 Configuration

### Environment Variables

Key configuration variables in `env.txt`:

- `POSTGRES_DB`: Database name
- `JWT_SECRET`: JWT signing secret
- `GOOGLE_CLIENT_ID`: Google OAuth client ID
- `SMTP_HOST`: Email server configuration
- `CORS_ALLOWED_ORIGINS`: Allowed frontend origins

### Database Configuration

The system uses PostgreSQL with separate databases:
- `auth_db`: User authentication and management
- `leave_db`: Leave management and reporting

## 🛠️ Development

### Local Development Setup

1. **Backend Services**
   ```bash
   # Build all services
   mvn clean install
   
   # Run individual services
   cd auth-service && mvn spring-boot:run
   cd leave-service && mvn spring-boot:run
   cd eureka-server && mvn spring-boot:run
   ```

2. **Frontend Development**
   ```bash
   cd leavefrontend
   npm install
   npm run dev
   ```

### Code Structure

```
├── auth-service/          # Authentication microservice
├── leave-service/         # Leave management microservice
├── eureka-server/         # Service discovery
├── auth-service-api/      # Shared API models
├── leavefrontend/         # React frontend
├── docker-compose.yml     # Docker orchestration
├── env.txt               # Environment configuration
└── init.sql              # Database initialization
```

## 🔒 Security

### Authentication

- JWT-based authentication
- Google OAuth 2.0 integration
- Role-based access control (STAFF, MANAGER, ADMIN)

### Authorization

- Spring Security with method-level security
- PreAuthorize annotations for endpoint protection
- Service-to-service authentication

## 📈 Performance

### Optimizations

- Connection pooling with HikariCP
- JPA/Hibernate optimizations
- Frontend code splitting and lazy loading
- Nginx caching and compression

### Scaling

- Horizontal scaling with Eureka service discovery
- Stateless services for easy scaling
- Database connection pooling

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Issues**
   ```bash
   # Check PostgreSQL logs
   docker-compose logs postgres
   
   # Verify database is running
   docker-compose exec postgres pg_isready -U postgres
   ```

2. **Service Discovery Issues**
   ```bash
   # Check Eureka server
   curl http://localhost:8761/eureka/apps
   
   # Verify service registration
   docker-compose logs eureka-server
   ```

3. **Frontend Build Issues**
   ```bash
   # Clear node modules and rebuild
   cd leavefrontend
   rm -rf node_modules package-lock.json
   npm install
   npm run build
   ```

### Health Checks

```bash
# Check all services
docker-compose ps

# Check specific service health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## 📝 API Documentation

### Auth Service API

- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/auth/profile` - Get user profile
- `GET /api/auth/users` - Get all users (Admin only)

### Leave Service API

- `GET /api/leave-types` - Get leave types
- `POST /api/leaves` - Apply for leave
- `GET /api/leaves` - Get user leaves
- `POST /api/leaves/{id}/approve` - Approve leave
- `POST /api/leaves/{id}/reject` - Reject leave

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the troubleshooting section

---

**Built with ❤️ using Spring Boot, React, and Docker** 