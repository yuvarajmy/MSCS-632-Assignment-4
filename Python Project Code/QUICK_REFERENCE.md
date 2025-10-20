# Quick Reference Card

## 🚀 Getting Started

```bash
# 1. Install dependencies
pip install streamlit pandas matplotlib pydantic

# 2. Run the app
streamlit run app.py

# 3. Or use the quick start script
./run.sh
```

## 📋 Common Tasks

### Load Sample Data
1. Open sidebar
2. Click "📥 Load Sample Data (12 Employees)"

### Generate Schedule
1. Tab: "⚙️ Generate Schedule"
2. Set seed (default: 42)
3. Click "🚀 Generate Weekly Schedule"

### Export Schedule
1. Tab: "📊 Schedule & Exports"
2. Click download button (CSV/JSON/PNG)

### Add New Employee
1. Tab: "👥 Employees & Preferences"
2. Enter name
3. Click "➕ Add Employee"

### Set Preferences
**Single Shift:**
- Select "Single Shift"
- Choose shift from dropdown
- Click "Save"

**Ranked Preferences:**
- Select "Ranked Preferences"
- Assign priorities (1-3)
- Click "Save"

## 🔧 Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Navigate tabs | Tab key |
| Submit form | Enter |
| Scroll page | Arrow keys |
| Reload app | R (in Streamlit) |

## 📊 Understanding Metrics

| Metric | Meaning |
|--------|---------|
| 1st Preference | Assigned to first choice |
| 2nd Preference | Assigned to second choice |
| 3rd Preference | Assigned to third choice |
| Alternative/Backfill | Other assignments |

## 🎯 Scheduling Rules

- ✅ Max 5 days/week per employee
- ✅ Min 2 employees per shift
- ✅ 1 shift per day per employee
- ✅ 7 days: Mon-Sun
- ✅ 3 shifts: Morning, Afternoon, Evening

## 🔍 Conflict Log Icons

| Icon | Meaning |
|------|---------|
| ⚠️ | Warning/Conflict |
| ✓ | Resolved |
| 🔧 | Backfill |
| 📅 | Spillover |
| ❌ | Error |

## 💾 File Formats

### JSON (employees.json)
```json
[
  {
    "name": "Alice",
    "preferences": {
      "Mon": "Morning",
      "Tue": {"Morning": 1, "Afternoon": 2}
    }
  }
]
```

### CSV (schedule.csv)
```
Day,Morning,Afternoon,Evening
Mon,Alice,Bob,Charlie
Tue,Diana,Ethan,Fiona
...
```

## 🧪 Running Tests

```bash
python tests/test_scheduler.py
```

**Tests verify:**
- Max days constraint
- No double-booking
- Minimum coverage
- Deterministic results

## 🐛 Troubleshooting

**Problem**: Dependencies not installed
**Solution**: `pip install -r requirements.txt`

**Problem**: Port 8501 in use
**Solution**: `streamlit run app.py --server.port 8502`

**Problem**: PNG export fails
**Solution**: Ensure matplotlib is installed

**Problem**: Non-deterministic results
**Solution**: Use same seed and employee order

## 📁 Project Structure

```
project/
├── app.py              # Main application
├── scheduler.py        # Scheduling logic
├── models.py           # Data models
├── utils.py            # I/O utilities
├── requirements.txt    # Dependencies
├── data/
│   └── sample_employees.json
└── tests/
    └── test_scheduler.py
```

## 🎨 Preference Examples

**Example 1: Fixed Morning Shifts**
```json
{
  "Mon": "Morning",
  "Wed": "Morning",
  "Fri": "Morning"
}
```

**Example 2: Flexible with Priorities**
```json
{
  "Mon": {"Morning": 1, "Afternoon": 2},
  "Thu": {"Afternoon": 1, "Evening": 2, "Morning": 3}
}
```

**Example 3: Weekend Evening Preference**
```json
{
  "Sat": "Evening",
  "Sun": "Evening"
}
```

## 📈 Algorithm Phases

1. **Preference Pass** → Assign first choices
2. **Same-Day Alternative** → Try other shifts same day
3. **Spillover** → Try subsequent days
4. **Backfill** → Ensure minimum coverage

## 🔒 Data Safety

- In-memory storage (session-based)
- No automatic persistence
- Manual export required
- No data loss on schedule regeneration
- Employees preserved when resetting schedule

## 🌐 Browser Compatibility

✅ Chrome (recommended)
✅ Firefox
✅ Safari
✅ Edge

## 💡 Tips

1. **Use deterministic seed** for reproducible demos
2. **Export employees** before making major changes
3. **Check conflict logs** to understand assignments
4. **Review metrics** to optimize preferences
5. **Test with small dataset** first

## 📞 Getting Help

1. Check **README.md** for full documentation
2. Review **INSTALL.md** for setup issues
3. Read **PROJECT_SUMMARY.md** for technical details
4. Run tests to verify installation

## 🎓 Learning Path

1. Load sample data
2. Generate schedule with default settings
3. Review the schedule table
4. Check conflict logs
5. Export to all formats
6. Add your own employees
7. Customize preferences
8. Experiment with different seeds
9. Run tests to understand constraints
10. Modify constants in models.py

## ⚡ Performance

- **12 employees**: <1 second
- **25 employees**: ~1-2 seconds
- **50 employees**: ~2-5 seconds

## 🔐 Security Notes

- No authentication required
- Local execution only
- No external API calls
- No data transmission
- Session-based storage

## 📝 License

Educational and commercial use permitted.

---

**Version**: 1.0
**Last Updated**: 2025-10-19
**Python**: 3.10+
**Framework**: Streamlit
