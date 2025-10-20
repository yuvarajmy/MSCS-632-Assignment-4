# Employee Shift Scheduler

A comprehensive Python-based employee shift scheduling system with a Streamlit GUI. This application intelligently assigns employees to weekly shifts based on their preferences while respecting business constraints.

## Features

- **Smart Scheduling Algorithm**: Assigns employees to shifts based on ranked preferences
- **Business Constraints**:
  - Maximum 5 workdays per employee per week
  - Minimum 2 employees per shift
  - One shift maximum per day per employee
- **Preference System**: Supports both simple and ranked preferences (1st, 2nd, 3rd choice)
- **Conflict Resolution**: Automatically handles scheduling conflicts with spillover and backfill logic
- **Deterministic Results**: Uses seeded randomization for reproducible schedules
- **Data Persistence**: Import/export employees and schedules via JSON
- **Multiple Export Formats**: CSV, JSON, and PNG image exports
- **Rich UI**: Interactive Streamlit interface with live conflict logs and metrics

## Installation

### Prerequisites

- Python 3.10 or higher
- pip package manager

### Setup Steps

1. **Clone or download this repository**

2. **Create a virtual environment** (recommended):

```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies**:

```bash
pip install -r requirements.txt
```

## Running the Application

### Start the Streamlit App

```bash
streamlit run app.py
```

The application will open in your default web browser (typically at `http://localhost:8501`).

### Quick Start Guide

1. **Load Sample Data**:
   - Click "📥 Load Sample Data (12 Employees)" in the sidebar
   - This loads 12 demo employees with various shift preferences

2. **Generate Schedule**:
   - Navigate to the "⚙️ Generate Schedule" tab
   - Configure options (random seed, enforce max days)
   - Click "🚀 Generate Weekly Schedule"
   - View live logs showing the scheduling process

3. **View & Export**:
   - Go to "📊 Schedule & Exports" tab
   - Review the weekly schedule table
   - Check preference satisfaction metrics
   - Download schedule in CSV, JSON, or PNG format

## Using the Application

### Tab 1: Employees & Preferences

**Adding Employees:**
- Enter employee name and click "➕ Add Employee"

**Setting Preferences:**

For each employee and day, choose from:

1. **None**: No preference for that day
2. **Single Shift**: Choose one preferred shift
3. **Ranked Preferences**: Assign priorities (1=highest, 2=second, 3=third)

**Example Ranked Preference:**
- Morning: 1 (first choice)
- Afternoon: 2 (second choice)
- Evening: 3 (third choice)

**Import/Export:**
- Load existing employee data from JSON
- Export current employees to JSON for backup

### Tab 2: Generate Schedule

**Configuration Options:**

- **Random Seed**: Set for deterministic scheduling (default: 42)
- **Enforce Max Days**: Ensure no employee exceeds 5 workdays (default: on)

**Scheduling Process:**

The algorithm runs in 4 passes:

1. **Preference Pass**: Assign employees to preferred shifts
2. **Same-Day Alternatives**: Try other shifts on the same day for conflicts
3. **Spillover Pass**: Assign to next available day if current day is full
4. **Backfill Pass**: Ensure minimum 2 employees per shift

**Live Logs:**
- ✓ Successful assignments
- ⚠️ Warnings for conflicts
- 🔧 Backfill operations
- 📅 Spillover assignments
- ❌ Errors

### Tab 3: Schedule & Exports

**Weekly Schedule Table:**
- 7 columns (Mon-Sun)
- 3 rows per day (Morning, Afternoon, Evening)
- Lists assigned employees per shift

**Work Days Summary:**
- Shows how many days each employee is scheduled

**Preference Satisfaction Metrics:**
- **1st Preference**: % of assignments matching first choice
- **2nd Preference**: % matching second choice
- **3rd Preference**: % matching third choice
- **Alternative/Backfill**: % assigned via spillover or backfill

**Export Options:**

1. **CSV**: Download schedule as spreadsheet
2. **JSON**: Download full schedule data with metadata
3. **PNG**: Save schedule as image (schedule.png in project root)

**Reset Schedule:**
- Clears the generated schedule (keeps employee data)
- Requires confirmation click

## Understanding schedule.png

When you click "🖼️ Save as PNG", the application:

1. Generates a formatted table image using matplotlib
2. Saves it to the project root as `schedule.png`
3. Provides a download button in the UI
4. Includes preference satisfaction metrics in the footer

The PNG file will appear in your project directory and can be:
- Shared via email or messaging
- Printed for physical posting
- Embedded in presentations or documents

## Taking UI Screenshots

To capture the Streamlit interface:

**On macOS:**
- Press `Cmd + Shift + 4` for selection tool
- Press `Cmd + Shift + 3` for full screen

**On Windows:**
- Press `Win + Shift + S` for Snipping Tool
- Press `PrtScn` for full screen

**On Linux:**
- Press `Shift + PrtScn` for selection tool
- Use `gnome-screenshot` or similar utilities

## Algorithm Details

### Scheduling Rules

1. **One shift per day**: No employee can work multiple shifts on the same day
2. **Maximum 5 days**: Employees work at most 5 days per week
3. **Minimum coverage**: Each shift must have at least 2 employees

