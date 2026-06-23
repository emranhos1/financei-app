package com.finance.controller;

import com.finance.entity.User;
import com.finance.service.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AdminPanelController {
    private final UserService userService;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Long> idColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, User.UserRole> roleColumn;

    @FXML
    private TableColumn<User, User.UserStatus> statusColumn;

    @FXML
    public void initialize() {
        setupTable();
        loadUsers();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadUsers() {
        List<User> users = userService.getAllUsers();
        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    @FXML
    public void handleActivateUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a user");
            return;
        }

        try {
            userService.updateUserStatus(selected.getId(), User.UserStatus.active);
            loadUsers();
            showAlert("Success", "User activated successfully");
        } catch (Exception e) {
            showAlert("Error", "Failed to activate user: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeactivateUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a user");
            return;
        }

        try {
            userService.updateUserStatus(selected.getId(), User.UserStatus.inactive);
            loadUsers();
            showAlert("Success", "User deactivated successfully");
        } catch (Exception e) {
            showAlert("Error", "Failed to deactivate user: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
