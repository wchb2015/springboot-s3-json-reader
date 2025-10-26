#!/bin/bash

echo "Spring Boot S3 Scheduler Test Script"
echo "====================================="
echo ""
echo "This script will test the scheduled loader functionality."
echo "Make sure your AWS credentials are configured and you have access to the S3 bucket."
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8080/api/scheduler"

echo -e "${YELLOW}1. Checking scheduler status...${NC}"
curl -s -X GET "$BASE_URL/status" | python3 -m json.tool
echo ""

echo -e "${YELLOW}2. Checking scheduler health...${NC}"
curl -s -X GET "$BASE_URL/health" | python3 -m json.tool
echo ""

echo -e "${YELLOW}3. Triggering manual load...${NC}"
curl -s -X POST "$BASE_URL/trigger-load" | python3 -m json.tool
echo ""

echo -e "${YELLOW}4. Getting cached data...${NC}"
curl -s -X GET "$BASE_URL/cached-data" | python3 -m json.tool
echo ""

echo -e "${YELLOW}5. Checking status after load...${NC}"
curl -s -X GET "$BASE_URL/status" | python3 -m json.tool
echo ""

echo -e "${GREEN}Test complete!${NC}"
echo ""
echo "The scheduler is configured to load data every 5 seconds."
echo "You can monitor the logs to see the scheduled loads happening."
echo ""
echo "To change the schedule interval, set the SCHEDULER_FIXED_DELAY environment variable (in milliseconds)."
echo "Example: export SCHEDULER_FIXED_DELAY=10000  # 10 seconds"
