"""
Streamlit GUI for Employee Shift Scheduler.
"""
import streamlit as st
import json
import os
from models import Employee, DAYS, SHIFTS, MIN_PER_SHIFT, MAX_DAYS_PER_EMP
from scheduler import ShiftScheduler
from utils import (
    export_employees_to_json,
    import_employees_from_json,
    export_schedule_to_json,
    export_schedule_to_csv,
    save_schedule_as_png,
    create_schedule_dataframe
)


# Page configuration
st.set_page_config(
    page_title="Employee Shift Scheduler",
    page_icon="📅",
    layout="wide"
)


# Initialize session state
if 'employees' not in st.session_state:
    st.session_state.employees = []
if 'schedule_result' not in st.session_state:
    st.session_state.schedule_result = None
if 'random_seed' not in st.session_state:
    st.session_state.random_seed = 42
if 'enforce_max_days' not in st.session_state:
    st.session_state.enforce_max_days = True


def load_sample_data():
    """Load sample employees from JSON."""
    try:
        sample_path = os.path.join("data", "sample_employees.json")
        st.session_state.employees = import_employees_from_json(sample_path)
        st.success(f"Loaded {len(st.session_state.employees)} sample employees!")
    except Exception as e:
        st.error(f"Error loading sample data: {e}")


def add_employee(name: str):
    """Add a new employee."""
    if not name:
        st.error("Employee name cannot be empty!")
        return

    if any(emp.name == name for emp in st.session_state.employees):
        st.error(f"Employee '{name}' already exists!")
        return

    new_emp = Employee(name=name, preferences={})
    st.session_state.employees.append(new_emp)
    st.success(f"Added employee: {name}")


def delete_employee(emp_name: str):
    """Delete an employee."""
    st.session_state.employees = [
        emp for emp in st.session_state.employees if emp.name != emp_name
    ]
    st.success(f"Deleted employee: {emp_name}")


def update_preference(emp_name: str, day: str, preference_type: str, values: dict):
    """Update employee preference for a day."""
    for emp in st.session_state.employees:
        if emp.name == emp_name:
            if preference_type == "none":
                if day in emp.preferences:
                    del emp.preferences[day]
            elif preference_type == "single":
                emp.preferences[day] = values["shift"]
            elif preference_type == "ranked":
                emp.preferences[day] = values
            break


# Sidebar
with st.sidebar:
    st.title("📅 Shift Scheduler")
    st.markdown("---")

    st.subheader("Rules Summary")
    st.markdown(f"""
    - **Max {MAX_DAYS_PER_EMP} days/week** per employee
    - **Min {MIN_PER_SHIFT} employees** per shift
    - **1 shift max** per day per employee
    - **3 shifts**: Morning, Afternoon, Evening
    """)

    st.markdown("---")
    st.subheader("Quick Actions")

    if st.button("📥 Load Sample Data (12 Employees)", use_container_width=True):
        load_sample_data()

    uploaded_file = st.file_uploader("Import JSON", type=['json'])
    if uploaded_file is not None:
        try:
            data = json.load(uploaded_file)
            st.session_state.employees = [Employee(**emp_data) for emp_data in data]
            st.success(f"Imported {len(st.session_state.employees)} employees!")
        except Exception as e:
            st.error(f"Error importing: {e}")

    if st.session_state.employees:
        st.markdown(f"**Current employees:** {len(st.session_state.employees)}")

    # Conflict log viewer
    if st.session_state.schedule_result and st.session_state.schedule_result.conflicts:
        st.markdown("---")
        st.subheader("Conflict Logs")
        with st.expander("View Logs", expanded=False):
            for conflict in st.session_state.schedule_result.conflicts:
                if "❌" in conflict or "⚠️" in conflict:
                    st.error(conflict)
                elif "✓" in conflict or "🔧" in conflict or "📅" in conflict:
                    st.info(conflict)


# Main content
st.title("Employee Shift Scheduler")
st.markdown("Manage employee shift preferences and generate optimized weekly schedules.")

# Tabs
tab1, tab2, tab3 = st.tabs(["👥 Employees & Preferences", "⚙️ Generate Schedule", "📊 Schedule & Exports"])

