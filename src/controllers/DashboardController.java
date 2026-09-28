// DashboardController.java - Enhanced with fixed sorting functionality
package controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Part;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.beans.InvalidationListener;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.Node;
import java.util.Optional;

public class DashboardController implements Initializable {

    // FXML Controls
    @FXML private Label userNameLabel, statusLabel, totalPartsLabel, inStockLabel, lowStockLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter, statusFilter;
    @FXML private ComboBox<String> sortByCombo; // Added sorting dropdown
    @FXML private Button gridViewBtn, listViewBtn, tableViewBtn;
    @FXML private Button addPartBtn, editPartBtn, deletePartBtn, signOutBtn;
    @FXML private FlowPane partsGrid;
    @FXML private ImageView partImage;
    @FXML private Label labelPartName, labelPartPrice, labelStock, labelAvailability, labelCategory, labelDescription;
    @FXML private Button viewDetailsBtn, duplicateBtn, printBtn;
    @FXML private Label lastUpdatedLabel, versionLabel;
    @FXML private ScrollPane partsScrollPane;

    // Data
    private final ObservableList<Part> allParts = FXCollections.observableArrayList();
    private final FilteredList<Part> filteredParts = new FilteredList<>(allParts);
    private final SortedList<Part> sortedParts = new SortedList<>(filteredParts);
    private Part selectedPart;
    private String currentView = "grid";
    private String currentSortBy = "name"; // Default sort

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupInitialData();
        setupFilters();
        setupSorting();
        setupViewButtons();
        setupActionButtons();
        setupEventHandlers();
        updateGrid();
        updateLastUpdated();

