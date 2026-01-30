#!/bin/bash
# AI Testing Script
# Runs AI vs AI games with turn monitoring

set -e

# Clean up old log file
rm -f game_test.log

echo "Starting AI test..."
echo "Game logs will be written to: game_test.log"
echo "Turn-by-turn status will be shown below:"
echo "=========================================="
echo ""

# Run the test with AI testing log configuration
mvn -q exec:java \
  -Dlogback.configurationFile=src/main/resources/logback-test.xml \
  -Dexec.mainClass="com.developingstorm.games.sad.testing.AITestCLI" \
  -Dexec.args="$*"

echo ""
echo "=========================================="
echo "Test complete. Check game_test.log for detailed logs."
