# Auth Service Configuration Guide

This document describes the configuration options available for the Auth Service.

## Environment Variables

### Database Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_HOST` | Database host | localhost | Yes |
| `DB_PORT` | Database port | 3306 | Yes |
| `DB_NAME` | Database name | leave_management | Yes |
| `DB_USERNAME` | Database username | root | Yes |
| `DB_PASSWORD` | Database password | password | Yes |
| `DB_DRIVER` | Database driver class | com.mysql.cj.jdbc.Driver | No |

### Spring Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | docker | No |
| `SPRING_DATASOURCE_URL` | Database connection URL | Auto-generated | No |
| `SPRING_DATASOURCE_USERNAME` | Database username | From DB_USERNAME | No |
| `SPRING_DATASOURCE_PASSWORD` | Database password | From DB_PASSWORD | No |

### JPA Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JPA_DDL_AUTO` | Hibernate DDL mode | update | No |
| `JPA_SHOW_SQL` | Show SQL queries | false | No |
| `HIBERNATE_FORMAT_SQL` | Format SQL output | false | No |
| `HIBERNATE_DIALECT` | Hibernate dialect | MySQLDialect | No |

### JWT Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JWT_SECRET` | JWT signing secret | Generated default | Yes (in prod) |
| `JWT_EXPIRATION` | JWT expiration time (ms) | 86400000 (24h) | No |
| `JWT_REFRESH_EXPIRATION` | Refresh token expiration (ms) | 604800000 (7d) | No |

### Email Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SMTP_HOST` | SMTP server host | smtp.gmail.com | Yes |
| `SMTP_PORT` | SMTP server port | 587 | Yes |
| `SMTP_USERNAME` | SMTP username | - | Yes |
| `SMTP_PASSWORD` | SMTP password/app password | - | Yes |
| `MAIL_DEBUG` | Enable mail debugging | false | No |
| `MAIL_ENABLED` | Enable email functionality | true | No |

### Google OAuth Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | - | Yes (for OAuth) |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | - | Yes (for OAuth) |

### Server Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Server port | 8081 | No |

### CORS Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `CORS_ALLOWED_ORIGINS` | Allowed origins (comma-separated) | localhost:3000,5713 | No |
| `CORS_ALLOWED_METHODS` | Allowed HTTP methods | GET,POST,PUT,DELETE,OPTIONS | No |
| `CORS_ALLOWED_HEADERS` | Allowed headers | * | No |
| `CORS_ALLOW_CREDENTIALS` | Allow credentials | true | No |
| `CORS_MAX_AGE` | CORS preflight cache time | 3600 | No |

### Eureka Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka server URL | http://eureka-server:8761/eureka/ | No |
| `EUREKA_INSTANCE_HOSTNAME` | Service hostname | auth-service | No |
| `EUREKA_INSTANCE_PREFER_IP_ADDRESS` | Use IP instead of hostname | true | No |

### Logging Configuration
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `LOG_LEVEL_SECURITY` | Security logging level | INFO | No |
| `LOG_LEVEL_OAUTH2` | OAuth2 logging level | INFO | No |
| `LOG_LEVEL_APP` | Application logging level | DEBUG | No |
| `LOG_LEVEL_ROOT` | Root logging level | INFO | No |

## Spring Profiles

### Development Profile (`dev`)
- Enables SQL logging
- Uses `create-drop` DDL mode
- Enables mail debugging
- Detailed health check information

### Production Profile (`prod`)
- Disables SQL logging
- Uses `validate` DDL mode
- Disables mail debugging
- Restricted health check information
- Optimized for performance

### Docker Profile (`docker`)
- Balanced logging levels
- Uses `update` DDL mode
- Configured for containerized environment
- Eureka hostname configuration

## Configuration Examples

### Local Development
```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost
export DB_PASSWORD=your_password
export JWT_SECRET=your_secret_key
export GOOGLE_CLIENT_ID=your_google_client_id
export GOOGLE_CLIENT_SECRET=your_google_client_secret
```

### Docker Environment
```bash
export SPRING_PROFILES_ACTIVE=docker
export DB_HOST=mysql
export DB_PASSWORD=password
export JWT_SECRET=your_production_secret
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
```

### Production Environment
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=your_production_db_host
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your_very_secure_secret
export CORS_ALLOWED_ORIGINS=https://yourdomain.com
export PRODUCTION_ALLOWED_ORIGINS=https://yourdomain.com
```

## Security Considerations

1. **JWT Secret**: Always use a strong, unique secret in production
2. **Database Password**: Use strong passwords and consider using a secrets manager
3. **OAuth Credentials**: Keep Google OAuth credentials secure
4. **CORS**: Restrict allowed origins in production
5. **Logging**: Avoid logging sensitive information in production

## Health Checks

The service exposes health check endpoints:
- `/actuator/health` - Basic health check
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

## Monitoring

The service exposes metrics at `/actuator/metrics` and Prometheus format at `/actuator/prometheus`. 