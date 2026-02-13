#!/bin/bash
# Quick test with very short turn limit
mvn -q exec:java \
  -Dexec.mainClass="com.developingstorm.games.sad.testing.AITestCLI" \
  -Dexec.args="single baseline/v1.0 profiles/easy --turn-limit 10" \
  2>&1 | tail -100
