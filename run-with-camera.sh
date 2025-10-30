#!/bin/bash

# Script to run the application with proper camera permissions on macOS

echo "Starting Student Attendance System with camera access..."
echo "=========================================="
echo ""
echo "NOTE: If this is your first time running, macOS will ask for camera permission."
echo "Please click 'OK' or 'Allow' when prompted."
echo ""

# Run the application
mvn clean javafx:run

echo ""
echo "Application closed."
