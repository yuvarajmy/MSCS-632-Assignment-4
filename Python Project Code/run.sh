#!/bin/bash

# Employee Shift Scheduler - Quick Start Script

echo "=================================="
echo "Employee Shift Scheduler"
echo "=================================="
echo ""

# Check Python version
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 not found. Please install Python 3.10+"
    exit 1
fi

PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
echo "✓ Python version: $PYTHON_VERSION"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo ""
    echo "Creating virtual environment..."
    python3 -m venv venv
    echo "✓ Virtual environment created"
fi

# Activate virtual environment
echo ""
echo "Activating virtual environment..."
source venv/bin/activate

# Check if dependencies are installed
if ! python3 -c "import streamlit" 2>/dev/null; then
    echo ""
    echo "Installing dependencies..."
    pip install -q streamlit pandas matplotlib pydantic
    echo "✓ Dependencies installed"
else
    echo "✓ Dependencies already installed"
fi

# Run tests
echo ""
echo "Running tests..."
python tests/test_scheduler.py
TEST_RESULT=$?

if [ $TEST_RESULT -eq 0 ]; then
    echo ""
    echo "=================================="
    echo "✓ All tests passed!"
    echo "=================================="
    echo ""
    echo "Starting Streamlit application..."
    echo ""
    echo "The app will open at: http://localhost:8501"
    echo "Press Ctrl+C to stop the server"
    echo ""
    streamlit run app.py
else
    echo ""
    echo "❌ Tests failed. Please check the output above."
    exit 1
fi
