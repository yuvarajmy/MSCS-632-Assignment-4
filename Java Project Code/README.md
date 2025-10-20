# Employee Shift Scheduler (JavaFX)

A single-language desktop application built with Java and JavaFX that intelligently schedules employees across Morning, Afternoon, and Evening shifts for a 7-day week. The application keeps scheduling simple with in-memory data management and JSON import/export capabilities.

## Features

- **Employee Management**: Add, edit, and delete employees with unique IDs
- **Flexible Preferences**: Support for both single preferred shifts and ranked preferences (1st, 2nd, 3rd choice) per day
- **Intelligent Scheduling**: Automated shift assignment with conflict resolution and backfill logic
- **Deterministic Random Assignment**: Configurable random seed (default: 42) for reproducible results
- **Multiple Export Formats**: Export schedules as JSON, CSV, or PNG screenshots
- **Visual Schedule Display**: Clear 7x3 grid showing all assignments
- **Metrics & Logs**: Track preference satisfaction and view detailed scheduling decisions

## Prerequisites

- **Java 17+** (JDK 17 or higher)
- **Maven 3.6+** for building and running
- **JavaFX 21+** (included via Maven dependencies)

## Installation & Setup

### 1. Clone or Download the Project

```bash
cd employee-shift-scheduler-javafx
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn javafx:run
```

The application window will launch with a clean, minimal interface.

## How to Use

### Getting Started with Sample Data

1. **Load Sample Data**:
   - Go to `Data` menu → `Load Sample Data`
   - This loads 12 pre-configured employees with varied preferences

2. **Generate Schedule**:
   - Click the **Generate Schedule** button in the top toolbar
   - The application automatically switches to the Schedule tab to display results

3. **View Results**:
   - **Schedule Grid**: See all 21 shifts (7 days × 3 shifts) with assigned employees
   - **Metrics Panel**: View preference satisfaction statistics
   - **Logs Panel**: Review detailed scheduling decisions and conflict resolutions

### Working with Employees

#### Adding Employees
1. Go to the **Employees & Preferences** tab
2. Click **Add Employee**
3. Enter the employee name
4. The employee is added with an auto-generated unique ID

#### Setting Preferences
1. Select an employee from the table
2. For each day (Mon-Sun), choose one of three options:
   - **No Preference**: Employee has no preference for that day
   - **Single Preference**: Choose one preferred shift (Morning/Afternoon/Evening)
   - **Ranked Preferences**: Rank up to 3 shifts (1st, 2nd, 3rd choice)
3. Preferences are saved automatically when you make changes

#### Editing/Deleting Employees
- **Edit**: Select employee → Click **Edit Employee** → Update name
- **Delete**: Select employee → Click **Delete Employee** → Confirm deletion

### Configuring the Random Seed

The **Random Seed** field (default: 42) controls deterministic random behavior for:
- Conflict resolution when multiple solutions exist
- Backfilling shifts that need more employees

**To change the seed:**
1. Enter a different number in the seed field (e.g., 100, 999)
2. Click **Generate Schedule**
3. The same seed with the same employee data will always produce the same schedule

### Exporting Data

#### Export Employees (JSON)
- `File` menu → `Export Employees JSON`
- Saves all employees and their preferences
- File location: Choose your preferred location (default: `employees.json`)

#### Import Employees (JSON)
- `File` menu → `Import Employees JSON`
- Loads employees from a previously exported JSON file

#### Export Schedule (JSON)
- Click **Export Schedule JSON** in the Schedule tab
- Includes full schedule, statistics, and logs
- File location: Choose your preferred location (default: `schedule.json`)

#### Export Schedule (CSV)
- Click **Export Schedule CSV** in the Schedule tab
- Creates a simple table: Day, Shift, Employees
- File location: Choose your preferred location (default: `schedule.csv`)

#### Save Schedule as PNG
- Click **Save Schedule as PNG** in the Schedule tab
- Takes a snapshot of the schedule grid
- File location: Choose your preferred location (default: `schedule.png`)

