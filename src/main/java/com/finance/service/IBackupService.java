package com.finance.service;

import java.io.File;

public interface IBackupService {

    /**
     * Runs mysqldump and writes the output as backup_data.sql inside targetFolder.
     * @return the absolute path of the created backup file
     */
    String exportToLocal(File targetFolder) throws Exception;

    /**
     * Runs mysql to import the given .sql file into the configured database.
     * @return a human-readable success message
     */
    String importFromLocal(File sqlFile) throws Exception;
}
