package com.finance.controller;

import com.finance.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    public void handleResetPassword() {
        hideMessage();

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (username.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Please fill in all fields.");
            return;
        }
        if (newPassword.length() < 4) {
            showMessage("New password must be at least 4 characters.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showMessage("Passwords do not match.");
            return;
        }
        if (!userService.userExists(username)) {
            showMessage("No account found with that username.");
            return;
        }

        try {
            userService.resetPassword(username, newPassword);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Password Reset");
            alert.setHeaderText(null);
            alert.setContentText("Your password has been reset. You can now log in with your new password.");
            alert.showAndWait();
            closeWindow();
        } catch (Exception e) {
            showMessage("Failed to reset password: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) usernameField.getScene().getWindow()).close();
    }

    private void showMessage(String text) {
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
