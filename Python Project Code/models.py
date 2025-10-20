"""
Data models for the Employee Shift Scheduler.
"""
from typing import Dict, List, Union
from pydantic import BaseModel, Field


DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
SHIFTS = ["Morning", "Afternoon", "Evening"]
MIN_PER_SHIFT = 2
MAX_DAYS_PER_EMP = 5


class Employee(BaseModel):
    """Employee model with shift preferences."""
    name: str
    preferences: Dict[str, Union[str, Dict[str, int]]] = Field(default_factory=dict)

    def get_day_preferences(self, day: str) -> List[str]:
        """Get ordered list of shift preferences for a given day."""
        pref = self.preferences.get(day)
        if not pref:
            return []

        if isinstance(pref, str):
            # Single preference
            return [pref]
        elif isinstance(pref, dict):
            # Ranked preferences (shift: priority)
            # Lower priority number = higher preference
            sorted_prefs = sorted(pref.items(), key=lambda x: x[1])
            return [shift for shift, _ in sorted_prefs]

        return []


class ScheduleResult(BaseModel):
    """Result of scheduling operation."""
    schedule: Dict[str, Dict[str, List[str]]] = Field(default_factory=dict)
    work_counts: Dict[str, int] = Field(default_factory=dict)
    conflicts: List[str] = Field(default_factory=list)
    satisfaction_metrics: Dict[str, float] = Field(default_factory=dict)

    def to_dict(self):
        """Convert to dictionary for JSON export."""
        return {
            "schedule": self.schedule,
            "work_counts": self.work_counts,
            "conflicts": self.conflicts,
            "satisfaction_metrics": self.satisfaction_metrics
        }
