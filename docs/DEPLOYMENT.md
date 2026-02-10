# BookMyShow Clone - Deployment Guide

## Deployment Options

### Option 1: Docker Compose (Development / Staging)

```bash
# Build all images
docker compose build

# Start all services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f booking-service
```

For production-like settings:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Option 2: Kubernetes with Helm (Production)

#### Prerequisites

- Kubernetes cluster (EKS, GKE, AKS, or local with minikube)
- Helm 3.x installed
- kubectl configured
- Docker images pushed to registry

#### Step 1: Build & Push Docker Images

```bash
# Build all backend services
for service in movie-service booking-service notification-service api-gateway; do
  cd backend/$service
  mvn clean package -DskipTests
  docker build -t bookmyshow/$service:latest .
  docker push bookmyshow/$service:latest
  cd ../..
done

# Build frontend
cd frontend
docker build -t bookmyshow/frontend:latest .
docker push bookmyshow/frontend:latest
cd ..
```

#### Step 2: Deploy with Helm

```bash
# Add Bitnami repo for dependencies
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Update dependencies
cd helm/bookmyshow
helm dependency update

# Deploy to staging
helm install bookmyshow . \
  -f values-dev.yaml \
  --namespace bookmyshow \
  --create-namespace \
  --set database.password=$DB_PASSWORD

# Deploy to production
helm install bookmyshow . \
  --namespace bookmyshow \
  --create-namespace \
  --set database.password=$DB_PASSWORD
```

#### Step 3: Verify Deployment

```bash
kubectl get pods -n bookmyshow
kubectl get svc -n bookmyshow
kubectl get ingress -n bookmyshow
```

#### Step 4: Setup Ingress

```bash
# Install nginx ingress controller
helm install nginx-ingress ingress-nginx/ingress-nginx --namespace ingress-nginx --create-namespace

# Install cert-manager for TLS
helm install cert-manager jetstack/cert-manager --namespace cert-manager --create-namespace --set installCRDs=true
```

### Option 3: Raw Kubernetes Manifests

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/ingress.yaml
```

## Scaling

### Horizontal Pod Autoscaler

All services have HPA configured. The booking service scales more aggressively:

- **Booking Service:** 3–15 replicas, 60% CPU target
- **Other Services:** 2–8 replicas, 70% CPU target

### Manual Scaling

```bash
kubectl scale deployment booking-service --replicas=5 -n bookmyshow
```

## Monitoring

### Prometheus & Grafana

```bash
# Access Grafana
kubectl port-forward svc/grafana 3001:3000 -n monitoring

# Default credentials: admin / admin
# Dashboards auto-provisioned from monitoring/grafana/dashboards/
```

### Key Dashboards

- **Service Overview:** Health, request rates, response times, error rates
- **Booking:** Seat locks, booking flow, confirmation rates

### Alerts

Configured in `monitoring/prometheus/alerts/`:

- Service down
- High response time (p95 > 2s)
- High error rate (> 5%)
- DB connection pool exhaustion
- Redis memory usage

## Troubleshooting

### Common Issues

**Services can't connect to DB:**

```bash
kubectl logs deployment/booking-service -n bookmyshow | grep -i "connection"
kubectl get svc postgres-service -n bookmyshow
```

**Kafka consumer lag:**

```bash
kubectl exec -it kafka-0 -n bookmyshow -- kafka-consumer-groups --bootstrap-server localhost:9092 --describe --all-groups
```

**Seat lock issues:**

```bash
kubectl exec -it redis-master-0 -n bookmyshow -- redis-cli KEYS "seat-lock:*"
```

### Rolling Restart

```bash
kubectl rollout restart deployment/booking-service -n bookmyshow
```

### Rollback

```bash
helm rollback bookmyshow -n bookmyshow
```

## CI/CD

Three GitHub Actions workflows:

1. **backend-ci.yml** — Tests & builds all backend services on push
2. **frontend-ci.yml** — Lints, type-checks, builds frontend on push
3. **deploy-prod.yml** — Manual trigger deployment to staging/production

### Required GitHub Secrets

- `DOCKER_USERNAME` / `DOCKER_PASSWORD`
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_REGION`
- `DB_PASSWORD`
- `API_URL`
