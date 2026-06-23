package com.finance.controller;

import com.finance.context.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final SessionContext sessionContext;

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
    private Button transferBtn;

    @FXML
    private Button reportsBtn;

    @FXML
    private Button adminBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    public void initialize() {
        // Set up button handlers
        dashboardBtn.setOnAction(e -> loadView("/fxml/DashboardHome.fxml"));
        accountsBtn.setOnAction(e -> loadView("/fxml/Accounts.fxml"));
        transactionBtn.setOnAction(e -> loadView("/fxml/Transaction.fxml"));
        transferBtn.setOnAction(e -> loadView("/fxml/Transfer.fxml"));
        reportsBtn.setOnAction(e -> loadView("/fxml/Reports.fxml"));
        logoutBtn.setOnAction(e -> handleLogout());

        // Show admin panel only for admin users
        if (!sessionContext.isAdmin()) {
            adminBtn.setVisible(false);
            adminBtn.setManaged(false);
        } else {
            adminBtn.setOnAction(e -> loadView("/fxml/AdminPanel.fxml"));
        }

        // Load dashboard home on startup
        loadView("/fxml/DashboardHome.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(com.finance.FinanceApplication.getApplicationContext()::getBean);
            Parent view = loader.load();
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleLogout() {
        sessionContext.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            loader.setControllerFactory(com.finance.FinanceApplication.getApplicationContext()::getBean);
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Daily Finance Management System");
            loginStage.setScene(new javafx.scene.Scene(root, 500, 400));
            loginStage.show();

            // Close dashboard
            Stage dashboardStage = (Stage) mainBorderPane.getScene().getWindow();
            dashboardStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
