package TodoApp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class TodoListEnhanced extends Application {

    private ObservableList<Task> tasks;
    private ListView<Task> listView;
    private ComboBox<String> categoryFilterBox;
    private static final String[] DEFAULT_CATEGORIES = {"All", "Work", "Personal", "Study", "Other"};
    private File dataFile = new File("tasks.json");

    @Override
    public void start(Stage primaryStage) {
        tasks = FXCollections.observableArrayList();
        loadTasks();

        listView = new ListView<>(tasks);
        listView.setCellFactory(param -> new TaskCell());

        // --- Inputs ---

        TextField inputField = new TextField();
        inputField.setPromptText("Enter new task");

        ComboBox<TaskPriority> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll(TaskPriority.values());
        priorityBox.setValue(TaskPriority.MEDIUM);

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(Arrays.copyOfRange(DEFAULT_CATEGORIES, 1, DEFAULT_CATEGORIES.length)); // skip "All" for entry
        categoryBox.setEditable(true);
        categoryBox.setPromptText("Category");
        categoryBox.setValue("Work");

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Due Date");

        DatePicker reminderDatePicker = new DatePicker();
        reminderDatePicker.setPromptText("Remind On");
        ComboBox<String> reminderTimeBox = new ComboBox<>();
        for (int hour = 0; hour < 24; hour++) {
            reminderTimeBox.getItems().add(String.format("%02d:00", hour));
            reminderTimeBox.getItems().add(String.format("%02d:30", hour));
        }
        reminderTimeBox.setPromptText("Time");

        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> {
            addTask(inputField, priorityBox, categoryBox, dueDatePicker, reminderDatePicker, reminderTimeBox);
        });

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                addTask(inputField, priorityBox, categoryBox, dueDatePicker, reminderDatePicker, reminderTimeBox);
            }
        });

        // --- Category Filter ----
        categoryFilterBox = new ComboBox<>();
        categoryFilterBox.getItems().addAll(DEFAULT_CATEGORIES);
        categoryFilterBox.setValue("All");
        categoryFilterBox.setOnAction(e -> filterTasks());

        HBox inputRow1 = new HBox(10, inputField, categoryBox, priorityBox);
        HBox inputRow2 = new HBox(10, dueDatePicker, reminderDatePicker, reminderTimeBox, addButton);
        inputRow1.setPadding(new Insets(10, 10, 2, 10));
        inputRow2.setPadding(new Insets(2, 10, 10, 10));

        VBox inputVBox = new VBox(inputRow1, inputRow2, new Label("View: "), categoryFilterBox);

        VBox root = new VBox(10, inputVBox, listView);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 750, 480);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Enhanced To-Do List");
        primaryStage.show();

        // On exit, save tasks
        primaryStage.setOnCloseRequest(e -> saveTasks());
    }

    private void addTask(TextField inputField, ComboBox<TaskPriority> priorityBox, ComboBox<String> categoryBox,
                         DatePicker dueDatePicker, DatePicker reminderDatePicker, ComboBox<String> reminderTimeBox) {
        String text = inputField.getText().trim();
        String category = categoryBox.getValue() != null ? categoryBox.getValue().trim() : "Other";
        if (category.isEmpty()) category = "Other";
        if (!text.isEmpty()) {
            LocalDate dueDate = dueDatePicker.getValue();
            LocalDate remindDate = reminderDatePicker.getValue();
            LocalTime remindTime = null;
            if (remindDate != null && reminderTimeBox.getValue() != null) {
                remindTime = LocalTime.parse(reminderTimeBox.getValue());
            }
            Task task = new Task(
                    text,
                    priorityBox.getValue(),
                    category,
                    dueDate,
                    remindDate != null && remindTime != null ? LocalDateTime.of(remindDate, remindTime) : null
            );
            tasks.add(task);
            inputField.clear();
            dueDatePicker.setValue(null);
            reminderDatePicker.setValue(null);
            reminderTimeBox.setValue(null);
            saveTasks();
            filterTasks();
        }
    }

    // For category filtering
    private void filterTasks() {
        String category = categoryFilterBox.getValue();
        if (category == null || category.equals("All")) {
            listView.setItems(tasks);
        } else {
            listView.setItems(tasks.filtered(t -> t.getCategory().equalsIgnoreCase(category)));
        }
    }

    // --- Save and Load from File (JSON) ---
    private void saveTasks() {
        try (Writer writer = new FileWriter(dataFile)) {
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
            gson.toJson(new ArrayList<>(tasks), writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadTasks() {
        if (!dataFile.exists()) return;
        try (Reader reader = new FileReader(dataFile)) {
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
            List<Task> loadedTasks = gson.fromJson(reader, new TypeToken<ArrayList<Task>>(){}.getType());
            if (loadedTasks != null) tasks.addAll(loadedTasks);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Task and Related Classes ---
    public static class Task {
        private String text;
        private TaskPriority priority;
        private String category;
        private LocalDate dueDate;
        private LocalDateTime reminderDateTime;
        private boolean completed;

        public Task(String text, TaskPriority priority, String category, LocalDate dueDate, LocalDateTime reminderDateTime) {
            this.text = text;
            this.priority = priority;
            this.category = category;
            this.dueDate = dueDate;
            this.reminderDateTime = reminderDateTime;
            this.completed = false;
        }

        public String getText() { return text; }
        public TaskPriority getPriority() { return priority; }
        public String getCategory() { return category; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDateTime getReminderDateTime() { return reminderDateTime; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean b) { completed = b; }
    }

    public enum TaskPriority {
        HIGH("High", 3, "⚠", "#ef4444"),
        MEDIUM("Medium", 2, "➖", "#facc15"),
        LOW("Low", 1, "✔", "#22c55e");

        public final String label;
        public final int weight;
        public final String symbol;
        public final String colorHex;

        TaskPriority(String label, int weight, String symbol, String colorHex) {
            this.label = label;
            this.weight = weight;
            this.symbol = symbol;
            this.colorHex = colorHex;
        }

        @Override public String toString() { return label; }
    }

    // --- TaskCell for custom list cell ---
    private class TaskCell extends ListCell<Task> {
        private final CheckBox completedBox = new CheckBox();
        private final Label textLabel = new Label();
        private final Label catLabel = new Label();
        private final Label dueDateLabel = new Label();
        private final Label reminderLabel = new Label();
        private final Label priorityLabel = new Label();
        private final Button deleteBtn = new Button("✖");
        private final HBox container = new HBox(8);

        public TaskCell() {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            deleteBtn.setOnAction(e -> {
                Task task = getItem();
                if (task != null) {
                    tasks.remove(task);
                    saveTasks();
                    filterTasks();
                }
            });

            completedBox.setOnAction(e -> {
                Task task = getItem();
                if (task != null) {
                    task.setCompleted(completedBox.isSelected());
                    updateItem(task, false);
                    saveTasks();
                }
            });

            container.getChildren().addAll(priorityLabel, completedBox, textLabel, catLabel, dueDateLabel, reminderLabel, spacer, deleteBtn);
            container.setPadding(new Insets(5));
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
            } else {
                completedBox.setSelected(task.isCompleted());
                textLabel.setText(task.getText());
                catLabel.setText("[" + task.getCategory() + "]");
                catLabel.setTextFill(Color.web("#6366f1")); // violet-ish

                if (task.getDueDate() != null) {
                    dueDateLabel.setText("Due: " + task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd")));
                    dueDateLabel.setTextFill(Color.web("#2dd4bf"));
                } else {
                    dueDateLabel.setText("");
                }

                if (task.getReminderDateTime() != null) {
                    reminderLabel.setText("🔔 " + task.getReminderDateTime().format(DateTimeFormatter.ofPattern("MMM dd HH:mm")));
                    reminderLabel.setTextFill(Color.web("#f59e42"));
                } else {
                    reminderLabel.setText("");
                }

                priorityLabel.setText(task.getPriority().symbol);
                priorityLabel.setTextFill(Color.web(task.getPriority().colorHex));

                textLabel.setStyle(task.isCompleted() ? "-fx-strikethrough: true; -fx-text-fill: grey;" : "");
                setGraphic(container);
            }
        }
    }

    // --- Gson TypeAdapters ---
    static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        public JsonElement serialize(LocalDate src, java.lang.reflect.Type t, JsonSerializationContext c) { return new JsonPrimitive(src.toString()); }
        public LocalDate deserialize(JsonElement json, java.lang.reflect.Type t, JsonDeserializationContext c) { return LocalDate.parse(json.getAsString()); }
    }
    static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        public JsonElement serialize(LocalDateTime src, java.lang.reflect.Type t, JsonSerializationContext c) { return new JsonPrimitive(src.toString()); }
        public LocalDateTime deserialize(JsonElement json, java.lang.reflect.Type t, JsonDeserializationContext c) { return LocalDateTime.parse(json.getAsString()); }
    }

    public static void main(String[] args) { launch(args); }
}
