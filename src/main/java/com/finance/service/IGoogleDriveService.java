package com.finance.service;

import java.io.File;

public interface IGoogleDriveService {

    /** True once a successful OAuth2 connection has been established in this session. */
    boolean isConnected();

    /** Runs the OAuth2 installed-app flow (opens the system browser) and stores the token for reuse. */
    void connect() throws Exception;

    /** Revokes the current Google account's token and clears the locally stored token, so the next connect() requires a fresh sign-in. */
    void disconnect() throws Exception;

    /**
     * Uploads the given local dump file to the user's Google Drive root folder as backup_data.sql.
     * @return a human-readable success message
     */
    String upload(File localDumpFile) throws Exception;

    /**
     * Downloads the most recently modified backup_data.sql found on Google Drive into targetFolder.
     * @return the downloaded file
     */
    File downloadLatest(File targetFolder) throws Exception;
}
