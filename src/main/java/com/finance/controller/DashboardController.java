package com.finance.controller;

import com.finance.FinanceApplication;
import com.finance.context.SessionContext;
import com.finance.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final SessionContext sessionContext;
    private final UserService userService;

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private VBox sidebarVBox;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button accountsBtn;

    @FXML
    private Button transactionBtn;

    @FXML
    private Button reportsBtn;

    @FXML
    private Button calculatorBtn;

    @FXML
    private Button adminBtn;

    @FXML
    private Button changePasswordBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    public void initialize() {
        dashboardBtn.setOnAction(e -> { loadView("/fxml/DashboardHome.fxml"); setActiveButton(dashboardBtn); });
        accountsBtn.setOnAction(e -> { loadView("/fxml/Accounts.fxml"); setActiveButton(accountsBtn); });
        transactionBtn.setOnAction(e -> { loadView("/fxml/Transactions.fxml"); setActiveButton(transactionBtn); });
        reportsBtn.setOnAction(e -> { loadView("/fxml/Reports.fxml"); setActiveButton(reportsBtn); });
        calculatorBtn.setOnAction(e -> { loadView("/fxml/Calculator.fxml"); setActiveButton(calculatorBtn); });
        changePasswordBtn.setOnAction(e -> handleChangePassword());
        logoutBtn.setOnAction(e -> handleLogout());

        if (!sessionContext.isAdmin()) {
            adminBtn.setVisible(false);
            adminBtn.setManaged(false);
        } else {
            adminBtn.setOnAction(e -> { loadView("/fxml/AdminPanel.fxml"); setActiveButton(adminBtn); });
        }

        loadView("/fxml/DashboardHome.fxml");
        setActiveButton(dashboardBtn);
    }

    /** Called from the Dashboard's loan reminder toast so clicking it jumps straight to the
     *  Loans sub-tab (index 2 of Transaction/Transfer/Loans) rather than just the Transaction page. */
    public void goToLoansTab() {
        loadView("/fxml/Transactions.fxml");
        setActiveButton(transactionBtn);
        TabPane tabPane = (TabPane) mainBorderPane.getCenter().lookup("#mainTabPane");
        if (tabPane != null) tabPane.getSelectionModel().select(2);
    }

    private void setActiveButton(Button active) {
        for (Button b : new Button[]{dashboardBtn, accountsBtn, transactionBtn, reportsBtn, calculatorBtn, adminBtn}) {
            b.getStyleClass().remove("sidebar-btn-active");
        }
        active.getStyleClass().add("sidebar-btn-active");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(FinanceApplication.getApplicationContext()::getBean);
            Parent view = loader.load();
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleChangePassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Change your password");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Current password");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(oldPasswordField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String newPass = newPasswordField.getText();
            if (newPass == null || newPass.isEmpty()) {
                showAlert("Validation Error", "New password cannot be empty"); return;
            }
            if (!newPass.equals(confirmPasswordField.getText())) {
                showAlert("Validation Error", "New password and confirmation do not match"); return;
            }
            try {
                userService.changePassword(sessionContext.getCurrentUserId(), oldPasswordField.getText(), newPass);
                showAlert("Success", "Password changed successfully");
            } catch (Exception e) {
                showAlert("Error", e.getMessage());
            }
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void handleLogout() {
        sessionContext.logout();
        try {
            FinanceApplication.showLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}