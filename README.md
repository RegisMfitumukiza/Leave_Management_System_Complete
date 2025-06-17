# Leave Management System

A comprehensive leave management system built with Spring Boot microservices and React frontend, designed to comply with Rwandan Labor Law (2023) requirements.

## 🏗️ Architecture

This system follows a microservice architecture with the following components:

- **Authentication Service** - Handles user authentication and authorization
- **Leave Management Service** - Manages leave applications, approvals, and balances
- **Eureka Server** - Service discovery and registration
- **React Frontend** - Modern UI built with Material-UI

## 🚀 Features

### Core Features
- ✅ Employee Dashboard with leave balance viewing
- ✅ Leave application submission with document upload
- ✅ Manager/Admin approval workflow
- ✅ Leave balance management with monthly accrual
- ✅ Team leave calendar with profile pictures
- ✅ Public holidays calendar
- ✅ Email and in-app notifications
- ✅ Google Authenticator integration
- ✅ Cross-platform compatibility

### Leave Types
- Personal Time Off (PTO) - 20 days per year
- Sick Leave
- Compassionate Leave
- Maternity Leave
- Other leave types as per labor law

### User Roles
- Staff
- Department Managers
- Administrators

## 🛠️ Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.x**
- **Spring Security**
- **Spring Data JPA**
- **Maven**
- **MySQL/PostgreSQL**
- **Eureka Server** (Service Discovery)

### Frontend
- **React 18**
- **Vite**
- **Material-UI (MUI)**
- **JavaScript**

### Infrastructure
- **Docker**
- **Docker Compose**

## 📋 Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- Docker and Docker Compose
- Git

## 🚀 Quick Start

### Option 1: Using Docker (Recommended)

1. **Clone the repository**
   ```bash
   git clone <your-repository-url>
   cd Leave-Management
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env file with your configuration
   ```

3. **Start all services with Docker Compose**
   ```bash
   docker-compose up -d
   ```

4. **Access the application**
   - Frontend: http://localhost:3000
   - Auth Service: http://localhost:8081
   - Leave Service: http://localhost:8082
   - Eureka Server: http://localhost:8761

### Option 2: Local Development

1. **Start the database**
   ```bash
   docker-compose up -d mysql
   ```

2. **Start Eureka Server**
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```

3. **Start Authentication Service**
   ```bash
   cd auth-service
   mvn spring-boot:run
   ```

4. **Start Leave Management Service**
   ```bash
   cd leave-service
   mvn spring-boot:run
   ```

5. **Start Frontend**
   ```bash
   cd leavefrontend
   npm install
   npm run dev
   ```

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=leave_management
DB_USERNAME=root
DB_PASSWORD=password

# JWT Configuration
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Service Ports
AUTH_SERVICE_PORT=8081
LEAVE_SERVICE_PORT=8082
EUREKA_SERVER_PORT=8761
FRONTEND_PORT=3000
```

## 📁 Project Structure

```
Leave Management/
├── auth-service/           # Authentication microservice
├── auth-service-api/       # Auth service API contracts
├── eureka-server/         # Service discovery server
├── leave-service/         # Leave management microservice
├── leavefrontend/         # React frontend application
├── docker-compose.yml     # Docker orchestration
├── .env                   # Environment variables
└── README.md             # This file
```

## 🐳 Docker Deployment

### Build Images
```bash
# Build all services
docker-compose build

# Build individual services
docker-compose build auth-service
docker-compose build leave-service
docker-compose build eureka-server
```

### Run with Docker Compose
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## 🌐 Production Deployment

### Option 1: DigitalOcean
1. Create a Droplet
2. Install Docker and Docker Compose
3. Clone the repository
4. Configure environment variables
5. Run `docker-compose up -d`

### Option 2: AWS
1. Launch EC2 instance
2. Install Docker and Docker Compose
3. Configure security groups
4. Deploy using the same Docker commands

### Option 3: Railway/Render
1. Connect your GitHub repository
2. Configure environment variables
3. Deploy automatically

## 🧪 Testing

### Backend Testing
```bash
# Run all tests
mvn test

# Run specific service tests
cd auth-service && mvn test
cd leave-service && mvn test
```

### Frontend Testing
```bash
cd leavefrontend
npm test
```

## 📊 API Documentation

### Authentication Service
- Base URL: `http://localhost:8081`
- Endpoints:
  - `POST /api/auth/login` - User login
  - `POST /api/auth/register` - User registration
  - `GET /api/auth/profile` - Get user profile

### Leave Management Service
- Base URL: `http://localhost:8082`
- Endpoints:
  - `GET /api/leaves` - Get all leaves
  - `POST /api/leaves` - Create leave application
  - `PUT /api/leaves/{id}` - Update leave application
  - `DELETE /api/leaves/{id}` - Delete leave application

## 🔒 Security Features

- JWT-based authentication
- Role-based access control
- Google OAuth integration
- Password encryption
- CORS configuration
- Input validation

## 📈 Monitoring

- Eureka Dashboard: http://localhost:8761
- Application logs available in Docker containers
- Health check endpoints for each service

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team

## 🔄 Updates

- Monthly leave accrual (1.66 days/month)
- Carry-forward rules (max 5 days)
- Automatic expiration by January 31st
- Real-time notifications
- Document upload functionality 