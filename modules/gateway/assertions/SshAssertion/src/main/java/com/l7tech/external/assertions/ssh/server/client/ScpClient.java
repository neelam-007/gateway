package com.l7tech.external.assertions.ssh.server.client;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;

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

    @Override
    public void connect() throws ScpException {
        scpClient.connect();
    }

    @Override
    public boolean isConnected() {
        return scpClient.isConnected();
    }

    @Override
    public void disconnect() {
        scpClient.disconnect();
    }

    /**
     * Uploads the input stream to the scpServer.
     *
     * @param in The input stream to upload
     * @param remoteDir The remote directory the file is located in
     * @param remoteFile The Name of the remote file
     * @param fileLength The file length. If this is -1 the entire stream will be uploaded until EOF
     * @throws IOException This is thrown if there was an exception writing to the file.
     */
    public void upload(InputStream in, String remoteDir, String remoteFile, long fileLength) throws IOException {
        if(fileLength == -1){
            //This will upload in.available() bits of data.
            scpClient.upload(in, normalizeAndAppendDirectorySeparator(remoteDir), remoteFile);
        } else {
            scpClient.upload(in, fileLength, normalizeAndAppendDirectorySeparator(remoteDir), remoteFile);
        }
    }

    /**
     * This will write data to the output stream given for the specified file.
     *
     * @param out The output stream to write file data to.
     * @param remoteDir The remote directory the file is located in
     * @param remoteFile The Name of the remote file
     * @throws IOException This is thrown if there was an exception reading the file.
     */
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
