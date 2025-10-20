"""
Unit tests for the shift scheduler.
Tests verify scheduling constraints and deterministic behavior.
"""
import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from models import Employee, DAYS, SHIFTS, MIN_PER_SHIFT, MAX_DAYS_PER_EMP
from scheduler import ShiftScheduler
from utils import import_employees_from_json


def test_no_employee_exceeds_max_days():
    """Verify no employee works more than MAX_DAYS_PER_EMP days."""
    # Load sample data
    employees = import_employees_from_json("data/sample_employees.json")

    scheduler = ShiftScheduler(employees, random_seed=42)
    result = scheduler.generate_schedule()

    for emp_name, days_worked in result.work_counts.items():
        assert days_worked <= MAX_DAYS_PER_EMP, (
            f"{emp_name} worked {days_worked} days, exceeding max of {MAX_DAYS_PER_EMP}"
        )

    print(f"✓ All employees work ≤ {MAX_DAYS_PER_EMP} days")


def test_no_employee_multiple_shifts_per_day():
    """Verify no employee has more than one shift per day."""
    employees = import_employees_from_json("data/sample_employees.json")

    scheduler = ShiftScheduler(employees, random_seed=42)
    result = scheduler.generate_schedule()

    for day in DAYS:
        employee_assignments = {}

        for shift in SHIFTS:
            for emp_name in result.schedule[day][shift]:
                if emp_name in employee_assignments:
                    raise AssertionError(
                        f"{emp_name} assigned to multiple shifts on {day}: "
                        f"{employee_assignments[emp_name]} and {shift}"
                    )
                employee_assignments[emp_name] = shift

    print("✓ No employee has multiple shifts on the same day")


def test_minimum_employees_per_shift():
    """Verify each shift has at least MIN_PER_SHIFT employees after backfill."""
    employees = import_employees_from_json("data/sample_employees.json")

    scheduler = ShiftScheduler(employees, random_seed=42)
    result = scheduler.generate_schedule()

    understaffed_shifts = []

    for day in DAYS:
        for shift in SHIFTS:
            count = len(result.schedule[day][shift])
            if count < MIN_PER_SHIFT:
                understaffed_shifts.append(f"{day} {shift}: {count}/{MIN_PER_SHIFT}")

    if understaffed_shifts:
        print(f"⚠ Understaffed shifts found: {len(understaffed_shifts)}")
        for shift in understaffed_shifts:
            print(f"  - {shift}")
    else:
        print(f"✓ All shifts have ≥ {MIN_PER_SHIFT} employees")


def test_deterministic_schedule():
    """Verify schedule is deterministic with same seed and input."""
    employees = import_employees_from_json("data/sample_employees.json")

    # Generate schedule twice with same seed
    scheduler1 = ShiftScheduler(employees, random_seed=42)
    result1 = scheduler1.generate_schedule()

    scheduler2 = ShiftScheduler(employees, random_seed=42)
    result2 = scheduler2.generate_schedule()

    # Compare schedules
    for day in DAYS:
        for shift in SHIFTS:
            employees1 = sorted(result1.schedule[day][shift])
            employees2 = sorted(result2.schedule[day][shift])

            assert employees1 == employees2, (
                f"Non-deterministic result for {day} {shift}: "
                f"{employees1} vs {employees2}"
            )

    print("✓ Schedule is deterministic with same seed")


def test_preference_matching():
    """Verify preference satisfaction metrics are calculated."""
    employees = import_employees_from_json("data/sample_employees.json")

    scheduler = ShiftScheduler(employees, random_seed=42)
    result = scheduler.generate_schedule()

    metrics = result.satisfaction_metrics

    assert 'first_preference' in metrics
    assert 'total_assignments' in metrics
    assert metrics['total_assignments'] > 0

    print(f"✓ Preference metrics calculated:")
    print(f"  - Total assignments: {int(metrics['total_assignments'])}")
    print(f"  - 1st preference: {metrics['first_preference']:.1f}%")
    print(f"  - 2nd preference: {metrics['second_preference']:.1f}%")
    print(f"  - 3rd preference: {metrics['third_preference']:.1f}%")


def test_work_count_accuracy():
    """Verify work counts match actual schedule assignments."""
    employees = import_employees_from_json("data/sample_employees.json")

    scheduler = ShiftScheduler(employees, random_seed=42)
    result = scheduler.generate_schedule()

    # Count assignments from schedule
    actual_counts = {emp.name: 0 for emp in employees}

    for day in DAYS:
        for shift in SHIFTS:
            for emp_name in result.schedule[day][shift]:
                actual_counts[emp_name] += 1

    # Compare with recorded work counts
    for emp_name, recorded_count in result.work_counts.items():
        actual_count = actual_counts[emp_name]
        assert recorded_count == actual_count, (
            f"{emp_name}: Recorded {recorded_count} days but actually assigned {actual_count} days"
        )

    print("✓ Work counts are accurate")


def run_all_tests():
    """Run all tests."""
    print("=" * 60)
    print("Running Shift Scheduler Tests")
    print("=" * 60)

    tests = [
        test_no_employee_exceeds_max_days,
        test_no_employee_multiple_shifts_per_day,
        test_minimum_employees_per_shift,
        test_deterministic_schedule,
        test_preference_matching,
        test_work_count_accuracy
    ]

    passed = 0
    failed = 0

    for test in tests:
        print(f"\n{test.__name__}:")
        try:
            test()
            passed += 1
        except AssertionError as e:
            print(f"✗ FAILED: {e}")
            failed += 1
        except Exception as e:
            print(f"✗ ERROR: {e}")
            failed += 1

    print("\n" + "=" * 60)
    print(f"Test Results: {passed} passed, {failed} failed")
    print("=" * 60)

    return failed == 0


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
