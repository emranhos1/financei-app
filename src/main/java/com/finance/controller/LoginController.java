package com.finance.controller;

import com.finance.FinanceApplication;
import com.finance.context.SessionContext;
import com.finance.entity.User;
import com.finance.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LoginController {
    private final UserService userService;
    private final SessionContext sessionContext;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Please enter username and password");
            return;
        }

        Optional<User> user = userService.authenticateUser(username, password);
        if (user.isPresent()) {
            sessionContext.login(user.get());
            try {
                FinanceApplication.showDashboard();
            } catch (Exception e) {
                showAlert("Error", "Failed to load dashboard");
            }
        } else {
            showAlert("Authentication Failed", "Invalid username or password, or account is inactive");
            passwordField.clear();
        }
    }

    @FXML
    public void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
            loader.setControllerFactory(FinanceApplication.getApplicationContext()::getBean);
            Parent root = loader.load();

            Stage registerStage = new Stage();
            registerStage.setTitle("Register New Account");
            registerStage.setScene(new Scene(root, 400, 300));
            registerStage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to open registration window");
        }
    }

    @FXML
    public void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ForgotPassword.fxml"));
            loader.setControllerFactory(FinanceApplication.getApplicationContext()::getBean);
            Parent root = loader.load();

            Stage forgotPasswordStage = new Stage();
            forgotPasswordStage.setTitle("Reset Password");
            forgotPasswordStage.setScene(new Scene(root, 400, 420));
            forgotPasswordStage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to open reset password window");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}