#!/bin/bash
# ============================================
# Create geo-shard databases on first startup
# ============================================
# The "north" shard uses the default POSTGRES_DB (bookmyshow).
# This script creates the "south" shard database with the same schema.
# Both databases run inside the same Postgres instance (for local dev).
# In production, each shard would be a separate Postgres cluster.

set -e

echo "Creating south shard database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE bookmyshow_south OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'bookmyshow_south')\gexec

    -- Enable UUID extension in south database
    \c bookmyshow_south
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOSQL

echo "South shard database created successfully."
