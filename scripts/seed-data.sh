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

echo "[1/7] Seeding cities..."
$PSQL_CMD < database/seeds/cities.sql
echo "✅ Cities seeded"

echo "[2/7] Seeding movies..."
$PSQL_CMD < database/seeds/movies.sql
echo "✅ Movies seeded"

echo "[3/7] Seeding theaters (+ screens + seats)..."
$PSQL_CMD < database/seeds/theaters.sql
echo "✅ Theaters seeded"

echo "[4/7] Seeding shows..."
$PSQL_CMD < database/seeds/shows.sql
echo "✅ Shows seeded"

echo "[5/7] Seeding show seats..."
$PSQL_CMD < database/seeds/show_seats.sql
echo "✅ Show seats seeded"

echo "[6/7] Seeding coupons..."
$PSQL_CMD < database/seeds/coupons.sql
echo "✅ Coupons seeded"

echo "[7/7] Seeding bookings & reviews..."
$PSQL_CMD < database/seeds/bookings.sql
$PSQL_CMD < database/seeds/reviews.sql
echo "✅ Bookings & reviews seeded"

echo ""
echo "=========================================="
echo "  ✅ Database fully seeded!"
echo "==========================================" 
