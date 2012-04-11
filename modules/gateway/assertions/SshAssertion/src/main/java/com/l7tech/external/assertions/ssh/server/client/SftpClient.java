package com.l7tech.external.assertions.ssh.server.client;

import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.l7tech.message.SshKnob;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Currently wraps JSCAPE's SFTP client, exposing a subset of the client methods for the Gateway.
 */
public class SftpClient implements SshClient {
    Sftp sftpClient;

    public SftpClient(@NotNull Sftp sftpClient) {
        this.sftpClient = sftpClient;
        this.sftpClient.disableFileAccessLogging();
    }

    public void connect() throws ScpException, SftpException {
        sftpClient.connect();
    }

    public boolean isConnected() {
        return sftpClient.isConnected();
    }

    public void disconnect() {
        sftpClient.disconnect();
    }

    public void upload(InputStream in, String remoteDir, String remoteFile) throws IOException {
        if (!StringUtils.isEmpty(remoteDir)) {
            sftpClient.setDir(remoteDir);
        }
        sftpClient.upload(in, remoteFile);
    }

    public void upload(InputStream in, String remoteDir, String remoteFile, SshKnob.FileMetadata fileMetadata) throws IOException{
        upload(in, remoteDir, remoteFile);
        if(fileMetadata != null && fileMetadata.getPermission() >= 0){
            sftpClient.setFilePermissions(remoteFile, fileMetadata.getPermission());
        }
    }
    
    public void download(OutputStream out, String remoteDir, String remoteFile) throws IOException {
        if (!StringUtils.isEmpty(remoteDir)) {
            sftpClient.setDir(remoteDir);
        }
        sftpClient.download(out, remoteFile);
    }
}
