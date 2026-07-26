package com.finance.controller;

import com.finance.FinanceApplication;
import com.finance.context.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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
    private Button calculatorBtn;

    @FXML
    private Button adminBtn;

    @FXML
    private Button backupRestoreBtn;

    @FXML
    private Button logoutBtn;

    @FXML
    public void initialize() {
        dashboardBtn.setOnAction(e -> { loadView("/fxml/DashboardHome.fxml"); setActiveButton(dashboardBtn); });
        accountsBtn.setOnAction(e -> { loadView("/fxml/Accounts.fxml"); setActiveButton(accountsBtn); });
        transactionBtn.setOnAction(e -> { loadView("/fxml/Transaction.fxml"); setActiveButton(transactionBtn); });
        transferBtn.setOnAction(e -> { loadView("/fxml/Transfer.fxml"); setActiveButton(transferBtn); });
        reportsBtn.setOnAction(e -> { loadView("/fxml/Reports.fxml"); setActiveButton(reportsBtn); });
        calculatorBtn.setOnAction(e -> { loadView("/fxml/Calculator.fxml"); setActiveButton(calculatorBtn); });
        logoutBtn.setOnAction(e -> handleLogout());

        if (!sessionContext.isAdmin()) {
            adminBtn.setVisible(false);
            adminBtn.setManaged(false);
            backupRestoreBtn.setVisible(false);
            backupRestoreBtn.setManaged(false);
        } else {
            adminBtn.setOnAction(e -> { loadView("/fxml/AdminPanel.fxml"); setActiveButton(adminBtn); });
            backupRestoreBtn.setOnAction(e -> { loadView("/fxml/BackupRestore.fxml"); setActiveButton(backupRestoreBtn); });
        }

        loadView("/fxml/DashboardHome.fxml");
        setActiveButton(dashboardBtn);
    }

    private void setActiveButton(Button active) {
        for (Button b : new Button[]{dashboardBtn, accountsBtn, transactionBtn, transferBtn, reportsBtn, calculatorBtn, adminBtn, backupRestoreBtn}) {
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

    private void handleLogout() {
        sessionContext.logout();
        try {
            FinanceApplication.showLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}