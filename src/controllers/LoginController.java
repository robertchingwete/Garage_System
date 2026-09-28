package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.application.Platform;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Label statusLabel;

    @FXML
    private Button loginButton;

    @FXML
    private StackPane loadingPane;

    @FXML
    private void initialize() {
        // Clear any previous status messages when user starts typing
        usernameField.textProperty().addListener((obs, oldText, newText) -> hideStatus());
        passwordField.textProperty().addListener((obs, oldText, newText) -> hideStatus());

        // Load remembered credentials if available
        loadRememberedCredentials();
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Input validation
        if (username.isEmpty()) {
            showStatus("Please enter your username", true);
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showStatus("Please enter your password", true);
            passwordField.requestFocus();
            return;
        }

        // Show loading and disable form
        showLoading(true);
        hideStatus();

        // Simulate authentication process with a background task
        Task<Boolean> loginTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Simulate network delay
                Thread.sleep(1000);

                // Authentication logic
                return authenticateUser(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showLoading(false);

                if (loginTask.getValue()) {
                    handleSuccessfulLogin(username);
                } else {
                    showStatus("Invalid username or password. Please try again.", true);
                    passwordField.clear();
                    passwordField.requestFocus();
                }
            });
        });

        loginTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoading(false);
                showStatus("Login failed. Please try again.", true);
            });
        });

        new Thread(loginTask).start();
    }

    @FXML
    public void handleRegister() {
        // For now, show a coming soon message
        showAlert("Registration", "Registration feature coming soon!\n\nFor now, you can use:\nUsername: admin\nPassword: 1234", Alert.AlertType.INFORMATION);
    }

    @FXML
    public void handleForgotPassword() {
        // Create a simple dialog for password reset
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Password Reset");
        dialog.setHeaderText("Reset Your Password");
        dialog.setContentText("Enter your username or email:");

        dialog.showAndWait().ifPresent(input -> {
            if (!input.trim().isEmpty()) {
                showAlert("Password Reset",
                        "Password reset instructions have been sent to your email address.\n\n" +
                                "If you don't receive an email within a few minutes, please check your spam folder.",
                        Alert.AlertType.INFORMATION);
            }
        });
    }

    @FXML
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    private boolean authenticateUser(String username, String password) {
        // Simple authentication - in a real app, this would connect to a database
        // Add more users as needed
        return (username.equals("admin") && password.equals("1234")) ||
                (username.equals("user") && password.equals("password")) ||
                (username.equals("demo") && password.equals("demo"));
    }

    private void handleSuccessfulLogin(String username) {
        // Save credentials if remember me is checked
        if (rememberMeCheckBox.isSelected()) {
            saveCredentials(username);
        } else {
            clearSavedCredentials();
        }

        showStatus("Login successful! Opening dashboard...", false);

        // Navigate to dashboard
        Platform.runLater(() -> {
            try {
                // Load dashboard FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/Dashboard.fxml"));
                Parent dashboardRoot = loader.load();

                // Get dashboard controller and set current user
                DashboardController dashboardController = loader.getController();
                //dashboardController.setCurrentUser(username);

                // Get current stage
                Stage stage = (Stage) loginButton.getScene().getWindow();

                // Create new scene and set it
                Scene dashboardScene = new Scene(dashboardRoot);
                stage.setScene(dashboardScene);
                stage.setTitle("Garage Management System - Dashboard");
                stage.setMaximized(true); // Maximize for better dashboard view
                stage.centerOnScreen();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Could not load dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ?
                "-fx-text-fill: #d32f2f; -fx-font-size: 12px;" :
                "-fx-text-fill: #2e7d32; -fx-font-size: 12px;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showLoading(boolean show) {
        loadingPane.setVisible(show);
        loadingPane.setManaged(show);
        loginButton.setDisable(show);
        usernameField.setDisable(show);
        passwordField.setDisable(show);
    }

    private void saveCredentials(String username) {
        // In a real application, you would save this securely
        // For now, we'll use Java preferences (basic storage)
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(LoginController.class);
        prefs.put("remembered_username", username);
        prefs.putBoolean("remember_me", true);
    }

    private void loadRememberedCredentials() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(LoginController.class);
        boolean rememberMe = prefs.getBoolean("remember_me", false);

        if (rememberMe) {
            String username = prefs.get("remembered_username", "");
            if (!username.isEmpty()) {
                usernameField.setText(username);
                rememberMeCheckBox.setSelected(true);
                passwordField.requestFocus(); // Focus on password field since username is filled
            }
        }
    }

    private void clearSavedCredentials() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(LoginController.class);
        prefs.remove("remembered_username");
        prefs.putBoolean("remember_me", false);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Convenience method for backward compatibility
    private void showAlert(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }
}