# TAB 1: Employees & Preferences
with tab1:
    st.header("Employees & Preferences")

    # Add new employee
    col1, col2 = st.columns([3, 1])
    with col1:
        new_emp_name = st.text_input("Employee Name", key="new_emp_input")
    with col2:
        st.write("")
        st.write("")
        if st.button("➕ Add Employee", use_container_width=True):
            add_employee(new_emp_name)
            st.rerun()

    st.markdown("---")

    if not st.session_state.employees:
        st.info("No employees added yet. Add employees or load sample data to get started.")
    else:
        # Display employees
        for idx, emp in enumerate(st.session_state.employees):
            with st.expander(f"👤 {emp.name}", expanded=False):
                col1, col2 = st.columns([5, 1])

                with col2:
                    if st.button("🗑️ Delete", key=f"del_{idx}"):
                        delete_employee(emp.name)
                        st.rerun()

                with col1:
                    st.subheader("Shift Preferences by Day")

                    # Create a grid for preferences
                    for day in DAYS:
                        st.markdown(f"**{day}**")

                        pref_type = st.radio(
                            f"Preference type for {day}",
                            ["None", "Single Shift", "Ranked Preferences"],
                            key=f"pref_type_{emp.name}_{day}",
                            horizontal=True,
                            label_visibility="collapsed"
                        )

                        if pref_type == "Single Shift":
                            current_pref = emp.preferences.get(day)
                            default_shift = current_pref if isinstance(current_pref, str) else SHIFTS[0]

                            shift = st.selectbox(
                                f"Select shift for {day}",
                                SHIFTS,
                                index=SHIFTS.index(default_shift) if default_shift in SHIFTS else 0,
                                key=f"single_{emp.name}_{day}",
                                label_visibility="collapsed"
                            )

                            if st.button(f"Save {day}", key=f"save_single_{emp.name}_{day}"):
                                update_preference(emp.name, day, "single", {"shift": shift})
                                st.success(f"Saved: {day} → {shift}")

                        elif pref_type == "Ranked Preferences":
                            current_pref = emp.preferences.get(day)
                            if isinstance(current_pref, dict):
                                default_ranks = current_pref
                            else:
                                default_ranks = {}

                            st.write("Assign priority (1=highest, 3=lowest):")
                            cols = st.columns(3)

                            ranks = {}
                            for i, shift in enumerate(SHIFTS):
                                with cols[i]:
                                    rank = st.number_input(
                                        f"{shift}",
                                        min_value=0,
                                        max_value=3,
                                        value=default_ranks.get(shift, 0),
                                        key=f"rank_{emp.name}_{day}_{shift}",
                                        help="0 = not preferred, 1-3 = priority"
                                    )
                                    if rank > 0:
                                        ranks[shift] = rank

                            if st.button(f"Save {day}", key=f"save_ranked_{emp.name}_{day}"):
                                if ranks:
                                    update_preference(emp.name, day, "ranked", ranks)
                                    st.success(f"Saved ranked preferences for {day}")
                                else:
                                    st.warning("No priorities set")

                        st.markdown("---")

        # Export employees
        st.subheader("Export Employee Data")
        col1, col2 = st.columns(2)

        with col1:
            if st.button("💾 Export Employees to JSON", use_container_width=True):
                try:
                    export_employees_to_json(st.session_state.employees, "employees.json")
                    with open("employees.json", "r") as f:
                        st.download_button(
                            label="⬇️ Download employees.json",
                            data=f.read(),
                            file_name="employees.json",
                            mime="application/json",
                            use_container_width=True
                        )
                except Exception as e:
                    st.error(f"Export error: {e}")

# TAB 2: Generate Schedule
with tab2:
    st.header("Generate Weekly Schedule")

    if not st.session_state.employees:
        st.warning("Please add employees in the 'Employees & Preferences' tab first.")
    else:
        st.subheader("Scheduling Options")

        col1, col2 = st.columns(2)

        with col1:
            seed = st.number_input(
                "Random Seed (for deterministic results)",
                min_value=0,
                value=st.session_state.random_seed,
                help="Use the same seed for reproducible schedules"
            )
            st.session_state.random_seed = seed

        with col2:
            enforce = st.checkbox(
                f"Enforce max {MAX_DAYS_PER_EMP} days per employee",
                value=st.session_state.enforce_max_days,
                help="If unchecked, employees may work more than 5 days"
            )
            st.session_state.enforce_max_days = enforce

        st.markdown("---")

        if st.button("🚀 Generate Weekly Schedule", use_container_width=True, type="primary"):
            with st.spinner("Generating schedule..."):
                try:
                    scheduler = ShiftScheduler(
                        st.session_state.employees,
                        random_seed=st.session_state.random_seed
                    )
                    result = scheduler.generate_schedule()
                    st.session_state.schedule_result = result

                    st.success("✅ Schedule generated successfully!")

                    # Display summary
                    st.subheader("Scheduling Summary")

                    col1, col2, col3 = st.columns(3)

                    with col1:
                        st.metric("Total Assignments", int(result.satisfaction_metrics.get('total_assignments', 0)))

                    with col2:
                        total_conflicts = len([c for c in result.conflicts if "❌" in c or "⚠️" in c])
                        st.metric("Conflicts", total_conflicts)

                    with col3:
                        first_pref_pct = result.satisfaction_metrics.get('first_preference', 0)
                        st.metric("1st Preference Match", f"{first_pref_pct:.1f}%")

                    # Show live logs
                    st.subheader("Scheduling Logs")
                    log_container = st.container()

                    with log_container:
                        if result.conflicts:
                            for log in result.conflicts:
                                if "❌" in log:
                                    st.error(log)
                                elif "⚠️" in log:
                                    st.warning(log)
                                elif "✓" in log or "🔧" in log or "📅" in log:
                                    st.info(log)
                        else:
                            st.info("No conflicts or issues detected.")

                except Exception as e:
                    st.error(f"Error generating schedule: {e}")

