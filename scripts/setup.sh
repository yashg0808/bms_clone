#!/bin/bash
set -e

echo "=========================================="
echo "  BookMyShow Clone - Setup Script"
echo "=========================================="

# Check prerequisites
echo ""
echo "Checking prerequisites..."

command -v java >/dev/null 2>&1 || { echo "❌ Java 17+ is required. Install: https://adoptium.net/"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven 3.9+ is required. Install: https://maven.apache.org/"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "❌ Node.js 18+ is required. Install: https://nodejs.org/"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required. Install: https://docs.docker.com/get-docker/"; exit 1; }

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17+ required, found Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Java $(java -version 2>&1 | head -1 | cut -d'"' -f2)"
echo "✅ Maven $(mvn -v 2>&1 | head -1 | cut -d' ' -f3)"
echo "✅ Node.js $(node -v)"
echo "✅ Docker $(docker --version | cut -d' ' -f3)"

# Copy env file
echo ""
echo "Setting up environment..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "✅ Created .env from .env.example"
    echo "⚠️  Please update .env with your actual values"
else
    echo "✅ .env already exists"
fi

# Start infrastructure
echo ""
echo "Starting infrastructure services..."
docker compose up -d postgres redis kafka zookeeper elasticsearch
echo "⏳ Waiting for services to be ready..."
sleep 15

# Verify infrastructure
echo ""
echo "Verifying infrastructure..."
docker compose ps

# Build shared module
echo ""
echo "Building shared module..."
cd backend/shared
mvn clean install -q -DskipTests
echo "✅ Shared module built"

# Build all services
echo ""
echo "Building backend services..."
for service in movie-service booking-service notification-service api-gateway; do
    echo "  Building $service..."
    cd ../$service
    mvn clean package -q -DskipTests
    echo "  ✅ $service built"
done
cd ../..

# Install frontend dependencies
echo ""
echo "Installing frontend dependencies..."
cd frontend
npm ci --silent
echo "✅ Frontend dependencies installed"
cd ..

echo ""
echo "=========================================="
echo "  ✅ Setup Complete!"
echo "=========================================="
echo ""
echo "To start all services with Docker:"
echo "  docker compose up -d"
echo ""
echo "To start services individually:"
echo "  cd backend/api-gateway && mvn spring-boot:run"
echo "  cd frontend && npm run dev"
echo ""
echo "Frontend: http://localhost:3000"
echo "API Gateway: http://localhost:8080"
echo ""
