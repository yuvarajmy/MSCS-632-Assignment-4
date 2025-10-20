package com.scheduler.controller;

import com.scheduler.model.*;
import com.scheduler.service.SchedulerService;
import com.scheduler.util.IOUtil;
import com.scheduler.util.TableSnapshotUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

public class MainController {

    @FXML private TextField seedField;
    @FXML private TabPane tabPane;
    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private VBox preferencesBox;
    @FXML private GridPane scheduleGrid;
    @FXML private TextArea metricsArea;
    @FXML private TextArea logsArea;

    private ObservableList<Employee> employees;
    private SchedulingResult currentResult;
    private Map<Day, Map<Shift, ComboBox<String>>> preferenceControls;

    @FXML
    public void initialize() {
        employees = FXCollections.observableArrayList();
        preferenceControls = new HashMap<>();

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        employeeTable.setItems(employees);

        employeeTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    updatePreferencesView(newSelection);
                }
            }
        );

        buildPreferencesUI();
    }

    private void buildPreferencesUI() {
        preferencesBox.getChildren().clear();

        for (Day day : Day.values()) {
            VBox dayBox = new VBox(8);
            dayBox.setStyle("-fx-padding: 10; -fx-border-color: #dddddd; -fx-border-width: 1; -fx-background-color: #f9f9f9;");

            Label dayLabel = new Label(day.toString());
            dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

            RadioButton singleRadio = new RadioButton("Single Preference");
            RadioButton rankedRadio = new RadioButton("Ranked Preferences");
            RadioButton noneRadio = new RadioButton("No Preference");

            ToggleGroup modeGroup = new ToggleGroup();
            singleRadio.setToggleGroup(modeGroup);
            rankedRadio.setToggleGroup(modeGroup);
            noneRadio.setToggleGroup(modeGroup);
            noneRadio.setSelected(true);

            ComboBox<String> singleChoice = new ComboBox<>();
            singleChoice.getItems().addAll("MORNING", "AFTERNOON", "EVENING");
            singleChoice.setDisable(true);

            HBox rankedBox = new HBox(10);
            ComboBox<String> rank1 = new ComboBox<>();
            ComboBox<String> rank2 = new ComboBox<>();
            ComboBox<String> rank3 = new ComboBox<>();

            for (ComboBox<String> cb : Arrays.asList(rank1, rank2, rank3)) {
                cb.getItems().addAll("MORNING", "AFTERNOON", "EVENING");
                cb.setPromptText("Select");
                cb.setDisable(true);
            }

            rankedBox.getChildren().addAll(
                new Label("1st:"), rank1,
                new Label("2nd:"), rank2,
                new Label("3rd:"), rank3
            );

            singleRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                singleChoice.setDisable(!newVal);
                if (newVal) {
                    rank1.setDisable(true);
                    rank2.setDisable(true);
                    rank3.setDisable(true);
                    saveCurrentEmployeePreferences();
                }
            });

            rankedRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                rank1.setDisable(!newVal);
                rank2.setDisable(!newVal);
                rank3.setDisable(!newVal);
                if (newVal) {
                    singleChoice.setDisable(true);
                    saveCurrentEmployeePreferences();
                }
            });

            noneRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    singleChoice.setDisable(true);
                    rank1.setDisable(true);
                    rank2.setDisable(true);
                    rank3.setDisable(true);
                    saveCurrentEmployeePreferences();
                }
            });

            singleChoice.setOnAction(e -> saveCurrentEmployeePreferences());
            rank1.setOnAction(e -> saveCurrentEmployeePreferences());
            rank2.setOnAction(e -> saveCurrentEmployeePreferences());
            rank3.setOnAction(e -> saveCurrentEmployeePreferences());

            dayBox.getChildren().addAll(
                dayLabel,
                noneRadio,
                singleRadio,
                singleChoice,
                rankedRadio,
                rankedBox
            );

            preferencesBox.getChildren().add(dayBox);

            Map<String, Object> controls = new HashMap<>();
            controls.put("singleRadio", singleRadio);
            controls.put("rankedRadio", rankedRadio);
            controls.put("noneRadio", noneRadio);
            controls.put("singleChoice", singleChoice);
            controls.put("rank1", rank1);
            controls.put("rank2", rank2);
            controls.put("rank3", rank3);
            controls.put("day", day);

            preferencesBox.setUserData(controls);
        }
    }

    private void updatePreferencesView(Employee employee) {
        if (employee == null) return;

        for (javafx.scene.Node node : preferencesBox.getChildren()) {
            if (node instanceof VBox) {
                VBox dayBox = (VBox) node;
                Day day = null;
                RadioButton singleRadio = null;
                RadioButton rankedRadio = null;
                RadioButton noneRadio = null;
                ComboBox<String> singleChoice = null;
                ComboBox<String> rank1 = null;
                ComboBox<String> rank2 = null;
                ComboBox<String> rank3 = null;

                for (javafx.scene.Node child : dayBox.getChildren()) {
                    if (child instanceof Label && ((Label) child).getStyle().contains("bold")) {
                        day = Day.valueOf(((Label) child).getText());
                    } else if (child instanceof RadioButton) {
                        RadioButton rb = (RadioButton) child;
                        if (rb.getText().equals("Single Preference")) {
                            singleRadio = rb;
                        } else if (rb.getText().equals("Ranked Preferences")) {
                            rankedRadio = rb;
                        } else if (rb.getText().equals("No Preference")) {
                            noneRadio = rb;
                        }
                    } else if (child instanceof ComboBox) {
                        singleChoice = (ComboBox<String>) child;
                    } else if (child instanceof HBox) {
                        HBox rankedBox = (HBox) child;
                        int comboIndex = 0;
                        for (javafx.scene.Node rankedChild : rankedBox.getChildren()) {
                            if (rankedChild instanceof ComboBox) {
                                if (comboIndex == 0) rank1 = (ComboBox<String>) rankedChild;
                                else if (comboIndex == 1) rank2 = (ComboBox<String>) rankedChild;
                                else if (comboIndex == 2) rank3 = (ComboBox<String>) rankedChild;
                                comboIndex++;
                            }
                        }
                    }
                }

                if (day != null) {
                    Preference pref = employee.getPreference(day);

                    if (pref.isSingle()) {
                        singleRadio.setSelected(true);
                        singleChoice.setValue(pref.getSingle().orElse(null));
                    } else if (pref.isRanked()) {
                        rankedRadio.setSelected(true);
                        Map<String, Integer> ranked = pref.getRanked();
                        for (Map.Entry<String, Integer> entry : ranked.entrySet()) {
                            if (entry.getValue() == 1 && rank1 != null) {
                                rank1.setValue(entry.getKey());
                            } else if (entry.getValue() == 2 && rank2 != null) {
                                rank2.setValue(entry.getKey());
                            } else if (entry.getValue() == 3 && rank3 != null) {
                                rank3.setValue(entry.getKey());
                            }
                        }
                    } else {
                        noneRadio.setSelected(true);
                        if (singleChoice != null) singleChoice.setValue(null);
                        if (rank1 != null) rank1.setValue(null);
                        if (rank2 != null) rank2.setValue(null);
                        if (rank3 != null) rank3.setValue(null);
                    }
                }
            }
        }
    }

    private void saveCurrentEmployeePreferences() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        for (javafx.scene.Node node : preferencesBox.getChildren()) {
            if (node instanceof VBox) {
                VBox dayBox = (VBox) node;
                Day day = null;
                RadioButton singleRadio = null;
                RadioButton rankedRadio = null;
                RadioButton noneRadio = null;
                ComboBox<String> singleChoice = null;
                ComboBox<String> rank1 = null;
                ComboBox<String> rank2 = null;
                ComboBox<String> rank3 = null;

                for (javafx.scene.Node child : dayBox.getChildren()) {
                    if (child instanceof Label && ((Label) child).getStyle().contains("bold")) {
                        day = Day.valueOf(((Label) child).getText());
                    } else if (child instanceof RadioButton) {
                        RadioButton rb = (RadioButton) child;
                        if (rb.getText().equals("Single Preference")) {
                            singleRadio = rb;
                        } else if (rb.getText().equals("Ranked Preferences")) {
                            rankedRadio = rb;
                        } else if (rb.getText().equals("No Preference")) {
                            noneRadio = rb;
                        }
                    } else if (child instanceof ComboBox) {
                        singleChoice = (ComboBox<String>) child;
                    } else if (child instanceof HBox) {
                        HBox rankedBox = (HBox) child;
                        int comboIndex = 0;
                        for (javafx.scene.Node rankedChild : rankedBox.getChildren()) {
                            if (rankedChild instanceof ComboBox) {
                                if (comboIndex == 0) rank1 = (ComboBox<String>) rankedChild;
                                else if (comboIndex == 1) rank2 = (ComboBox<String>) rankedChild;
                                else if (comboIndex == 2) rank3 = (ComboBox<String>) rankedChild;
                                comboIndex++;
                            }
                        }
                    }
                }

                if (day != null) {
                    Preference pref = new Preference();

                    if (singleRadio != null && singleRadio.isSelected() && singleChoice != null && singleChoice.getValue() != null) {
                        pref.setSingle(singleChoice.getValue());
                    } else if (rankedRadio != null && rankedRadio.isSelected()) {
                        Map<String, Integer> ranked = new HashMap<>();
                        if (rank1 != null && rank1.getValue() != null) {
                            ranked.put(rank1.getValue(), 1);
                        }
                        if (rank2 != null && rank2.getValue() != null) {
                            ranked.put(rank2.getValue(), 2);
                        }
                        if (rank3 != null && rank3.getValue() != null) {
                            ranked.put(rank3.getValue(), 3);
                        }
                        pref.setRanked(ranked);
                    }

                    selected.setPreference(day, pref);
                }
            }
        }
    }

    @FXML
    private void handleAddEmployee() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Employee");
        dialog.setHeaderText("Enter employee name:");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Employee emp = new Employee(name.trim());
                employees.add(emp);
            }
        });
    }

    @FXML
    private void handleEditEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an employee to edit.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Edit Employee");
        dialog.setHeaderText("Edit employee name:");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                selected.setName(name.trim());
                employeeTable.refresh();
            }
        });
    }

    @FXML
    private void handleDeleteEmployee() {
        Employee selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an employee to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete employee: " + selected.getName() + "?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            employees.remove(selected);
        }
    }

    @FXML
    private void handleGenerateSchedule() {
        saveCurrentEmployeePreferences();

        if (employees.isEmpty()) {
            showAlert("No Employees", "Please add employees before generating a schedule.");
            return;
        }

        try {
            long seed = Long.parseLong(seedField.getText().trim());
            SchedulerService service = new SchedulerService(seed);
            currentResult = service.generateSchedule(new ArrayList<>(employees));

            displaySchedule(currentResult.getSchedule());
            displayMetrics(currentResult.getStats());
            displayLogs(currentResult.getLogs());

            tabPane.getSelectionModel().select(1);
        } catch (NumberFormatException e) {
            showAlert("Invalid Seed", "Please enter a valid number for the random seed.");
        }
    }

    private void displaySchedule(Schedule schedule) {
        scheduleGrid.getChildren().clear();
        scheduleGrid.setStyle("-fx-padding: 10; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: white;");

        Label emptyCorner = new Label("");
        emptyCorner.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0; -fx-border-color: #999999; -fx-border-width: 1; -fx-padding: 8;");
        scheduleGrid.add(emptyCorner, 0, 0);

        Day[] days = Day.values();
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i].toString());
            dayLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0; -fx-border-color: #999999; -fx-border-width: 1; -fx-padding: 8; -fx-min-width: 100;");
            dayLabel.setAlignment(Pos.CENTER);
            scheduleGrid.add(dayLabel, i + 1, 0);
        }

        Shift[] shifts = Shift.values();
        for (int i = 0; i < shifts.length; i++) {
            Label shiftLabel = new Label(shifts[i].toString());
            shiftLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0; -fx-border-color: #999999; -fx-border-width: 1; -fx-padding: 8; -fx-min-width: 80;");
            shiftLabel.setAlignment(Pos.CENTER);
            scheduleGrid.add(shiftLabel, 0, i + 1);
        }

        for (int dayIdx = 0; dayIdx < days.length; dayIdx++) {
            for (int shiftIdx = 0; shiftIdx < shifts.length; shiftIdx++) {
                Day day = days[dayIdx];
                Shift shift = shifts[shiftIdx];
                List<String> assigned = schedule.getAssignedEmployees(day, shift);

                String employeeText = String.join(", ", assigned);
                Label cell = new Label(employeeText);
                cell.setWrapText(true);
                cell.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 8; -fx-min-width: 100; -fx-min-height: 60; -fx-background-color: white;");
                scheduleGrid.add(cell, dayIdx + 1, shiftIdx + 1);
            }
        }
    }

    private void displayMetrics(Map<String, Double> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total Assignments: %.0f\n", stats.getOrDefault("totalAssignments", 0.0)));
        sb.append(String.format("First Choice: %.0f (%.1f%%)\n",
            stats.getOrDefault("firstChoice", 0.0),
            stats.getOrDefault("firstChoicePct", 0.0)));
        sb.append(String.format("Second Choice: %.0f (%.1f%%)\n",
            stats.getOrDefault("secondChoice", 0.0),
            stats.getOrDefault("secondChoicePct", 0.0)));
        sb.append(String.format("Third Choice: %.0f (%.1f%%)\n",
            stats.getOrDefault("thirdChoice", 0.0),
            stats.getOrDefault("thirdChoicePct", 0.0)));
        sb.append(String.format("Backfills: %.0f\n", stats.getOrDefault("backfills", 0.0)));

        metricsArea.setText(sb.toString());
    }

    private void displayLogs(List<String> logs) {
        logsArea.setText(String.join("\n", logs));
    }

    @FXML
    private void handleImportEmployees() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Employees");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = fileChooser.showOpenDialog(employeeTable.getScene().getWindow());
        if (file != null) {
            try {
                List<Employee> imported = IOUtil.importEmployees(file);
                employees.setAll(imported);
                showAlert("Success", "Employees imported successfully.");
            } catch (Exception e) {
                showAlert("Error", "Failed to import employees: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportEmployees() {
        saveCurrentEmployeePreferences();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Employees");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialFileName("employees.json");

        File file = fileChooser.showSaveDialog(employeeTable.getScene().getWindow());
        if (file != null) {
            try {
                IOUtil.exportEmployees(new ArrayList<>(employees), file);
                showAlert("Success", "Employees exported successfully.");
            } catch (Exception e) {
                showAlert("Error", "Failed to export employees: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportScheduleJSON() {
        if (currentResult == null) {
            showAlert("No Schedule", "Please generate a schedule first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Schedule JSON");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialFileName("schedule.json");

        File file = fileChooser.showSaveDialog(scheduleGrid.getScene().getWindow());
        if (file != null) {
            try {
                IOUtil.exportScheduleJSON(currentResult, file);
                showAlert("Success", "Schedule exported successfully.");
            } catch (Exception e) {
                showAlert("Error", "Failed to export schedule: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportScheduleCSV() {
        if (currentResult == null) {
            showAlert("No Schedule", "Please generate a schedule first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Schedule CSV");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("schedule.csv");

        File file = fileChooser.showSaveDialog(scheduleGrid.getScene().getWindow());
        if (file != null) {
            try {
                IOUtil.exportScheduleCSV(currentResult.getSchedule(), file);
                showAlert("Success", "Schedule exported successfully.");
            } catch (Exception e) {
                showAlert("Error", "Failed to export schedule: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSaveSchedulePNG() {
        if (currentResult == null) {
            showAlert("No Schedule", "Please generate a schedule first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Schedule as PNG");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PNG Files", "*.png")
        );
        fileChooser.setInitialFileName("schedule.png");

        File file = fileChooser.showSaveDialog(scheduleGrid.getScene().getWindow());
        if (file != null) {
            try {
                TableSnapshotUtil.saveNodeAsPNG(scheduleGrid, file);
                showAlert("Success", "Schedule saved as PNG successfully.");
            } catch (Exception e) {
                showAlert("Error", "Failed to save schedule: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleResetSchedule() {
        currentResult = null;
        scheduleGrid.getChildren().clear();
        metricsArea.clear();
        logsArea.clear();
    }

    @FXML
    private void handleLoadSampleData() {
        try {
            List<Employee> sampleData = IOUtil.loadSampleData();
            employees.setAll(sampleData);
            showAlert("Success", "Sample data loaded successfully.");
        } catch (Exception e) {
            showAlert("Error", "Failed to load sample data: " + e.getMessage());
        }
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Employee Shift Scheduler");
        alert.setContentText("Version 1.0\n\nA JavaFX application for scheduling employees across shifts.\n\nBuilt with Java 17+ and JavaFX 21+");
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
