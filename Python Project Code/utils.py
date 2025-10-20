"""
Utility functions for I/O and export operations.
"""
import json
import pandas as pd
import matplotlib.pyplot as plt
from typing import Dict, List
from models import Employee, ScheduleResult, DAYS, SHIFTS


def export_employees_to_json(employees: List[Employee], filepath: str = "employees.json"):
    """Export employees and their preferences to JSON."""
    data = [{"name": emp.name, "preferences": emp.preferences} for emp in employees]
    with open(filepath, 'w') as f:
        json.dump(data, f, indent=2)


def import_employees_from_json(filepath: str) -> List[Employee]:
    """Import employees from JSON file."""
    with open(filepath, 'r') as f:
        data = json.load(f)
    return [Employee(**emp_data) for emp_data in data]


def export_schedule_to_json(result: ScheduleResult, filepath: str = "schedule.json"):
    """Export schedule result to JSON."""
    with open(filepath, 'w') as f:
        json.dump(result.to_dict(), f, indent=2)


def export_schedule_to_csv(result: ScheduleResult, filepath: str = "schedule.csv"):
    """Export schedule to CSV format."""
    rows = []
    for day in DAYS:
        row = {"Day": day}
        for shift in SHIFTS:
            employees = result.schedule[day][shift]
            row[shift] = ", ".join(employees) if employees else "-"
        rows.append(row)

    df = pd.DataFrame(rows)
    df.to_csv(filepath, index=False)


def save_schedule_as_png(result: ScheduleResult, filepath: str = "schedule.png"):
    """Save schedule as PNG image using matplotlib."""
    fig, ax = plt.subplots(figsize=(16, 10))
    ax.axis('tight')
    ax.axis('off')

    # Prepare table data
    table_data = []
    header = ["Day"] + SHIFTS

    for day in DAYS:
        row = [day]
        for shift in SHIFTS:
            employees = result.schedule[day][shift]
            cell_text = "\n".join(employees) if employees else "-"
            row.append(cell_text)
        table_data.append(row)

    # Create table
    table = ax.table(
        cellText=table_data,
        colLabels=header,
        cellLoc='left',
        loc='center',
        colWidths=[0.12, 0.28, 0.28, 0.28]
    )

    # Style the table
    table.auto_set_font_size(False)
    table.set_fontsize(9)
    table.scale(1, 2.5)

    # Header styling
    for i in range(len(header)):
        cell = table[(0, i)]
        cell.set_facecolor('#4CAF50')
        cell.set_text_props(weight='bold', color='white')

    # Alternate row colors
    for i in range(1, len(table_data) + 1):
        for j in range(len(header)):
            cell = table[(i, j)]
            if i % 2 == 0:
                cell.set_facecolor('#f0f0f0')
            else:
                cell.set_facecolor('#ffffff')

    # Add title
    plt.title("Weekly Employee Shift Schedule", fontsize=16, fontweight='bold', pad=20)

    # Add metrics footer
    metrics = result.satisfaction_metrics
    footer_text = (
        f"Total Assignments: {int(metrics.get('total_assignments', 0))} | "
        f"1st Pref: {metrics.get('first_preference', 0):.1f}% | "
        f"2nd Pref: {metrics.get('second_preference', 0):.1f}% | "
        f"3rd Pref: {metrics.get('third_preference', 0):.1f}%"
    )
    plt.figtext(0.5, 0.02, footer_text, ha='center', fontsize=10, style='italic')

    plt.tight_layout()
    plt.savefig(filepath, dpi=300, bbox_inches='tight')
    plt.close()


def create_schedule_dataframe(result: ScheduleResult) -> pd.DataFrame:
    """Create a pandas DataFrame from schedule result."""
    rows = []
    for day in DAYS:
        row = {"Day": day}
        for shift in SHIFTS:
            employees = result.schedule[day][shift]
            row[shift] = ", ".join(employees) if employees else "-"
        rows.append(row)

    return pd.DataFrame(rows)
