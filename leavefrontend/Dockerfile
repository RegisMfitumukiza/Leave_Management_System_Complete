# Multi-stage build for React Frontend
FROM node:18-alpine AS build

# Set working directory
WORKDIR /app

# Copy package files
COPY leavefrontend/package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source code
COPY leavefrontend/src ./src
COPY leavefrontend/public ./public
COPY leavefrontend/index.html ./
COPY leavefrontend/vite.config.js ./

# Build the application
RUN npm run build

# Production stage with nginx
FROM nginx:alpine

# Copy custom nginx configuration
COPY leavefrontend/nginx.conf /etc/nginx/nginx.conf

# Copy built application
COPY --from=build /app/dist /usr/share/nginx/html

# Create health check file
RUN echo "healthy" > /usr/share/nginx/html/health

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"] 