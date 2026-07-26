package com.finance.service.impl;

import com.finance.service.IBackupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BackupServiceImpl implements IBackupService {

    private static final DateTimeFormatter BACKUP_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'.'HHmm");

    private static final Pattern URL_PATTERN = Pattern.compile("jdbc:mysql://([^:/]+):(\\d+)/([^?]+)");
    private static final Pattern REG_LOCATION_PATTERN = Pattern.compile("Location\\s+REG_SZ\\s+(.+)");

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Override
    public String exportToLocal(File targetFolder) throws Exception {
        if (targetFolder == null || !targetFolder.isDirectory()) {
            throw new IllegalArgumentException("Please select a valid folder");
        }
        String[] conn = parseConnection();
        File outputFile = new File(targetFolder, generateBackupFileName());

        ProcessBuilder pb = new ProcessBuilder(
                resolveMysqlCommand("mysqldump"),
                "-h", conn[0],
                "-P", conn[1],
                "-u", dbUsername,
                conn[2]
        );
        pb.environment().put("MYSQL_PWD", dbPassword);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new Exception("mysqldump command not found. Please make sure the MySQL client tools are installed and added to your system PATH.");
        }

        try (InputStream in = process.getInputStream();
             OutputStream out = new FileOutputStream(outputFile)) {
            in.transferTo(out);
        }
        String errorOutput = readStream(process.getErrorStream());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            outputFile.delete();
            throw new Exception("mysqldump failed: " + (errorOutput.isBlank() ? "exit code " + exitCode : errorOutput.trim()));
        }
        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new Exception("Backup file was not created or is empty");
        }
        return outputFile.getAbsolutePath();
    }

    @Override
    public String importFromLocal(File sqlFile) throws Exception {
        if (sqlFile == null || !sqlFile.isFile()) {
            throw new IllegalArgumentException("Please select a valid .sql file");
        }
        String[] conn = parseConnection();

        ProcessBuilder pb = new ProcessBuilder(
                resolveMysqlCommand("mysql"),
                "-h", conn[0],
                "-P", conn[1],
                "-u", dbUsername,
                conn[2]
        );
        pb.environment().put("MYSQL_PWD", dbPassword);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new Exception("mysql command not found. Please make sure the MySQL client tools are installed and added to your system PATH.");
        }

        try (InputStream fileIn = new FileInputStream(sqlFile);
             OutputStream procIn = process.getOutputStream()) {
            fileIn.transferTo(procIn);
        }
        String errorOutput = readStream(process.getErrorStream());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new Exception("mysql import failed: " + (errorOutput.isBlank() ? "exit code " + exitCode : errorOutput.trim()));
        }
        return "Database restored successfully from " + sqlFile.getName();
    }

    /**
     * If the given MySQL client command is already reachable via the current PATH, use it as-is.
     * Otherwise, look up the real install location MySQL's own Windows installer registered in
     * the registry (HKLM\SOFTWARE\MySQL AB\<version>\Location) and return the full path to the
     * exe there instead. Nothing is hardcoded - this is discovered at runtime on the client's own
     * machine. (Note: ProcessBuilder.environment() PATH changes do NOT affect how Windows/Java
     * resolves a bare command name, so the full resolved path must be passed as the command itself.)
     */
    private String resolveMysqlCommand(String name) {
        if (isOnPath(name)) {
            return name;
        }
        String binDir = findMysqlBinDirFromRegistry();
        if (binDir != null) {
            File exe = new File(binDir, name + ".exe");
            if (exe.isFile()) return exe.getAbsolutePath();
        }
        return name;
    }

    private boolean isOnPath(String exeName) {
        String path = System.getenv("Path");
        if (path == null) path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, exeName + ".exe").isFile()) return true;
        }
        return false;
    }

    private String findMysqlBinDirFromRegistry() {
        try {
            Process regProcess = new ProcessBuilder("reg", "query", "HKLM\\SOFTWARE\\MySQL AB", "/s").start();
            String output = readStream(regProcess.getInputStream());
            regProcess.waitFor();

            Matcher m = REG_LOCATION_PATTERN.matcher(output);
            String bestBinDir = null;
            while (m.find()) {
                File binDir = new File(m.group(1).trim(), "bin");
                if (binDir.isDirectory() && new File(binDir, "mysqldump.exe").isFile()) {
                    bestBinDir = binDir.getAbsolutePath();
                }
            }
            return bestBinDir;
        } catch (Exception e) {
            return null;
        }
    }

    /** e.g. backup_data_20260131.1155.sql - date before the dot, time after it, so repeated backups never collide. */
    private String generateBackupFileName() {
        return "backup_data_" + LocalDateTime.now().format(BACKUP_NAME_FORMAT) + ".sql";
    }

    private String[] parseConnection() throws Exception {
        Matcher m = URL_PATTERN.matcher(datasourceUrl);
        if (!m.find()) {
            throw new Exception("Could not parse database connection details from application.properties");
        }
        return new String[]{m.group(1), m.group(2), m.group(3)};
    }

    private String readStream(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        in.transferTo(buffer);
        return buffer.toString();
    }
}