        // Set initial user info
        userNameLabel.setText("Welcome, Admin");
        statusLabel.setText("System Ready");
        versionLabel.setText("Version 2.0");
    }

    private void setupEventHandlers() {
        // Listen to changes in the filtered list (which affects sorted list)
        filteredParts.addListener((javafx.collections.ListChangeListener<Part>) change -> {
            updateGrid();
            updateStats();
        });

        // Also listen to changes in the source list
        allParts.addListener((javafx.collections.ListChangeListener<Part>) change -> {
            updateGrid();
            updateStats();
        });
    }

    private void setupInitialData() {
        // Sample data with more realistic parts
        allParts.addAll(List.of(
                new Part("001", "Engine Oil Filter", "Engine", 29.99, 25, "In Stock", "High-quality oil filter for diesel engines", "/images/oil-filter.png"),
                new Part("002", "Brake Pad Set", "Brakes", 89.99, 8, "Low Stock", "Premium ceramic brake pads", "/images/brake-pad.png"),
                new Part("003", "Air Filter", "Engine", 19.99, 15, "In Stock", "Heavy-duty air filter for trucks", "/images/air-filter.png"),
                new Part("004", "Spark Plug", "Engine", 12.99, 50, "In Stock", "Platinum spark plugs", "/images/spark-plug.png"),
                new Part("005", "Transmission Fluid", "Transmission", 45.99, 3, "Low Stock", "Synthetic transmission fluid", "/images/trans-fluid.png"),
                new Part("006", "Windshield Wipers", "Accessories", 24.99, 20, "In Stock", "All-weather windshield wipers", "/images/wipers.png"),
                new Part("007", "Headlight Bulb", "Electrical", 15.99, 12, "In Stock", "LED headlight bulbs", "/images/headlight.png"),
                new Part("008", "Fuel Pump", "Fuel System", 199.99, 0, "Out of Stock", "High-pressure fuel pump", "/images/fuel-pump.png"),
                new Part("009", "Battery", "Electrical", 129.99, 2, "Low Stock", "12V automotive battery", "/images/battery.png"),
                new Part("010", "Alternator", "Electrical", 189.99, 5, "Low Stock", "High-output alternator", "/images/alternator.png")
        ));
    }

    private void setupFilters() {
        // Get unique categories from parts
        Set<String> categories = allParts.stream()
                .map(Part::getCategory)
                .collect(Collectors.toSet());

        // Get unique statuses from parts
        Set<String> statuses = allParts.stream()
                .map(Part::getStatus)
                .collect(Collectors.toSet());

        // Populate filter dropdowns
        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(categories.stream().sorted().collect(Collectors.toList()));

        statusFilter.getItems().clear();
        statusFilter.getItems().add("All Status");
        statusFilter.getItems().addAll(statuses.stream().sorted().collect(Collectors.toList()));

        // Set default values
        categoryFilter.setValue("All Categories");
        statusFilter.setValue("All Status");

        // Setup search and filter functionality
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupSorting() {
        // Initialize sort combo if it exists in FXML
        if (sortByCombo != null) {
            sortByCombo.getItems().addAll(
                    "Name (A-Z)",
                    "Name (Z-A)",
                    "Price (Low to High)",
                    "Price (High to Low)",
                    "Stock (Low to High)",
                    "Stock (High to Low)",
                    "Category (A-Z)",
                    "Status (A-Z)"
            );
            sortByCombo.setValue("Name (A-Z)");
            sortByCombo.valueProperty().addListener((obs, oldVal, newVal) -> applySorting(newVal));
        }

        // Set default sorting
        applySorting("Name (A-Z)");
    }

    private void applySorting(String sortOption) {
        if (sortOption == null) return;

        Comparator<Part> comparator;

        switch (sortOption) {
            case "Name (A-Z)":
                comparator = Comparator.comparing(Part::getName);
                break;
            case "Name (Z-A)":
                comparator = Comparator.comparing(Part::getName).reversed();
                break;
            case "Price (Low to High)":
                comparator = Comparator.comparing(Part::getPrice);
                break;
            case "Price (High to Low)":
                comparator = Comparator.comparing(Part::getPrice).reversed();
                break;
            case "Stock (Low to High)":
                comparator = Comparator.comparing(Part::getStock);
                break;
            case "Stock (High to Low)":
                comparator = Comparator.comparing(Part::getStock).reversed();
                break;
            case "Category (A-Z)":
                comparator = Comparator.comparing(Part::getCategory)
                        .thenComparing(Part::getName);
                break;
            case "Status (A-Z)":
                comparator = getStatusComparator()
                        .thenComparing(Part::getName);
                break;
            default:
                comparator = Comparator.comparing(Part::getName);
        }

        sortedParts.setComparator(comparator);
        currentSortBy = sortOption;

        // Update status
        statusLabel.setText("Sorted by: " + sortOption);
    }

    private Comparator<Part> getStatusComparator() {
        // Custom comparator for status with priority: Out of Stock, Low Stock, In Stock
        Map<String, Integer> statusPriority = Map.of(
                "Out of Stock", 1,
                "Low Stock", 2,
                "In Stock", 3
        );

        return Comparator.comparing(part -> statusPriority.getOrDefault(part.getStatus(), 999));
    }

    private void applyFilters() {
        filteredParts.setPredicate(part -> {
            // Search filter
            String searchText = searchField.getText().toLowerCase().trim();
            boolean matchesSearch = searchText.isEmpty() ||
                    part.getName().toLowerCase().contains(searchText) ||
                    part.getId().toLowerCase().contains(searchText) ||
                    part.getCategory().toLowerCase().contains(searchText) ||
                    part.getDescription().toLowerCase().contains(searchText);

            // Category filter
            String categoryValue = categoryFilter.getValue();
            boolean matchesCategory = categoryValue == null ||
                    categoryValue.equals("All Categories") ||
                    part.getCategory().equals(categoryValue);

            // Status filter
            String statusValue = statusFilter.getValue();
            boolean matchesStatus = statusValue == null ||
                    statusValue.equals("All Status") ||
                    part.getStatus().equals(statusValue);

            return matchesSearch && matchesCategory && matchesStatus;
        });
    }

    // Add method to manually trigger sorting by specific criteria
    public void sortByCategory() {
        applySorting("Category (A-Z)");
        if (sortByCombo != null) {
            sortByCombo.setValue("Category (A-Z)");
        }
    }

    public void sortByStatus() {
        applySorting("Status (A-Z)");
        if (sortByCombo != null) {
            sortByCombo.setValue("Status (A-Z)");
        }
    }

    public void sortByName() {
        applySorting("Name (A-Z)");
        if (sortByCombo != null) {
            sortByCombo.setValue("Name (A-Z)");
        }
    }

    public void sortByPrice() {
        applySorting("Price (Low to High)");
        if (sortByCombo != null) {
            sortByCombo.setValue("Price (Low to High)");
        }
    }

    public void sortByStock() {
        applySorting("Stock (Low to High)");
        if (sortByCombo != null) {
            sortByCombo.setValue("Stock (Low to High)");
        }
    }

    // Method to refresh filters and sorting
    public void refreshFiltersAndSorting() {
        setupFilters();
        applySorting(currentSortBy);
        applyFilters();
    }

    private void switchView(String viewType) {
        currentView = viewType;
        updateViewButtonStyles();
        updateGrid();

        statusLabel.setText("View: " + viewType.substring(0, 1).toUpperCase() + viewType.substring(1));
    }

    private void updateViewButtonStyles() {
        // Reset all buttons
        gridViewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-font-size: 12px;");
        listViewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-font-size: 12px;");
        tableViewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-font-size: 12px;");

        // Highlight active button
        String activeStyle = "-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: #333; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-font-size: 12px;";
        switch (currentView) {
            case "grid":
                gridViewBtn.setStyle(activeStyle);
                break;
            case "list":
                listViewBtn.setStyle(activeStyle);
                break;
            case "table":
                tableViewBtn.setStyle(activeStyle);
                break;
        }
    }

    private void setupViewButtons() {
        gridViewBtn.setOnAction(e -> switchView("grid"));
        listViewBtn.setOnAction(e -> switchView("list"));
        tableViewBtn.setOnAction(e -> switchView("table"));

        // Set initial view button states
        updateViewButtonStyles();
    }

    private void setupActionButtons() {
        addPartBtn.setOnAction(e -> addNewPart());
        editPartBtn.setOnAction(e -> editSelectedPart());
        deletePartBtn.setOnAction(e -> deleteSelectedPart());
        signOutBtn.setOnAction(e -> signOut());

        viewDetailsBtn.setOnAction(e -> viewPartDetails());
        duplicateBtn.setOnAction(e -> duplicatePart());
        printBtn.setOnAction(e -> printPart());

        // Disable edit/delete buttons initially
        editPartBtn.setDisable(true);
        deletePartBtn.setDisable(true);
        viewDetailsBtn.setDisable(true);
        duplicateBtn.setDisable(true);
        printBtn.setDisable(true);
    }

    private void updateGrid() {
        partsGrid.getChildren().clear();

        // Use sorted parts instead of filtered parts
        for (Part part : sortedParts) {
            VBox card = createPartCard(part);
            partsGrid.getChildren().add(card);
        }

        updateStats();
    }

    private VBox createPartCard(Part part) {
        VBox card = new VBox();
        card.setSpacing(0);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
                "-fx-border-color: #e9ecef; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 12; " +
                "-fx-cursor: hand;");

        // Card Header with Image
        VBox header = new VBox();
        header.setAlignment(Pos.CENTER);
        header.setSpacing(8);
        header.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12 12 0 0; -fx-padding: 15;");

        ImageView image = new ImageView();
        try {
            // Try to load image from resources
            image.setImage(new Image(getClass().getResourceAsStream(part.getImagePath())));
        } catch (Exception e) {
            // Fallback to placeholder
            image.setImage(new Image("https://via.placeholder.com/80x80/cccccc/666666?text=Part"));
        }
        image.setFitWidth(80);
        image.setFitHeight(80);
        image.setPreserveRatio(true);

        // Status Badge
        Label statusBadge = new Label(getStatusEmoji(part.getStatus()) + " " + part.getStatus());
        statusBadge.setStyle(getStatusBadgeStyle(part.getStatus()));

        header.getChildren().addAll(image, statusBadge);

        // Card Content
        VBox content = new VBox();
        content.setSpacing(12);
        content.setStyle("-fx-padding: 15;");

        // Part Name
        Label nameLabel = new Label(part.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        // Details Grid
        HBox detailsGrid = new HBox();
        detailsGrid.setSpacing(10);
        detailsGrid.setAlignment(Pos.CENTER_LEFT);

        VBox categoryBox = new VBox();
        categoryBox.setSpacing(4);
        Label categoryHeader = new Label("CATEGORY");
        categoryHeader.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        Label categoryValue = new Label(part.getCategory());
        categoryValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50;");
        categoryBox.getChildren().addAll(categoryHeader, categoryValue);

        VBox stockBox = new VBox();
        stockBox.setSpacing(4);
        Label stockHeader = new Label("STOCK");
        stockHeader.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        Label stockValue = new Label(String.valueOf(part.getStock()));
        stockValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50;");
        stockBox.getChildren().addAll(stockHeader, stockValue);

        VBox priceBox = new VBox();
        priceBox.setSpacing(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label priceHeader = new Label("PRICE");
        priceHeader.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        Label priceValue = new Label(String.format("$%.2f", part.getPrice()));
        priceValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        priceBox.getChildren().addAll(priceHeader, priceValue);

        detailsGrid.getChildren().addAll(categoryBox, stockBox, priceBox);

        // Stock Level Bar
        VBox stockLevelBox = new VBox();
        stockLevelBox.setSpacing(4);

        HBox stockLevelHeader = new HBox();
        stockLevelHeader.setSpacing(8);
        stockLevelHeader.setAlignment(Pos.CENTER_LEFT);
        Label stockLevelLabel = new Label("Stock Level");
        stockLevelLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        Label stockLevelStatus = new Label(getStockLevelText(part));
        stockLevelStatus.setStyle("-fx-font-size: 9px; -fx-text-fill: " + getStockLevelColor(part) + "; -fx-font-weight: bold;");
        stockLevelHeader.getChildren().addAll(stockLevelLabel, stockLevelStatus);

        ProgressBar stockProgress = new ProgressBar(part.getStockPercentage());
        stockProgress.setPrefWidth(250);
        stockProgress.setStyle("-fx-accent: " + getStockLevelColor(part) + "; -fx-background-color: #ecf0f1;");

        stockLevelBox.getChildren().addAll(stockLevelHeader, stockProgress);

        // Action Buttons
        HBox actionButtons = new HBox();
        actionButtons.setSpacing(6);
        actionButtons.setAlignment(Pos.CENTER);

        Button viewBtn = new Button("👁️ View");
        viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-font-size: 10px;");
        viewBtn.setOnAction(e -> selectPart(part));

        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-font-size: 10px;");
        editBtn.setOnAction(e -> editPart(part));

        Button copyBtn = new Button("📋 Copy");
        copyBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12 6 12; -fx-font-size: 10px;");
        copyBtn.setOnAction(e -> copyPart(part));

        actionButtons.getChildren().addAll(viewBtn, editBtn, copyBtn);

        content.getChildren().addAll(nameLabel, detailsGrid, stockLevelBox, actionButtons);

        card.getChildren().addAll(header, content);

        // Click handler for card selection
        card.setOnMouseClicked(e -> selectPart(part));

        return card;
    }

    private String getStatusEmoji(String status) {
        switch (status) {
            case "In Stock": return "🟢";
            case "Low Stock": return "🟡";
            case "Out of Stock": return "🔴";
            default: return "⚪";
        }
    }

    private String getStatusBadgeStyle(String status) {
        String baseStyle = "-fx-background-radius: 12; -fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: bold;";
        switch (status) {
            case "In Stock":
                return baseStyle + " -fx-background-color: #d4edda; -fx-text-fill: #155724;";
            case "Low Stock":
                return baseStyle + " -fx-background-color: #fff3cd; -fx-text-fill: #856404;";
            case "Out of Stock":
                return baseStyle + " -fx-background-color: #f8d7da; -fx-text-fill: #721c24;";
            default:
                return baseStyle + " -fx-background-color: #e2e3e5; -fx-text-fill: #6c757d;";
        }
    }

    private String getStockLevelText(Part part) {
        if (part.isOutOfStock()) return "Out";
        if (part.isLowStock()) return "Low";
        return "Good";
    }

    private String getStockLevelColor(Part part) {
        if (part.isOutOfStock()) return "#e74c3c";
        if (part.isLowStock()) return "#f39c12";
        return "#27ae60";
    }

    private void selectPart(Part part) {
        selectedPart = part;
        showDetails(part);

        // Enable action buttons
        editPartBtn.setDisable(false);
        deletePartBtn.setDisable(false);
        viewDetailsBtn.setDisable(false);
        duplicateBtn.setDisable(false);
        printBtn.setDisable(false);
    }

    private void showDetails(Part part) {
        labelPartName.setText(part.getName());
        labelPartPrice.setText(String.format("$%.2f", part.getPrice()));
        labelStock.setText(String.valueOf(part.getStock()));
        labelAvailability.setText(part.getStatus());
        labelCategory.setText(part.getCategory());
        labelDescription.setText(part.getDescription());

        // Update part image
        try {
            partImage.setImage(new Image(getClass().getResourceAsStream(part.getImagePath())));
        } catch (Exception e) {
            partImage.setImage(new Image("https://via.placeholder.com/200x200/cccccc/666666?text=Part"));
        }
    }

    private void updateStats() {
        // Use sortedParts for stats calculation
        totalPartsLabel.setText(String.valueOf(sortedParts.size()));

        long inStockCount = sortedParts.stream()
                .filter(p -> p.getStatus().equals("In Stock"))
                .count();

        long lowStockCount = sortedParts.stream()
                .filter(p -> p.getStatus().equals("Low Stock"))
                .count();

        inStockLabel.setText(String.valueOf(inStockCount));
        lowStockLabel.setText(String.valueOf(lowStockCount));
    }

    private void updateLastUpdated() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        lastUpdatedLabel.setText("Last updated: " + LocalDateTime.now().format(formatter));
    }

    private void addNewPart() {
        // Create a custom dialog for adding new parts
        Dialog<Part> dialog = new Dialog<>();
        dialog.setTitle("Add New Part");
        dialog.setHeaderText("Enter details for the new part");

        // Set the button types
        ButtonType addButtonType = new ButtonType("Add Part", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create the form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setPromptText("Part Name");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Engine", "Brakes", "Transmission", "Electrical",
                "Fuel System", "Accessories", "Suspension", "Cooling");
        categoryCombo.setPromptText("Select Category");

        TextField priceField = new TextField();
        priceField.setPromptText("0.00");

        TextField stockField = new TextField();
        stockField.setPromptText("0");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("In Stock", "Low Stock", "Out of Stock");
        statusCombo.setValue("In Stock");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Part description...");
        descriptionArea.setPrefRowCount(3);

        TextField imagePathField = new TextField();
        imagePathField.setPromptText("/images/part.png");
        imagePathField.setText("/images/default-part.png");

        Button browseImageBtn = new Button("Browse...");
        browseImageBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Part Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
            if (selectedFile != null) {
                imagePathField.setText(selectedFile.getPath());
            }
        });

        // Add validation for numeric fields
        priceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                priceField.setText(oldValue);
            }
        });

        stockField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                stockField.setText(oldValue);
            }
        });

        // Layout the form
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryCombo, 1, 1);

        grid.add(new Label("Price:"), 0, 2);
        HBox priceBox = new HBox(5);
        priceBox.getChildren().addAll(new Label("$"), priceField);
        grid.add(priceBox, 1, 2);

        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);

        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusCombo, 1, 4);

        grid.add(new Label("Description:"), 0, 5);
        grid.add(descriptionArea, 1, 5);

        grid.add(new Label("Image:"), 0, 6);
        HBox imageBox = new HBox(5);
        imageBox.getChildren().addAll(imagePathField, browseImageBtn);
        grid.add(imageBox, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable add button based on required fields
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);

        // Validation listener
        InvalidationListener validationListener = observable -> {
            boolean isValid = !nameField.getText().trim().isEmpty() &&
                    categoryCombo.getValue() != null &&
                    !priceField.getText().trim().isEmpty() &&
                    !stockField.getText().trim().isEmpty();
            addButton.setDisable(!isValid);
        };

        nameField.textProperty().addListener(validationListener);
        categoryCombo.valueProperty().addListener(validationListener);
        priceField.textProperty().addListener(validationListener);
        stockField.textProperty().addListener(validationListener);

        // Convert the result when add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText().trim();
                    String category = categoryCombo.getValue();
                    double price = Double.parseDouble(priceField.getText().trim());
                    int stock = Integer.parseInt(stockField.getText().trim());
                    String status = statusCombo.getValue();
                    String description = descriptionArea.getText().trim();
                    String imagePath = imagePathField.getText().trim();

                    // Auto-determine status based on stock if not manually set
                    if (stock == 0) {
                        status = "Out of Stock";
                    } else if (stock <= 10) {
                        status = "Low Stock";
                    } else {
                        status = "In Stock";
                    }

                    return new Part(
                            generateNewId(),
                            name,
                            category,
                            price,
                            stock,
                            status,
                            description.isEmpty() ? "No description available" : description,
                            imagePath
                    );
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter valid numeric values for price and stock.", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        // Show dialog and process result
        Optional<Part> result = dialog.showAndWait();

        result.ifPresent(part -> {
            // Add the new part to the list
            allParts.add(part);

            // Update the display
            updateLastUpdated();
            refreshFiltersAndSorting();

            // Show success message
            showAlert("Success", "Part '" + part.getName() + "' added successfully!", Alert.AlertType.INFORMATION);

            // Optionally select the new part
            selectPart(part);

            // Update status
            statusLabel.setText("New part added: " + part.getName());
        });
    }

    private void editSelectedPart() {
        if (selectedPart != null) {
            editPart(selectedPart);
        }
    }

    private void editPart(Part part) {
        showAlert("Edit Part", "Edit part functionality for '" + part.getName() + "' would be implemented here.", Alert.AlertType.INFORMATION);
        // After editing, refresh filters and sorting
        refreshFiltersAndSorting();
    }

    private void deleteSelectedPart() {
        if (selectedPart != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Part");
            alert.setHeaderText("Are you sure you want to delete this part?");
            alert.setContentText("Part: " + selectedPart.getName());

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    allParts.remove(selectedPart);
                    selectedPart = null;
                    clearDetails();
                    updateLastUpdated();
                    refreshFiltersAndSorting();
                    showAlert("Success", "Part deleted successfully.", Alert.AlertType.INFORMATION);
                }
            });
        }
    }

    private void copyPart(Part part) {
        Part newPart = new Part(
                generateNewId(),
                part.getName() + " (Copy)",
                part.getCategory(),
                part.getPrice(),
                part.getStock(),
                part.getStatus(),
                part.getDescription(),
                part.getImagePath()
        );

        allParts.add(newPart);
        updateLastUpdated();
        refreshFiltersAndSorting();
        showAlert("Success", "Part copied successfully.", Alert.AlertType.INFORMATION);
    }

    private void duplicatePart() {
        if (selectedPart != null) {
            copyPart(selectedPart);
        }
    }

    private void viewPartDetails() {
        if (selectedPart != null) {
            showAlert("Part Details", "Full details view for '" + selectedPart.getName() + "' would be implemented here.", Alert.AlertType.INFORMATION);
        }
    }

    private void printPart() {
        if (selectedPart != null) {
            showAlert("Print Part", "Print functionality for '" + selectedPart.getName() + "' would be implemented here.", Alert.AlertType.INFORMATION);
        }
    }

    private void signOut() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out");
        alert.setHeaderText("Are you sure you want to sign out?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Close the application or return to login screen
                Platform.exit();
            }
        });
    }

    private void clearDetails() {
        labelPartName.setText("Select a part to view details");
        labelPartPrice.setText("$0.00");
        labelStock.setText("0");
        labelAvailability.setText("N/A");
        labelCategory.setText("N/A");
        labelDescription.setText("No description available");
        partImage.setImage(new Image("https://via.placeholder.com/200x200/cccccc/666666?text=No+Part"));

        // Disable action buttons
        editPartBtn.setDisable(true);
        deletePartBtn.setDisable(true);
        viewDetailsBtn.setDisable(true);
        duplicateBtn.setDisable(true);
        printBtn.setDisable(true);
    }

    private String generateNewId() {
        int maxId = allParts.stream()
                .mapToInt(p -> Integer.parseInt(p.getId()))
                .max()
                .orElse(0);
        return String.format("%03d", maxId + 1);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}