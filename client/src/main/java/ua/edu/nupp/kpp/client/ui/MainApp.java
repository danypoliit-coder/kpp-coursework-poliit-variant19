
package ua.edu.nupp.kpp.client.ui;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import ua.edu.nupp.kpp.client.csv.TreeCsvImporter;
import ua.edu.nupp.kpp.client.http.ApiClient;
import ua.edu.nupp.kpp.client.logging.ClientLogger;
import ua.edu.nupp.kpp.client.model.Tree;
import ua.edu.nupp.kpp.client.model.Inspection;
import ua.edu.nupp.kpp.client.model.WorkRequest;
import ua.edu.nupp.kpp.client.model.WorkOrder;

import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainApp extends Application {

    private final ApiClient api = new ApiClient();

    private final TableView<Tree> treeTable = new TableView<>();
    private final TableView<Inspection> inspectionTable = new TableView<>();
    private final TableView<WorkRequest> requestTable = new TableView<>();
    private final TableView<WorkOrder> orderTable = new TableView<>();

    private final ObservableList<Tree> trees =
            FXCollections.observableArrayList();

    private final ObservableList<WorkRequest> requests =
            FXCollections.observableArrayList();

    private final FilteredList<Tree> filteredTrees =
            new FilteredList<>(trees, tree -> true);

    private final FilteredList<WorkRequest> filteredRequests =
            new FilteredList<>(requests, request -> true);

    private final TextField treeSearch = new TextField();

    private final ComboBox<String> requestStatusFilter =
            new ComboBox<>();

    private final Button clearTreeSearch =
            new Button("Очистити пошук");

    private final Button resetRequestFilter =
            new Button("Скинути фільтр");

    private final Label treeSearchCount =
            new Label("Знайдено: 0 із 0");

    private final Label requestFilterCount =
            new Label("Показано: 0 із 0");

    private final Label treeCount = new Label("Дерева: 0");
    private final Label inspectionCount = new Label("Огляди: 0");
    private final Label requestCount = new Label("Заявки: 0");
    private final Label orderCount = new Label("Наряди: 0");

    private final Label statusLabel =
            new Label("Готово до роботи");

    private final ProgressBar importProgress =
            new ProgressBar(0);

    private final Label importStatus =
            new Label("CSV-імпорт не запущено");

    private final Button importButton =
            new Button("Імпорт CSV");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ExecutorService executor =
            Executors.newFixedThreadPool(3, r -> {
                Thread thread = new Thread(r, "greenery-worker");
                thread.setDaemon(true);
                return thread;
            });

    @Override
    public void start(Stage stage) {

        ClientLogger.initialize();

        configureTables();
        configureFilters();

        TabPane tabs = new TabPane();

        Tab treesTab = new Tab("Дерева", createTreeTab());
        Tab inspectionsTab = new Tab("Огляди", createInspectionTab());
        Tab requestsTab = new Tab("Заявки", createRequestTab());
        Tab ordersTab = new Tab("Наряди", createOrderTab());

        for (Tab tab : new Tab[]{
                treesTab, inspectionsTab, requestsTab, ordersTab
        }) {
            tab.setClosable(false);
        }

        tabs.getTabs().addAll(
                treesTab, inspectionsTab, requestsTab, ordersTab
        );

        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> {

                    if (newTab == treesTab) {
                        refreshTrees();
                    } else if (newTab == inspectionsTab) {
                        refreshInspections();
                    } else if (newTab == requestsTab) {
                        refreshRequests();
                    } else if (newTab == ordersTab) {
                        refreshOrders();
                    }
                });

        Label heading = new Label("Greenery Care");
        heading.getStyleClass().add("app-title");

        Label subtitle = new Label(
                "Інформаційна система догляду за міськими зеленими насадженнями"
        );
        subtitle.getStyleClass().add("app-subtitle");

        VBox headingBox = new VBox(5, heading, subtitle);

        HBox header = new HBox(headingBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");

        HBox counters = new HBox(
                12,
                counterCard(treeCount),
                counterCard(inspectionCount),
                counterCard(requestCount),
                counterCard(orderCount)
        );

        counters.setPadding(new Insets(12));
        counters.getStyleClass().add("dashboard-counters");

        VBox top = new VBox(header, counters);

        statusLabel.getStyleClass().add("status-bar");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(tabs);
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 1200, 760);

        URL css = getClass().getResource("/css/style.css");

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("Не знайдено CSS: /css/style.css");
        }

        stage.setTitle("Greenery Care");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(600);
        stage.show();

        refreshTrees();
        refreshInspections();
        refreshRequests();
        refreshOrders();
    }

    // ================= COUNTERS =================

    private VBox counterCard(Label label) {

        label.getStyleClass().add("counter-value");

        VBox card = new VBox(label);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("counter-card");

        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    // ================= TABLE CONFIGURATION =================

    private void configureTables() {

        treeTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        inspectionTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        requestTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        orderTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        treeTable.setPlaceholder(
                new Label("Дерева відсутні")
        );

        inspectionTable.setPlaceholder(
                new Label("Огляди відсутні")
        );

        requestTable.setPlaceholder(
                new Label("Заявки відсутні")
        );

        orderTable.setPlaceholder(
                new Label("Наряди відсутні")
        );

        SortedList<Tree> sortedTrees =
                new SortedList<>(filteredTrees);

        sortedTrees.comparatorProperty().bind(
                treeTable.comparatorProperty()
        );

        treeTable.setItems(sortedTrees);

        SortedList<WorkRequest> sortedRequests =
                new SortedList<>(filteredRequests);

        sortedRequests.comparatorProperty().bind(
                requestTable.comparatorProperty()
        );

        requestTable.setItems(sortedRequests);
    }

    // ================= FILTER CONFIGURATION =================

    private void configureFilters() {

        treeSearch.setPromptText(
                "Пошук за ID, породою або адресою..."
        );
        treeSearch.setPrefWidth(330);

        treeSearch.textProperty().addListener(
                (obs, oldText, newText) -> applyTreeFilter()
        );

        clearTreeSearch.getStyleClass().add("button-secondary");
        clearTreeSearch.setOnAction(event -> treeSearch.clear());

        requestStatusFilter.getItems().setAll(
                "Усі статуси",
                "СТВОРЕНО",
                "ПЕРЕВІРЕНО",
                "ЗАПЛАНОВАНО",
                "ВИКОНАНО"
        );

        requestStatusFilter.setValue("Усі статуси");

        requestStatusFilter.valueProperty().addListener(
                (obs, oldValue, newValue) -> applyRequestFilter()
        );

        resetRequestFilter.getStyleClass().add("button-secondary");
        resetRequestFilter.setOnAction(
                event -> requestStatusFilter.setValue("Усі статуси")
        );

        treeSearchCount.getStyleClass().add("filter-count");
        requestFilterCount.getStyleClass().add("filter-count");

        updateFilterCounts();
    }

    private void applyTreeFilter() {

        String query = treeSearch.getText()
                .trim()
                .toLowerCase(Locale.ROOT);

        filteredTrees.setPredicate(tree -> {

            if (query.isEmpty()) {
                return true;
            }

            return str(tree.getId())
                    .toLowerCase(Locale.ROOT)
                    .contains(query)
                    || str(tree.getSpecies())
                    .toLowerCase(Locale.ROOT)
                    .contains(query)
                    || str(tree.getAddress())
                    .toLowerCase(Locale.ROOT)
                    .contains(query);
        });

        updateFilterCounts();
    }

    private void applyRequestFilter() {

        String selected = requestStatusFilter.getValue();

        filteredRequests.setPredicate(request -> {

            if (selected == null
                    || "Усі статуси".equals(selected)) {
                return true;
            }

            return selected.equals(request.getStatus());
        });

        updateFilterCounts();
    }

    private void updateFilterCounts() {

        treeSearchCount.setText(
                "Знайдено: " + filteredTrees.size()
                        + " із " + trees.size()
        );

        requestFilterCount.setText(
                "Показано: " + filteredRequests.size()
                        + " із " + requests.size()
        );
    }

    // ================= TABLE HELPERS =================

    private <T, V> void addColumn(
            TableView<T> table,
            String title,
            Function<T, V> getter) {

        TableColumn<T, V> column =
                new TableColumn<>(title);

        column.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(
                        getter.apply(data.getValue())
                )
        );

        column.setPrefWidth(145);
        table.getColumns().add(column);
    }

    private Button styledButton(
            String text,
            String styleClass) {

        Button button = new Button(text);

        if (styleClass != null) {
            button.getStyleClass().add(styleClass);
        }

        return button;
    }

    private HBox buttons(Button... buttons) {

        HBox box = new HBox(10, buttons);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10));

        return box;
    }

    private VBox layout(
            TableView<?> table,
            HBox actions) {

        VBox box = new VBox(10, table, actions);
        box.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);

        return box;
    }

    // ================= STATUS BADGES =================

    private void configureRequestStatusColumn() {

        for (TableColumn<WorkRequest, ?> column
                : requestTable.getColumns()) {

            if (!"Статус".equals(column.getText())) {
                continue;
            }

            @SuppressWarnings("unchecked")
            TableColumn<WorkRequest, String> statusColumn =
                    (TableColumn<WorkRequest, String>) column;

            statusColumn.setCellFactory(col ->
                    new TableCell<>() {

                        private final Label badge = new Label();

                        {
                            badge.getStyleClass().add("status-badge");
                        }

                        @Override
                        protected void updateItem(
                                String status,
                                boolean empty) {

                            super.updateItem(status, empty);

                            if (empty || status == null) {
                                setGraphic(null);
                                setText(null);
                                return;
                            }

                            badge.setText(status);

                            badge.getStyleClass().removeAll(
                                    "status-created",
                                    "status-checked",
                                    "status-planned",
                                    "status-completed"
                            );

                            switch (status) {

                                case "СТВОРЕНО" ->
                                        badge.getStyleClass().add(
                                                "status-created"
                                        );

                                case "ПЕРЕВІРЕНО" ->
                                        badge.getStyleClass().add(
                                                "status-checked"
                                        );

                                case "ЗАПЛАНОВАНО" ->
                                        badge.getStyleClass().add(
                                                "status-planned"
                                        );

                                case "ВИКОНАНО" ->
                                        badge.getStyleClass().add(
                                                "status-completed"
                                        );

                                default -> {
                                }
                            }

                            setText(null);
                            setGraphic(badge);
                        }
                    }
            );

            break;
        }
    }

    // ================= DIALOG STYLING =================

    private void styleDialog(Dialog<?> dialog) {

        URL css = getClass().getResource("/css/style.css");

        if (css != null) {
            dialog.getDialogPane()
                    .getStylesheets()
                    .add(css.toExternalForm());
        }

        dialog.getDialogPane()
                .getStyleClass()
                .add("greenery-dialog");
    }

    // ================= BACKGROUND REQUESTS =================

    private <T> void execute(
            Callable<T> operation,
            Consumer<T> success) {

        statusLabel.setText("Виконується запит...");

        Task<T> task = new Task<>() {

            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };

        task.setOnSucceeded(event -> {

            statusLabel.setText("Операцію виконано");

            if (success != null) {
                success.accept(task.getValue());
            }
        });

        task.setOnFailed(event -> {

            statusLabel.setText("Помилка виконання");

            Throwable error = task.getException();

            if (error != null) {
                error.printStackTrace();
            }

            showError(errorMessage(error));
        });

        executor.execute(task);
    }

    private String errorMessage(Throwable error) {

        if (error == null) {
            return "Невідома помилка";
        }

        Throwable cause = error;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause.getMessage() != null
                && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }

        return cause.getClass().getSimpleName();
    }

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        styleDialog(alert);

        alert.setTitle("Помилка");
        alert.setHeaderText("Не вдалося виконати операцію");
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void showInfo(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleDialog(alert);

        alert.setTitle("Інформація");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private boolean confirm(String message) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(alert);

        alert.setTitle("Підтвердження");
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    // ================= TREES =================

    private VBox createTreeTab() {

        addColumn(treeTable, "ID", Tree::getId);
        addColumn(treeTable, "Порода", Tree::getSpecies);
        addColumn(treeTable, "Адреса", Tree::getAddress);
        addColumn(treeTable, "Широта", Tree::getLatitude);
        addColumn(treeTable, "Довгота", Tree::getLongitude);
        addColumn(treeTable, "Технічний стан",
                Tree::getTechnicalState);
        addColumn(treeTable, "Інтенсивність",
                Tree::getPeopleIntensity);
        addColumn(treeTable, "Зелена зона",
                Tree::getGreenArea);

        Button refresh =
                styledButton("Оновити", "button-secondary");

        Button add =
                styledButton("Додати", "button-primary");

        Button edit =
                styledButton("Редагувати", "button-secondary");

        Button delete =
                styledButton("Видалити", "button-danger");

        Button risk =
                styledButton("Розрахувати ризик", "button-primary");

        importButton.getStyleClass().add("button-primary");

        refresh.setOnAction(e -> refreshTrees());

        add.setOnAction(e -> treeDialog(null));

        edit.setOnAction(e -> {

            Tree selected =
                    treeTable.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть дерево");
            } else {
                treeDialog(selected);
            }
        });

        delete.setOnAction(e -> {

            Tree selected =
                    treeTable.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть дерево");
                return;
            }

            if (confirm(
                    "Видалити дерево ID "
                            + selected.getId()
                            + " та пов'язані записи?"
            )) {

                execute(
                        () -> api.deleteTree(selected.getId()),
                        result -> refreshTrees()
                );
            }
        });

        risk.setOnAction(e -> calculateRisk());

        importButton.setOnAction(e -> importTreesFromCsv());

        importProgress.setPrefWidth(350);
        importProgress.setProgress(0);

        importStatus.setWrapText(true);

        Label importTitle =
                new Label("Фоновий CSV-імпорт");

        importTitle.getStyleClass().add("import-title");

        VBox importPanel = new VBox(
                8,
                importTitle,
                importProgress,
                importStatus
        );

        importPanel.getStyleClass().add("import-panel");

        HBox searchBar = new HBox(
                12,
                new Label("Пошук дерев:"),
                treeSearch,
                clearTreeSearch,
                treeSearchCount
        );

        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("filter-bar");

        VBox box = new VBox(
                10,
                searchBar,
                treeTable,
                buttons(
                        refresh,
                        add,
                        edit,
                        delete,
                        risk,
                        importButton
                ),
                importPanel
        );

        box.setPadding(new Insets(12));
        VBox.setVgrow(treeTable, Priority.ALWAYS);

        return box;
    }

    private void refreshTrees() {

        execute(
                api::getTrees,
                list -> {
                    trees.setAll(list);

                    treeCount.setText(
                            "Дерева: " + trees.size()
                    );

                    updateFilterCounts();
                }
        );
    }

    private void treeDialog(Tree existing) {

        Dialog<Tree> dialog = new Dialog<>();
        styleDialog(dialog);

        dialog.setTitle(
                existing == null
                        ? "Додати дерево"
                        : "Редагувати дерево"
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField species = field(
                existing == null ? "" : existing.getSpecies()
        );

        TextField address = field(
                existing == null ? "" : existing.getAddress()
        );

        TextField latitude = field(
                existing == null ? "" : str(existing.getLatitude())
        );

        TextField longitude = field(
                existing == null ? "" : str(existing.getLongitude())
        );

        TextField technical = field(
                existing == null ? "" : str(existing.getTechnicalState())
        );

        TextField intensity = field(
                existing == null ? "" : str(existing.getPeopleIntensity())
        );

        TextField green = field(
                existing == null ? "" : str(existing.getGreenArea())
        );

        GridPane grid = form();

        addRow(grid, 0, "Порода:", species);
        addRow(grid, 1, "Адреса:", address);
        addRow(grid, 2, "Широта:", latitude);
        addRow(grid, 3, "Довгота:", longitude);
        addRow(grid, 4, "Технічний стан (0–100):", technical);
        addRow(grid, 5, "Інтенсивність (0–100):", intensity);
        addRow(grid, 6, "Зелена зона (0–100):", green);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {

            if (button != ButtonType.OK) {
                return null;
            }

            try {

                Tree tree = new Tree();

                if (existing != null) {
                    tree.setId(existing.getId());
                }

                tree.setSpecies(species.getText().trim());
                tree.setAddress(address.getText().trim());
                tree.setLatitude(optionalDecimal(latitude));
                tree.setLongitude(optionalDecimal(longitude));
                tree.setTechnicalState(requiredDecimal(technical));
                tree.setPeopleIntensity(requiredDecimal(intensity));
                tree.setGreenArea(requiredDecimal(green));

                return tree;

            } catch (Exception ex) {

                showError("Некоректні дані: " + ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(tree -> {

            if (existing == null) {

                execute(
                        () -> api.createTree(tree),
                        result -> refreshTrees()
                );

            } else {

                execute(
                        () -> api.updateTree(tree),
                        result -> refreshTrees()
                );
            }
        });
    }

    // ================= CSV IMPORT =================

    private void importTreesFromCsv() {

        if (importButton.isDisabled()) {
            return;
        }

        Path csvPath = Path.of("sample-data", "trees.csv");

        if (!confirm(
                "Імпортувати дерева з файла?\n"
                        + csvPath.toAbsolutePath()
                        + "\n\nПовторний імпорт може створити дублікати."
        )) {
            return;
        }

        TreeCsvImporter task =
                new TreeCsvImporter(api, csvPath);

        importButton.setDisable(true);

        importProgress.progressProperty().unbind();
        importStatus.textProperty().unbind();

        importProgress.progressProperty().bind(
                task.progressProperty()
        );

        importStatus.textProperty().bind(
                task.messageProperty()
        );

        statusLabel.setText("Триває CSV-імпорт...");

        task.setOnSucceeded(event -> {

            importProgress.progressProperty().unbind();
            importStatus.textProperty().unbind();

            TreeCsvImporter.ImportResult result =
                    task.getValue();

            importProgress.setProgress(1);

            importStatus.setText(
                    "Завершено. Імпортовано: "
                            + result.imported()
                            + ", пропущено: "
                            + result.skipped()
            );

            importButton.setDisable(false);

            statusLabel.setText("CSV-імпорт завершено");

            refreshTrees();
            showInfo(result.summary());
        });

        task.setOnFailed(event -> {

            importProgress.progressProperty().unbind();
            importStatus.textProperty().unbind();

            Throwable error = task.getException();

            importStatus.setText("Помилка CSV-імпорту");
            importButton.setDisable(false);

            statusLabel.setText("Помилка CSV-імпорту");

            refreshTrees();
            showError(errorMessage(error));
        });

        task.setOnCancelled(event -> {

            importProgress.progressProperty().unbind();
            importStatus.textProperty().unbind();

            importStatus.setText("Імпорт скасовано");
            importButton.setDisable(false);

            refreshTrees();
        });

        executor.execute(task);
    }

    // ================= RISK CALCULATION =================

    private void calculateRisk() {

        Tree selected =
                treeTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showInfo("Оберіть дерево");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("50");
        styleDialog(dialog);

        dialog.setTitle("Інтегральний показник");
        dialog.setHeaderText("Розрахунок ризику дерева");

        dialog.setContentText(
                "F3 — геометричність втручання (0–100):"
        );

        dialog.showAndWait().ifPresent(value -> {

            try {

                BigDecimal f3 = new BigDecimal(value.trim());

                execute(
                        () -> api.calculateTreeRisk(
                                selected.getId(), f3
                        ),
                        result -> showInfo(
                                "Дерево ID: " + selected.getId()
                                        + "\nІнтегральний показник: "
                                        + result
                        )
                );

            } catch (NumberFormatException ex) {

                showError("Введіть коректне число");
            }
        });
    }

    // ================= INSPECTIONS =================

    private VBox createInspectionTab() {

        addColumn(inspectionTable, "ID", Inspection::getId);
        addColumn(inspectionTable, "ID дерева", Inspection::getTreeId);

        addColumn(inspectionTable, "Дата",
                inspection -> formatDate(
                        inspection.getInspectionDate()
                ));

        addColumn(inspectionTable, "Інспектор",
                Inspection::getInspector);

        addColumn(inspectionTable, "Технічний стан",
                Inspection::getTechnicalState);

        addColumn(inspectionTable, "Примітки",
                Inspection::getNotes);

        Button refresh =
                styledButton("Оновити", "button-secondary");

        Button add =
                styledButton("Додати", "button-primary");

        Button edit =
                styledButton("Редагувати", "button-secondary");

        Button delete =
                styledButton("Видалити", "button-danger");

        refresh.setOnAction(e -> refreshInspections());

        add.setOnAction(e -> inspectionDialog(null));

        edit.setOnAction(e -> {

            Inspection selected =
                    inspectionTable.getSelectionModel()
                            .getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть огляд");
            } else {
                inspectionDialog(selected);
            }
        });

        delete.setOnAction(e -> {

            Inspection selected =
                    inspectionTable.getSelectionModel()
                            .getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть огляд");
                return;
            }

            if (confirm(
                    "Видалити огляд ID "
                            + selected.getId() + "?"
            )) {

                execute(
                        () -> api.deleteInspection(
                                selected.getId()
                        ),
                        result -> refreshInspections()
                );
            }
        });

        return layout(
                inspectionTable,
                buttons(refresh, add, edit, delete)
        );
    }

    private void refreshInspections() {

        execute(
                api::getInspections,
                list -> {
                    inspectionTable.setItems(
                            FXCollections.observableArrayList(list)
                    );

                    inspectionCount.setText(
                            "Огляди: " + list.size()
                    );
                }
        );
    }

    private void inspectionDialog(Inspection existing) {

        Dialog<Inspection> dialog = new Dialog<>();
        styleDialog(dialog);

        dialog.setTitle(
                existing == null
                        ? "Додати огляд"
                        : "Редагувати огляд"
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField treeId = field(
                existing == null ? "" : str(existing.getTreeId())
        );

        DatePicker date = new DatePicker(
                existing == null
                        ? LocalDate.now()
                        : existing.getInspectionDate()
        );

        TextField inspector = field(
                existing == null ? "" : existing.getInspector()
        );

        TextField technical = field(
                existing == null
                        ? ""
                        : str(existing.getTechnicalState())
        );

        TextArea notes = new TextArea(
                existing == null ? "" : existing.getNotes()
        );

        notes.setPrefRowCount(3);

        GridPane grid = form();

        addRow(grid, 0, "ID дерева:", treeId);
        addRow(grid, 1, "Дата огляду:", date);
        addRow(grid, 2, "Інспектор:", inspector);
        addRow(grid, 3, "Технічний стан:", technical);
        addRow(grid, 4, "Примітки:", notes);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {

            if (button != ButtonType.OK) {
                return null;
            }

            try {

                Inspection inspection = new Inspection();

                if (existing != null) {
                    inspection.setId(existing.getId());
                }

                inspection.setTreeId(
                        Long.parseLong(treeId.getText().trim())
                );

                inspection.setInspectionDate(date.getValue());
                inspection.setInspector(inspector.getText().trim());

                inspection.setTechnicalState(
                        requiredDecimal(technical)
                );

                inspection.setNotes(notes.getText());

                return inspection;

            } catch (Exception ex) {

                showError("Некоректні дані: " + ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(inspection -> {

            if (existing == null) {

                execute(
                        () -> api.createInspection(inspection),
                        result -> refreshInspections()
                );

            } else {

                execute(
                        () -> api.updateInspection(inspection),
                        result -> refreshInspections()
                );
            }
        });
    }

    // ================= WORK REQUESTS =================

    private VBox createRequestTab() {

        addColumn(requestTable, "ID", WorkRequest::getId);
        addColumn(requestTable, "ID дерева", WorkRequest::getTreeId);
        addColumn(requestTable, "Опис", WorkRequest::getDescription);
        addColumn(requestTable, "Статус", WorkRequest::getStatus);

        addColumn(requestTable, "Створено",
                request -> formatDateTime(
                        request.getCreatedAt()
                ));

        addColumn(requestTable, "Заплановано",
                request -> formatDate(
                        request.getPlannedDate()
                ));

        configureRequestStatusColumn();

        Button refresh =
                styledButton("Оновити", "button-secondary");

        Button add =
                styledButton("Додати", "button-primary");

        Button edit =
                styledButton("Редагувати", "button-secondary");

        Button delete =
                styledButton("Видалити", "button-danger");

        Button status =
                styledButton("Змінити статус", "button-primary");

        refresh.setOnAction(e -> refreshRequests());

        add.setOnAction(e -> requestDialog(null));

        edit.setOnAction(e -> {

            WorkRequest selected =
                    requestTable.getSelectionModel()
                            .getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть заявку");
            } else {
                requestDialog(selected);
            }
        });

        delete.setOnAction(e -> {

            WorkRequest selected =
                    requestTable.getSelectionModel()
                            .getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть заявку");
                return;
            }

            if (confirm(
                    "Видалити заявку ID "
                            + selected.getId() + "?"
            )) {

                execute(
                        () -> api.deleteWorkRequest(
                                selected.getId()
                        ),
                        result -> refreshRequests()
                );
            }
        });

        status.setOnAction(e -> changeStatus());

        HBox filterBar = new HBox(
                12,
                new Label("Фільтр за статусом:"),
                requestStatusFilter,
                resetRequestFilter,
                requestFilterCount
        );

        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");

        VBox box = new VBox(
                10,
                filterBar,
                requestTable,
                buttons(refresh, add, edit, delete, status)
        );

        box.setPadding(new Insets(12));
        VBox.setVgrow(requestTable, Priority.ALWAYS);

        return box;
    }

    private void refreshRequests() {

        execute(
                api::getWorkRequests,
                list -> {
                    requests.setAll(list);

                    requestCount.setText(
                            "Заявки: " + requests.size()
                    );

                    updateFilterCounts();
                }
        );
    }

    private void requestDialog(WorkRequest existing) {

        Dialog<WorkRequest> dialog = new Dialog<>();
        styleDialog(dialog);

        dialog.setTitle(
                existing == null
                        ? "Додати заявку"
                        : "Редагувати заявку"
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField treeId = field(
                existing == null ? "" : str(existing.getTreeId())
        );

        TextArea description = new TextArea(
                existing == null ? "" : existing.getDescription()
        );

        description.setPrefRowCount(3);

        DatePicker plannedDate = new DatePicker(
                existing == null
                        ? null
                        : existing.getPlannedDate()
        );

        GridPane grid = form();

        addRow(grid, 0, "ID дерева:", treeId);
        addRow(grid, 1, "Опис:", description);
        addRow(grid, 2, "Дата планування:", plannedDate);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {

            if (button != ButtonType.OK) {
                return null;
            }

            try {

                WorkRequest request = new WorkRequest();

                if (existing != null) {
                    request.setId(existing.getId());
                }

                request.setTreeId(
                        Long.parseLong(treeId.getText().trim())
                );

                request.setDescription(
                        description.getText().trim()
                );

                request.setStatus(
                        existing == null
                                ? "СТВОРЕНО"
                                : existing.getStatus()
                );

                request.setPlannedDate(plannedDate.getValue());

                return request;

            } catch (Exception ex) {

                showError("Некоректні дані: " + ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(request -> {

            if (existing == null) {

                execute(
                        () -> api.createWorkRequest(request),
                        result -> refreshRequests()
                );

            } else {

                execute(
                        () -> api.updateWorkRequest(request),
                        result -> refreshRequests()
                );
            }
        });
    }

    // ================= STATUS LIFECYCLE =================

    private void changeStatus() {

        WorkRequest selected =
                requestTable.getSelectionModel()
                        .getSelectedItem();

        if (selected == null) {
            showInfo("Оберіть заявку");
            return;
        }

        String next = switch (selected.getStatus()) {

            case "СТВОРЕНО" -> "ПЕРЕВІРЕНО";
            case "ПЕРЕВІРЕНО" -> "ЗАПЛАНОВАНО";
            case "ЗАПЛАНОВАНО" -> "ВИКОНАНО";

            default -> null;
        };

        if (next == null) {
            showInfo("Заявка вже виконана");
            return;
        }

        if (!confirm(
                "Змінити статус з "
                        + selected.getStatus()
                        + " на " + next + "?"
        )) {
            return;
        }

        execute(
                () -> api.changeRequestStatus(
                        selected.getId(), next
                ),
                result -> refreshRequests()
        );
    }

    // ================= WORK ORDERS =================

    private VBox createOrderTab() {

        addColumn(orderTable, "ID", WorkOrder::getId);
        addColumn(orderTable, "ID заявки", WorkOrder::getRequestId);
        addColumn(orderTable, "Виконавець", WorkOrder::getAssignedTo);

        addColumn(orderTable, "Дата",
                order -> formatDate(
                        order.getOrderDate()
                ));

        addColumn(orderTable, "Примітки",
                WorkOrder::getCompletionNotes);

        Button refresh =
                styledButton("Оновити", "button-secondary");

        Button add =
                styledButton("Додати", "button-primary");

        Button edit =
                styledButton("Редагувати", "button-secondary");

        Button delete =
                styledButton("Видалити", "button-danger");

        refresh.setOnAction(e -> refreshOrders());

        add.setOnAction(e -> orderDialog(null));

        edit.setOnAction(e -> {

            WorkOrder selected =
                    orderTable.getSelectionModel()
                            .getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть наряд");
            } else {
                orderDialog(selected);
            }
        });

        delete.setOnAction(e -> {

            WorkOrder selected =
                    orderTable.getSelectionModel()
                            .getSelectedItem();

            if (selected == null) {
                showInfo("Оберіть наряд");
                return;
            }

            if (confirm(
                    "Видалити наряд ID "
                            + selected.getId() + "?"
            )) {

                execute(
                        () -> api.deleteWorkOrder(
                                selected.getId()
                        ),
                        result -> refreshOrders()
                );
            }
        });

        return layout(
                orderTable,
                buttons(refresh, add, edit, delete)
        );
    }

    private void refreshOrders() {

        execute(
                api::getWorkOrders,
                list -> {
                    orderTable.setItems(
                            FXCollections.observableArrayList(list)
                    );

                    orderCount.setText(
                            "Наряди: " + list.size()
                    );
                }
        );
    }

    private void orderDialog(WorkOrder existing) {

        Dialog<WorkOrder> dialog = new Dialog<>();
        styleDialog(dialog);

        dialog.setTitle(
                existing == null
                        ? "Додати наряд"
                        : "Редагувати наряд"
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField requestId = field(
                existing == null ? "" : str(existing.getRequestId())
        );

        TextField assignedTo = field(
                existing == null ? "" : existing.getAssignedTo()
        );

        DatePicker date = new DatePicker(
                existing == null
                        ? LocalDate.now().plusDays(1)
                        : existing.getOrderDate()
        );

        TextArea notes = new TextArea(
                existing == null
                        ? ""
                        : existing.getCompletionNotes()
        );

        notes.setPrefRowCount(3);

        GridPane grid = form();

        addRow(grid, 0, "ID заявки:", requestId);
        addRow(grid, 1, "Виконавець:", assignedTo);
        addRow(grid, 2, "Дата наряду:", date);
        addRow(grid, 3, "Примітки:", notes);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {

            if (button != ButtonType.OK) {
                return null;
            }

            try {

                WorkOrder order = new WorkOrder();

                if (existing != null) {
                    order.setId(existing.getId());
                }

                order.setRequestId(
                        Long.parseLong(requestId.getText().trim())
                );

                order.setAssignedTo(
                        assignedTo.getText().trim()
                );

                order.setOrderDate(date.getValue());
                order.setCompletionNotes(notes.getText());

                return order;

            } catch (Exception ex) {

                showError("Некоректні дані: " + ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(order -> {

            if (existing == null) {

                execute(
                        () -> api.createWorkOrder(order),
                        result -> refreshOrders()
                );

            } else {

                execute(
                        () -> api.updateWorkOrder(order),
                        result -> refreshOrders()
                );
            }
        });
    }

    // ================= DATE FORMATTING =================

    private String formatDate(LocalDate date) {

        return date == null
                ? ""
                : date.format(DATE_FORMAT);
    }

    private String formatDateTime(Object value) {

        if (value == null) {
            return "";
        }

        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME_FORMAT);
        }

        return value.toString();
    }

    // ================= FORM HELPERS =================

    private GridPane form() {

        GridPane grid = new GridPane();

        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        return grid;
    }

    private void addRow(
            GridPane grid,
            int row,
            String label,
            Control control) {

        grid.add(new Label(label), 0, row);
        grid.add(control, 1, row);

        if (control instanceof TextField field) {
            field.setPrefWidth(300);
        }
    }

    private TextField field(String value) {

        return new TextField(
                value == null ? "" : value
        );
    }

    private String str(Object value) {

        return value == null ? "" : value.toString();
    }

    private BigDecimal requiredDecimal(TextField field) {

        return new BigDecimal(
                field.getText().trim()
        );
    }

    private BigDecimal optionalDecimal(TextField field) {

        String text = field.getText().trim();

        return text.isEmpty()
                ? null
                : new BigDecimal(text);
    }

    // ================= APPLICATION STOP =================

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}