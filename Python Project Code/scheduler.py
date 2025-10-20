"""
Core scheduling logic for the Employee Shift Scheduler.
"""
import random
from typing import Dict, List, Tuple
from models import Employee, ScheduleResult, DAYS, SHIFTS, MIN_PER_SHIFT, MAX_DAYS_PER_EMP


class ShiftScheduler:
    """Main scheduler class implementing the scheduling algorithm."""

    def __init__(self, employees: List[Employee], random_seed: int = 42):
        self.employees = employees
        self.random_seed = random_seed
        self.schedule: Dict[str, Dict[str, List[str]]] = {}
        self.work_counts: Dict[str, int] = {}
        self.conflicts: List[str] = []
        self.assignments_log: List[Tuple[str, str, str, int]] = []  # (employee, day, shift, preference_rank)

    def initialize_schedule(self):
        """Initialize empty schedule and work counts."""
        self.schedule = {day: {shift: [] for shift in SHIFTS} for day in DAYS}
        self.work_counts = {emp.name: 0 for emp in self.employees}
        self.conflicts = []
        self.assignments_log = []
        random.seed(self.random_seed)

    def can_assign(self, emp_name: str, day: str) -> bool:
        """Check if employee can be assigned on a given day."""
        # Check if already assigned that day
        for shift in SHIFTS:
            if emp_name in self.schedule[day][shift]:
                return False

        # Check if exceeded max days per week
        if self.work_counts[emp_name] >= MAX_DAYS_PER_EMP:
            return False

        return True

    def assign_employee(self, emp_name: str, day: str, shift: str, preference_rank: int = 0):
        """Assign an employee to a shift."""
        self.schedule[day][shift].append(emp_name)
        self.work_counts[emp_name] += 1
        self.assignments_log.append((emp_name, day, shift, preference_rank))

    def try_assign_preferences(self):
        """First pass: Try to assign employees to their preferred shifts."""
        for emp in self.employees:
            for day in DAYS:
                prefs = emp.get_day_preferences(day)
                if not prefs:
                    continue

                assigned = False
                for rank, shift in enumerate(prefs, start=1):
                    if self.can_assign(emp.name, day):
                        self.assign_employee(emp.name, day, shift, preference_rank=rank)
                        assigned = True
                        break

                if not assigned and prefs:
                    self.conflicts.append(
                        f"⚠️ {emp.name}: Could not assign on {day} (preferences: {', '.join(prefs)}) - "
                        f"{'Already scheduled' if not self.can_assign(emp.name, day) else 'Max days reached'}"
                    )

    def try_same_day_alternatives(self):
        """Second pass: For conflicts, try other shifts on the same day."""
        unresolved = []
        for conflict in self.conflicts:
            # Parse conflict to try alternative
            if "Could not assign" in conflict:
                parts = conflict.split(":")
                emp_name = parts[0].replace("⚠️", "").strip()

                # Extract day from conflict message
                for day in DAYS:
                    if f"on {day}" in conflict:
                        # Find the employee
                        emp = next((e for e in self.employees if e.name == emp_name), None)
                        if not emp:
                            unresolved.append(conflict)
                            continue

                        prefs = emp.get_day_preferences(day)
                        assigned = False

                        # Try any shift on the same day
                        for shift in SHIFTS:
                            if shift not in prefs and self.can_assign(emp_name, day):
                                self.assign_employee(emp_name, day, shift, preference_rank=99)
                                self.conflicts.append(
                                    f"✓ Resolved: {emp_name} assigned to {shift} on {day} (alternative)"
                                )
                                assigned = True
                                break

                        if not assigned:
                            unresolved.append(conflict)
                        break

        # Keep only unresolved conflicts
        self.conflicts = [c for c in self.conflicts if "Could not assign" in c and c in unresolved]

    def try_spillover_to_next_days(self):
        """Third pass: Try to assign on subsequent days if same-day alternatives failed."""
        for emp in self.employees:
            if self.work_counts[emp.name] >= MAX_DAYS_PER_EMP:
                continue

            # Check if employee has any unassigned preferred days
            for day_idx, day in enumerate(DAYS):
                prefs = emp.get_day_preferences(day)
                if not prefs:
                    continue

                # Check if already assigned this day
                already_assigned = any(emp.name in self.schedule[day][shift] for shift in SHIFTS)
                if already_assigned:
                    continue

                # Try to assign to next available day
                for next_day_offset in range(1, 7):
                    next_day_idx = (day_idx + next_day_offset) % 7
                    next_day = DAYS[next_day_idx]

                    if self.can_assign(emp.name, next_day):
                        # Try preferred shifts first, then any shift
                        for shift in prefs + [s for s in SHIFTS if s not in prefs]:
                            if self.can_assign(emp.name, next_day):
                                self.assign_employee(emp.name, next_day, shift, preference_rank=98)
                                self.conflicts.append(
                                    f"📅 Spillover: {emp.name} assigned to {shift} on {next_day} "
                                    f"(originally wanted {day})"
                                )
                                break
                        break

    def backfill_understaffed_shifts(self):
        """Fourth pass: Ensure MIN_PER_SHIFT employees per shift."""
        for day in DAYS:
            for shift in SHIFTS:
                current_count = len(self.schedule[day][shift])

                if current_count < MIN_PER_SHIFT:
                    needed = MIN_PER_SHIFT - current_count

                    # Find eligible employees
                    eligible = [
                        emp for emp in self.employees
                        if self.can_assign(emp.name, day)
                    ]

                    # Randomly select from eligible
                    random.shuffle(eligible)
                    assigned_count = 0

                    for emp in eligible:
                        if assigned_count >= needed:
                            break

                        self.assign_employee(emp.name, day, shift, preference_rank=100)
                        self.conflicts.append(
                            f"🔧 Backfill: {emp.name} assigned to {shift} on {day} "
                            f"(understaffed: {current_count}/{MIN_PER_SHIFT})"
                        )
                        assigned_count += 1

                    if assigned_count < needed:
                        self.conflicts.append(
                            f"❌ Warning: {shift} on {day} only has "
                            f"{current_count + assigned_count}/{MIN_PER_SHIFT} employees"
                        )

    def calculate_satisfaction_metrics(self) -> Dict[str, float]:
        """Calculate preference satisfaction metrics."""
        total_assignments = len(self.assignments_log)
        if total_assignments == 0:
            return {
                "first_preference": 0.0,
                "second_preference": 0.0,
                "third_preference": 0.0,
                "backfill_alternative": 0.0
            }

        rank_counts = {1: 0, 2: 0, 3: 0, 98: 0, 99: 0, 100: 0}
        for _, _, _, rank in self.assignments_log:
            if rank <= 3:
                rank_counts[rank] += 1
            elif rank == 98:
                rank_counts[98] += 1  # Spillover
            elif rank == 99:
                rank_counts[99] += 1  # Same-day alternative
            else:
                rank_counts[100] += 1  # Backfill

        return {
            "first_preference": (rank_counts[1] / total_assignments) * 100,
            "second_preference": (rank_counts[2] / total_assignments) * 100,
            "third_preference": (rank_counts[3] / total_assignments) * 100,
            "spillover": (rank_counts[98] / total_assignments) * 100,
            "same_day_alternative": (rank_counts[99] / total_assignments) * 100,
            "backfill": (rank_counts[100] / total_assignments) * 100,
            "total_assignments": total_assignments
        }

    def generate_schedule(self) -> ScheduleResult:
        """Main scheduling algorithm."""
        self.initialize_schedule()

        # Pass 1: Assign preferences
        self.try_assign_preferences()

        # Pass 2: Try same-day alternatives
        self.try_same_day_alternatives()

        # Pass 3: Spillover to next days
        self.try_spillover_to_next_days()

        # Pass 4: Backfill understaffed shifts
        self.backfill_understaffed_shifts()

        # Calculate satisfaction
        satisfaction = self.calculate_satisfaction_metrics()

        return ScheduleResult(
            schedule=self.schedule,
            work_counts=self.work_counts,
            conflicts=self.conflicts,
            satisfaction_metrics=satisfaction
        )