### Preference Handling

- **Single Preference**: Direct assignment to preferred shift if available
- **Ranked Preferences**: Try 1st choice → 2nd choice → 3rd choice in order
- **Conflict Resolution**:
  - First, try alternative shifts the same day
  - Then, spillover to subsequent days
  - Finally, backfill understaffed shifts with available employees

### Deterministic Randomness

- Uses Python's `random.seed()` for reproducible results
- Same seed + same input = identical schedule every time
- Useful for testing and demonstrations

## Running Tests

Execute the test suite to verify scheduling constraints:

```bash
python tests/test_scheduler.py
```

**Tests verify:**
- No employee exceeds 5 workdays
- No employee has multiple shifts per day
- All shifts meet minimum staffing (2 employees)
- Schedule is deterministic with same seed
- Work counts match actual assignments
- Preference metrics are calculated correctly

## File Structure

```
employee-shift-scheduler/
├── app.py                          # Streamlit GUI application
├── scheduler.py                    # Core scheduling algorithm
├── models.py                       # Data models and constants
├── utils.py                        # I/O utilities (JSON, CSV, PNG)
├── requirements.txt                # Python dependencies
├── README.md                       # This file
├── data/
│   └── sample_employees.json       # Demo data (12 employees)
├── tests/
│   └── test_scheduler.py           # Unit tests
└── schedule.png                    # Generated schedule image (after export)
```

## Data Format

### Employee JSON Structure

```json
[
  {
    "name": "Alice Johnson",
    "preferences": {
      "Mon": {"Morning": 1, "Afternoon": 2},
      "Tue": "Morning",
      "Wed": {"Morning": 1, "Evening": 3}
    }
  }
]
```

**Preference Formats:**
- Single: `"Mon": "Morning"`
- Ranked: `"Mon": {"Morning": 1, "Afternoon": 2, "Evening": 3}`

### Schedule JSON Structure

```json
{
  "schedule": {
    "Mon": {
      "Morning": ["Alice Johnson", "Bob Smith"],
      "Afternoon": ["Charlie Davis", "Diana Martinez"],
      "Evening": ["Ethan Brown", "Fiona Wilson"]
    }
  },
  "work_counts": {
    "Alice Johnson": 5,
    "Bob Smith": 4
  },
  "conflicts": [...],
  "satisfaction_metrics": {...}
}
```

## Exporting and Sharing

### Export as ZIP

```bash
# Create a zip of the entire project
zip -r employee-scheduler.zip . -x "venv/*" -x "__pycache__/*" -x "*.pyc"
```

### Push to GitHub

```bash
# Initialize git repository
git init

# Add files
git add .

# Create .gitignore
echo "venv/
__pycache__/
*.pyc
.DS_Store
schedule.png
employees.json
schedule.json
schedule.csv" > .gitignore

# Commit
git commit -m "Initial commit: Employee Shift Scheduler"

# Add remote and push
git remote add origin <your-repo-url>
git branch -M main
git push -u origin main
```

## Configuration Constants

Located in `models.py`:

- `DAYS`: 7-day week (Mon-Sun)
- `SHIFTS`: Morning, Afternoon, Evening
- `MIN_PER_SHIFT`: Minimum 2 employees per shift
- `MAX_DAYS_PER_EMP`: Maximum 5 workdays per employee per week

Modify these constants to adjust scheduling rules.

## Troubleshooting

**Issue: Schedule has understaffed shifts**
- Solution: Add more employees or reduce MIN_PER_SHIFT constraint

**Issue: Many conflicts in logs**
- Solution: Review employee preferences for better distribution across days/shifts

**Issue: PNG export fails**
- Solution: Ensure matplotlib is installed correctly (`pip install matplotlib`)

**Issue: Non-deterministic results**
- Solution: Verify same random seed is used and employee list order is consistent

## Advanced Usage

### Custom Seed for Testing

```python
# In app.py or via UI
scheduler = ShiftScheduler(employees, random_seed=123)
result = scheduler.generate_schedule()
```

### Programmatic Access

```python
from models import Employee
from scheduler import ShiftScheduler

# Create employees
employees = [
    Employee(name="John", preferences={"Mon": "Morning", "Tue": "Afternoon"}),
    Employee(name="Jane", preferences={"Mon": {"Morning": 1, "Evening": 2}})
]

# Generate schedule
scheduler = ShiftScheduler(employees, random_seed=42)
result = scheduler.generate_schedule()

# Access results
print(result.schedule)
print(result.work_counts)
print(result.satisfaction_metrics)
```

## Contributing

To extend this project:

1. Add new shift types to `SHIFTS` in `models.py`
2. Modify constraints in `models.py` constants
3. Extend scheduling algorithm in `scheduler.py`
4. Add new export formats in `utils.py`
5. Enhance UI in `app.py`

## License

This project is provided as-is for educational and commercial use.

## Support

For issues, questions, or feature requests, please refer to the project documentation or contact the development team.

---

**Built with Python & Streamlit** | Deterministic Scheduling | Preference-Based Optimization