### Reset Schedule

To clear the current schedule without deleting employees:
- Go to `Data` menu → `Reset Schedule`

## Scheduling Algorithm

The scheduler follows a multi-pass approach with clear control structures:

### Constraints
- **MAX_DAYS_PER_EMP**: 5 workdays per week maximum
- **MIN_PER_SHIFT**: 2 employees per shift minimum
- **ONE_SHIFT_PER_DAY**: Each employee can work at most one shift per day

### Algorithm Phases

#### 1. Preference Pass
- Iterates through each employee and each day
- **If Ranked Preferences**: Tries shifts in priority order (1 → 2 → 3)
  - If placement succeeds, logs success and moves to next preference
  - If placement fails, adds to unresolved list
- **If Single Preference**: Tries the single preferred shift
  - If placement succeeds, logs success
  - If placement fails, adds to unresolved list
- **Placement Conditions**:
  - Employee not already scheduled that day
  - Employee hasn't exceeded 5 workdays

#### 2. Same-Day Alternatives Pass
- For each unresolved placement request:
  - Tries all three shifts on the original preferred day
  - If any shift has availability, places employee and logs as "same-day alternative"
  - Otherwise, keeps in unresolved list

#### 3. Next-Day Spillover Pass
- For remaining unresolved requests:
  - Advances circularly through days of the week (wraps from SUN to MON)
  - For each subsequent day, tries all three shifts
  - If placement succeeds, logs as "spillover from [original day]"
  - If all days exhausted, logs as "FAILED TO PLACE"

#### 4. Backfill Pass
- For each (day, shift) combination:
  - Checks if fewer than MIN_PER_SHIFT (2) employees assigned
  - **While under minimum**:
    - Collects eligible employees (not scheduled that day, under 5 workdays)
    - If no eligible employees, logs warning and breaks
    - Otherwise, randomly selects from eligible pool using seeded RNG
    - Places employee and logs as "BACKFILL"

### Statistics Tracked
- **Total Assignments**: Total number of shifts assigned
- **First Choice**: Assignments matching 1st preference or single preference
- **Second Choice**: Assignments matching 2nd preference
- **Third Choice**: Assignments matching 3rd preference
- **Backfills**: Assignments made to meet minimum staffing
- **Percentages**: Calculated for first/second/third choice satisfaction

## Testing

The project includes comprehensive JUnit 5 tests that verify:

1. **No employee exceeds 5 workdays**
2. **No employee has multiple shifts on the same day**
3. **Every shift has at least 2 employees** (after backfill)
4. **Deterministic scheduling**: Same seed + same data = same schedule
5. **Statistics calculation** accuracy
6. **Logs are generated** and contain expected information
7. **Edge cases**: Empty employee list, single employee

### Run Tests

```bash
mvn test
```

### Test Output
- Console shows pass/fail for each test
- Maven reports total tests run, failures, and errors

## Project Structure

```
employee-shift-scheduler-javafx/
├─ pom.xml                          # Maven configuration
├─ README.md                        # This file
├─ src/main/java/com/scheduler/
│  ├─ MainApp.java                  # JavaFX Application entry point
│  ├─ controller/
│  │  └─ MainController.java        # UI event handlers and logic
│  ├─ model/
│  │  ├─ Shift.java                 # Enum: MORNING, AFTERNOON, EVENING
│  │  ├─ Day.java                   # Enum: MON, TUE, WED, THU, FRI, SAT, SUN
│  │  ├─ Preference.java            # Single or ranked preference model
│  │  ├─ Employee.java              # Employee with ID, name, preferences
│  │  ├─ Schedule.java              # 7x3 grid of assignments
│  │  └─ SchedulingResult.java      # Schedule + stats + logs
│  ├─ service/
│  │  └─ SchedulerService.java      # Core scheduling algorithm
│  ├─ util/
│  │  ├─ IOUtil.java                # JSON import/export, CSV export
│  │  └─ TableSnapshotUtil.java     # PNG snapshot utility
│  └─ view/
│     └─ main_view.fxml             # FXML UI layout
├─ src/main/resources/
│  └─ employees_sample.json         # 12 sample employees
└─ src/test/java/com/scheduler/
   └─ SchedulerServiceTest.java     # JUnit 5 tests
```

