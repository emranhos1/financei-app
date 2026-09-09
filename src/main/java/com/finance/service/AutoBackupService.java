package com.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/** Runs a Google Drive backup automatically on app startup if enabled and enough time has passed
 *  since the last run. Only ever runs silently (no browser popup) - if no Google account was ever
 *  connected on this machine, it skips and waits for the user to connect once from Admin Panel.
 *  <p>
 *  The enabled/interval/last-run settings live in a local properties file on THIS machine only,
 *  never in the database: this database gets backed up and restored across machines, so a
 *  DB-stored setting would travel with a restore and show as "enabled" on a PC where the user
 *  never checked the box - exactly the confusion this avoids. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoBackupService {
    private static final String SETTINGS_FILE = "auto_backup.properties";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_INTERVAL_DAYS = "intervalDays";
    private static final String KEY_LAST_RUN = "lastRun";

    private final IBackupService backupService;
    private final IGoogleDriveService googleDriveService;

    public void runIfDue() {
        Properties settings = loadSettings();
        boolean enabled = Boolean.parseBoolean(settings.getProperty(KEY_ENABLED, "false"));
        if (!enabled) return;

        if (!googleDriveService.hasStoredCredentials()) {
            log.info("Auto backup is enabled on this PC but Google Drive has never been connected here - skipping until connected once from Admin Panel");
            return;
        }

        int intervalDays = Integer.parseInt(settings.getProperty(KEY_INTERVAL_DAYS, "1"));
        String lastRunStr = settings.getProperty(KEY_LAST_RUN);
        LocalDateTime now = LocalDateTime.now();
        if (lastRunStr != null && !LocalDateTime.parse(lastRunStr, TIMESTAMP_FORMAT).plusDays(intervalDays).isBefore(now)) {
            return;
        }

        try {
            if (!googleDriveService.isConnected()) {
                googleDriveService.connect();
            }
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            String localPath = backupService.exportToLocal(tempDir);
            googleDriveService.upload(new File(localPath));
            settings.setProperty(KEY_LAST_RUN, now.format(TIMESTAMP_FORMAT));
            saveSettings(settings);
            log.info("Automatic backup completed and uploaded to Google Drive");
        } catch (Exception e) {
            log.error("Automatic backup failed", e);
        }
    }

    public String getLastRunDisplay() {
        String lastRunStr = loadSettings().getProperty(KEY_LAST_RUN);
        return lastRunStr == null ? "Never" : lastRunStr.replace("T", " ");
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(loadSettings().getProperty(KEY_ENABLED, "false"));
    }

    public int getIntervalDays() {
        return Integer.parseInt(loadSettings().getProperty(KEY_INTERVAL_DAYS, "1"));
    }

    public void setEnabled(boolean enabled) {
        Properties settings = loadSettings();
        settings.setProperty(KEY_ENABLED, Boolean.toString(enabled));
        saveSettings(settings);
    }

    public void setIntervalDays(int days) {
        Properties settings = loadSettings();
        settings.setProperty(KEY_INTERVAL_DAYS, Integer.toString(days));
        saveSettings(settings);
    }

    private Properties loadSettings() {
        Properties props = new Properties();
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                log.warn("Could not read local auto-backup settings, using defaults", e);
            }
        }
        return props;
    }

    private void saveSettings(Properties props) {
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "Local machine-only auto-backup settings - not part of any database backup");
        } catch (IOException e) {
            log.error("Could not save local auto-backup settings", e);
        }
    }
}