# TAB 3: Schedule & Exports
with tab3:
    st.header("Schedule & Exports")

    if st.session_state.schedule_result is None:
        st.info("Generate a schedule in the 'Generate Schedule' tab to view results.")
    else:
        result = st.session_state.schedule_result

        # Display schedule table
        st.subheader("Weekly Schedule")

        df = create_schedule_dataframe(result)
        st.dataframe(df, use_container_width=True, height=350)

        # Work counts
        st.subheader("Employee Work Days")
        work_df = [{"Employee": name, "Days Worked": count} for name, count in sorted(result.work_counts.items())]
        st.dataframe(work_df, use_container_width=True)

        # Preference satisfaction metrics
        st.subheader("Preference Satisfaction Metrics")

        metrics = result.satisfaction_metrics

        col1, col2, col3, col4 = st.columns(4)

        with col1:
            st.metric("1st Preference", f"{metrics.get('first_preference', 0):.1f}%")
        with col2:
            st.metric("2nd Preference", f"{metrics.get('second_preference', 0):.1f}%")
        with col3:
            st.metric("3rd Preference", f"{metrics.get('third_preference', 0):.1f}%")
        with col4:
            backfill_pct = (
                metrics.get('spillover', 0) +
                metrics.get('same_day_alternative', 0) +
                metrics.get('backfill', 0)
            )
            st.metric("Alternative/Backfill", f"{backfill_pct:.1f}%")

        st.markdown("---")

        # Export options
        st.subheader("Export Options")

        col1, col2, col3 = st.columns(3)

        with col1:
            if st.button("📄 Download CSV", use_container_width=True):
                try:
                    export_schedule_to_csv(result, "schedule.csv")
                    with open("schedule.csv", "r") as f:
                        st.download_button(
                            label="⬇️ schedule.csv",
                            data=f.read(),
                            file_name="schedule.csv",
                            mime="text/csv",
                            use_container_width=True
                        )
                except Exception as e:
                    st.error(f"CSV export error: {e}")

        with col2:
            if st.button("📋 Download JSON", use_container_width=True):
                try:
                    export_schedule_to_json(result, "schedule.json")
                    with open("schedule.json", "r") as f:
                        st.download_button(
                            label="⬇️ schedule.json",
                            data=f.read(),
                            file_name="schedule.json",
                            mime="application/json",
                            use_container_width=True
                        )
                except Exception as e:
                    st.error(f"JSON export error: {e}")

        with col3:
            if st.button("🖼️ Save as PNG", use_container_width=True):
                try:
                    save_schedule_as_png(result, "schedule.png")
                    with open("schedule.png", "rb") as f:
                        st.download_button(
                            label="⬇️ schedule.png",
                            data=f.read(),
                            file_name="schedule.png",
                            mime="image/png",
                            use_container_width=True
                        )
                    st.success("✅ PNG saved to project root as schedule.png")
                except Exception as e:
                    st.error(f"PNG export error: {e}")

        st.markdown("---")

        # Reset button
        col1, col2, col3 = st.columns([1, 1, 1])
        with col2:
            if st.button("🔄 Reset Schedule", use_container_width=True, type="secondary"):
                if st.session_state.get('confirm_reset', False):
                    st.session_state.schedule_result = None
                    st.session_state.confirm_reset = False
                    st.success("Schedule cleared!")
                    st.rerun()
                else:
                    st.session_state.confirm_reset = True
                    st.warning("⚠️ Click again to confirm reset")


# Footer
st.markdown("---")
st.markdown(
    "<div style='text-align: center; color: #666;'>Employee Shift Scheduler | Built with Streamlit</div>",
    unsafe_allow_html=True
)
