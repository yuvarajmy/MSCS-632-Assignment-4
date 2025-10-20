# Installation Guide

## Quick Start

### 1. Ensure Python 3.10+ is Installed

```bash
python3 --version
```

Should show Python 3.10 or higher.

### 2. Create Virtual Environment (Recommended)

```bash
# Create virtual environment
python3 -m venv venv

# Activate it
# On macOS/Linux:
source venv/bin/activate

# On Windows:
venv\Scripts\activate
```

### 3. Install Dependencies

```bash
pip install streamlit pandas matplotlib pydantic
```

Or using the requirements file:

```bash
pip install -r requirements.txt
```

### 4. Run the Application

```bash
streamlit run app.py
```

The app will open at `http://localhost:8501`

### 5. Load Demo Data

Once the app opens:
1. Click "📥 Load Sample Data (12 Employees)" in the sidebar
2. Go to "⚙️ Generate Schedule" tab
3. Click "🚀 Generate Weekly Schedule"
4. View results in "📊 Schedule & Exports" tab

## Running Tests

```bash
python tests/test_scheduler.py
```

## Troubleshooting

### pip not found

If `pip` is not available, try:

```bash
python3 -m ensurepip --upgrade
```

Or install pip separately for your OS.

### Import Errors

Ensure all dependencies are installed:

```bash
pip list | grep -E "streamlit|pandas|matplotlib|pydantic"
```

### Port Already in Use

If port 8501 is occupied:

```bash
streamlit run app.py --server.port 8502
```

## System Requirements

- Python 3.10 or higher
- 500 MB free disk space
- Modern web browser (Chrome, Firefox, Safari, Edge)
- Internet connection (for initial Streamlit setup)

## Dependencies

- **streamlit** (>=1.28.0): Web UI framework
- **pandas** (>=2.0.0): Data manipulation
- **matplotlib** (>=3.7.0): Chart generation
- **pydantic** (>=2.0.0): Data validation

## Offline Installation

Download packages on a connected machine:

```bash
pip download -r requirements.txt -d ./packages
```

Then install on offline machine:

```bash
pip install --no-index --find-links=./packages -r requirements.txt
```
