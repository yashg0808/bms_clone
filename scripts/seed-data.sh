#!/bin/bash
set -e

echo "=========================================="
echo "  BookMyShow Clone - Seed Database"
echo "=========================================="

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-bookmyshow}
DB_USER=${DB_USERNAME:-bookmyshow_user}

echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "Using docker to run psql..."
    PSQL_CMD="docker exec -i bms_clone-postgres-1 psql -U $DB_USER -d $DB_NAME"
else
    PSQL_CMD="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
fi

echo "[1/8] Seeding cities..."
$PSQL_CMD < database/seeds/cities.sql
echo "✅ Cities seeded"

echo "[2/8] Seeding users..."
$PSQL_CMD < database/seeds/users.sql
echo "✅ Users seeded"

echo "[3/8] Seeding movies..."
$PSQL_CMD < database/seeds/movies.sql
echo "✅ Movies seeded"

echo "[4/8] Seeding theaters (+ screens + seats)..."
$PSQL_CMD < database/seeds/theaters.sql
echo "✅ Theaters seeded"

echo "[5/8] Seeding shows..."
$PSQL_CMD < database/seeds/shows.sql
echo "✅ Shows seeded"

echo "[6/8] Seeding show seats..."
$PSQL_CMD < database/seeds/show_seats.sql
echo "✅ Show seats seeded"

echo "[7/8] Seeding coupons..."
$PSQL_CMD < database/seeds/coupons.sql
echo "✅ Coupons seeded"

echo "[8/8] Seeding bookings, payments, reviews, audit log..."
$PSQL_CMD < database/seeds/bookings.sql
$PSQL_CMD < database/seeds/reviews.sql
echo "✅ Bookings, payments, reviews & audit log seeded"

echo ""
echo "=========================================="
echo "  ✅ Database fully seeded!"
echo "=========================================="
echo ""
echo "Test accounts (password for all: Test@1234):"
echo "  Admin:    admin@bookmyshow.com"
echo "  Customer: rahul@example.com"
echo "  Customer: sneha@example.com"
echo "  Customer: test@example.com"
echo "=========================================="
