package com.l7tech.external.assertions.ssh.server.client;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.SftpException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Currently wraps JSCAPE's SCP client, exposing a subset of the client methods for the Gateway.
 */
public class ScpClient implements SshClient {
    Scp scpClient;

    public ScpClient(Scp scpClient) {
        this.scpClient = scpClient;
    }

    public void setTimeout(long timeOut) {
        // JSCAPE's Scp client does not support timeout
    }

    public void connect() throws ScpException, SftpException {
        scpClient.connect();
    }

    public boolean isConnected() {
        return scpClient.isConnected();
    }

    public void disconnect() {
        scpClient.disconnect();
    }

    public void upload(InputStream in, String remoteDir, String remoteFile) throws IOException {
        scpClient.upload(in, normalizeAndAppendDirectorySeparator(remoteDir), remoteFile);
    }

    public void download(OutputStream out, String remoteDir, String remoteFile) throws IOException {
        scpClient.download(out, normalizeAndAppendDirectorySeparator(remoteDir), remoteFile);
    }

    private String normalizeAndAppendDirectorySeparator(String directory) {
        String normalizedDirectory = directory;
        if (normalizedDirectory != null) {
            // normalize separator character, separator character should be '/' always
            normalizedDirectory = normalizedDirectory.replace(File.separatorChar, '/');
            normalizedDirectory = normalizedDirectory.replace('\\', '/');

            // append directory separator to path if required, JScape SCP client simply concatenates the path and file name
            if (normalizedDirectory != null && !normalizedDirectory.endsWith("/")) {
                normalizedDirectory = normalizedDirectory + "/";
            }
        }
        return normalizedDirectory;
    }
}
