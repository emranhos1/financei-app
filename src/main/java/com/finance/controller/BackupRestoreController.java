package com.finance.controller;

import com.finance.service.IBackupService;
import com.finance.service.IGoogleDriveService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.File;

@Controller
@RequiredArgsConstructor
public class BackupRestoreController {
    private final IBackupService backupService;
    private final IGoogleDriveService googleDriveService;

    @FXML private Button exportLocalBtn;
    @FXML private Button importLocalBtn;
    @FXML private Label localStatusLabel;
    @FXML private ProgressIndicator localProgress;

    @FXML private Button connectDriveBtn;
    @FXML private Button uploadDriveBtn;
    @FXML private Button downloadDriveBtn;
    @FXML private Label driveStatusLabel;
    @FXML private ProgressIndicator driveProgress;

    private interface BackgroundAction {
        String run() throws Exception;
    }

    @FXML
    public void initialize() {
        localProgress.setVisible(false);
        driveProgress.setVisible(false);
        refreshDriveButtons();
    }

    @FXML
    public void handleExportLocal() {
        Window owner = exportLocalBtn.getScene().getWindow();
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a folder to save the backup");
        File folder = chooser.showDialog(owner);
        if (folder == null) return;

        runInBackground(localProgress, localStatusLabel, new Button[]{exportLocalBtn, importLocalBtn},
                () -> "Backup saved to " + backupService.exportToLocal(folder));
    }

    @FXML
    public void handleImportLocal() {
        Window owner = importLocalBtn.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a .sql backup file to import");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        if (!confirm("This will overwrite your current data with the selected backup file. Continue?")) return;

        runInBackground(localProgress, localStatusLabel, new Button[]{exportLocalBtn, importLocalBtn},
                () -> backupService.importFromLocal(file));
    }

    @FXML
    public void handleConnectDrive() {
        runInBackground(driveProgress, driveStatusLabel,
                new Button[]{connectDriveBtn, uploadDriveBtn, downloadDriveBtn},
                () -> { googleDriveService.connect(); return "Connected to Google Drive"; });
    }

    @FXML
    public void handleUploadDrive() {
        runInBackground(driveProgress, driveStatusLabel,
                new Button[]{connectDriveBtn, uploadDriveBtn, downloadDriveBtn},
                () -> {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"));
                    String localPath = backupService.exportToLocal(tempDir);
                    return googleDriveService.upload(new File(localPath));
                });
    }

    @FXML
    public void handleDownloadDrive() {
        if (!confirm("This will overwrite your current data with the latest backup from Google Drive. Continue?")) return;

        runInBackground(driveProgress, driveStatusLabel,
                new Button[]{connectDriveBtn, uploadDriveBtn, downloadDriveBtn},
                () -> {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"));
                    File downloaded = googleDriveService.downloadLatest(tempDir);
                    return backupService.importFromLocal(downloaded);
                });
    }

    private void runInBackground(ProgressIndicator progress, Label statusLabel, Button[] buttons, BackgroundAction action) {
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll("backup-status-success", "backup-status-error");
        progress.setVisible(true);
        for (Button b : buttons) b.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return action.run();
            }
        };

        task.setOnSucceeded(e -> {
            progress.setVisible(false);
            for (Button b : buttons) b.setDisable(false);
            refreshDriveButtons();
            statusLabel.setText(task.getValue());
            statusLabel.getStyleClass().add("backup-status-success");
        });

        task.setOnFailed(e -> {
            progress.setVisible(false);
            for (Button b : buttons) b.setDisable(false);
            refreshDriveButtons();
            Throwable ex = task.getException();
            statusLabel.setText(ex != null && ex.getMessage() != null ? ex.getMessage() : "Operation failed");
            statusLabel.getStyleClass().add("backup-status-error");
        });

        Thread thread = new Thread(task, "backup-restore-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshDriveButtons() {
        boolean connected = googleDriveService.isConnected();
        connectDriveBtn.setDisable(connected);
        uploadDriveBtn.setDisable(!connected);
        downloadDriveBtn.setDisable(!connected);
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message);
        alert.setTitle("Confirm");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
