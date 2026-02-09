#!/bin/bash
set -e

echo "=========================================="
echo "  BookMyShow Clone - Run All Tests"
echo "=========================================="

FAILED=0
TOTAL=0

# Build shared module
echo ""
echo "Building shared module..."
cd backend/shared
mvn clean install -q -DskipTests

# Test each backend service
for service in user-service movie-service booking-service payment-service notification-service; do
    echo ""
    echo "Testing $service..."
    TOTAL=$((TOTAL + 1))
    cd ../$service
    if mvn test -q 2>&1; then
        echo "✅ $service - PASSED"
    else
        echo "❌ $service - FAILED"
        FAILED=$((FAILED + 1))
    fi
done
cd ../..

# Frontend checks
echo ""
echo "Checking frontend..."
TOTAL=$((TOTAL + 1))
cd frontend
if npm run lint 2>&1 && npx tsc --noEmit 2>&1; then
    echo "✅ Frontend - PASSED"
else
    echo "❌ Frontend - FAILED"
    FAILED=$((FAILED + 1))
fi
cd ..

echo ""
echo "=========================================="
PASSED=$((TOTAL - FAILED))
echo "  Results: $PASSED/$TOTAL passed"
if [ $FAILED -gt 0 ]; then
    echo "  ❌ $FAILED test suite(s) failed"
    exit 1
else
    echo "  ✅ All tests passed!"
fi
echo "=========================================="
