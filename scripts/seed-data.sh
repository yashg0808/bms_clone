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

echo "Seeding cities..."
$PSQL_CMD < database/seeds/cities.sql
echo "✅ Cities seeded"

echo "Seeding theaters..."
$PSQL_CMD < database/seeds/theaters.sql
echo "✅ Theaters seeded"

echo "Seeding movies..."
$PSQL_CMD < database/seeds/movies.sql
echo "✅ Movies seeded"

echo ""
echo "=========================================="
echo "  ✅ Database seeded successfully!"
echo "=========================================="