## Technologies Used

- **Language**: Java 17+
- **GUI Framework**: JavaFX 21+
- **Build Tool**: Maven
- **JSON Processing**: Jackson (jackson-databind, jackson-datatype-jdk8)
- **Testing**: JUnit 5
- **Random Number Generation**: java.util.Random with configurable seed

## Design Principles

- **Single Responsibility**: Each class has one clear purpose
- **Simplicity**: No database, no web stack—pure desktop application
- **Determinism**: Seeded RNG ensures reproducible results for testing and debugging
- **Clear Control Structures**: Algorithm uses explicit loops, conditionals, and branching for readability
- **Observable Patterns**: Minimal use of bindings; prioritizes clear imperative flow

## Algorithm Notes & Constraints

### Constraint Satisfaction
The scheduler guarantees:
- ✅ No employee works more than 5 days
- ✅ No employee has multiple shifts on the same day
- ✅ Every shift has at least 2 employees (via backfill)

### Preference Satisfaction
The scheduler attempts to maximize preference satisfaction:
- **Best Case**: Employee gets 1st choice or single preference
- **Good Case**: Employee gets 2nd or 3rd choice
- **Acceptable Case**: Employee placed on different day (spillover)
- **Backfill Case**: Employee assigned to meet minimum staffing

### Conflicts & Resolution
When conflicts arise (e.g., too many employees want the same shift):
- First, try other shifts on the same day
- Then, try the next day (wrapping around the week)
- As a last resort, use backfill to ensure minimum staffing

### Determinism
- **Same seed + same employee data = same schedule**
- Useful for:
  - Testing and validation
  - Reproducing specific scenarios
  - Comparing different preference configurations

## File Locations

By default, imported/exported files are saved in:
- **User's choice via file dialog**
- Common locations: Desktop, Documents, project root

The application uses JavaFX `FileChooser` which opens your system's native file picker.

## Screenshots

After launching the application:

1. **Employees & Preferences Tab**: Manage employees and set daily preferences
2. **Schedule Tab**: View the generated 7x3 schedule grid with metrics and logs

To capture a screenshot of the schedule:
- Use the **Save Schedule as PNG** button in the Schedule tab
- Or use your system's screenshot tool (e.g., Cmd+Shift+4 on macOS, Win+Shift+S on Windows)

## Troubleshooting

### Application Won't Start
- Verify Java 17+ is installed: `java -version`
- Ensure Maven is installed: `mvn -version`
- Check that JavaFX dependencies downloaded: `mvn dependency:resolve`

### Tests Fail
- Ensure sample data file exists: `src/main/resources/employees_sample.json`
- Run `mvn clean test` to rebuild and test from scratch

### UI Elements Not Displaying
- Check console for FXML loading errors
- Verify `main_view.fxml` exists in `src/main/resources/`

### Schedule Looks Different Each Time
- Verify you're using the same random seed value
- Check that employee preferences haven't changed

## Future Enhancements

Possible improvements for future versions:
- Multi-week scheduling
- Employee availability exceptions (e.g., vacation days)
- Skill-based shift requirements
- Automatic schedule optimization
- Database persistence (PostgreSQL, SQLite)
- Web-based interface

## License

This project is provided as-is for educational and demonstration purposes.

## Contact & Support

For questions, issues, or contributions, please refer to the project repository or contact the maintainer.

---

**Version**: 1.0
**Last Updated**: October 2025
**Built with**: Java 17, JavaFX 21, Maven, Jackson, JUnit 5
