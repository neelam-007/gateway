package com.l7tech.external.assertions.ssh.server;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Encapsulate SSH client logic used by ServerSshRouteAssertion.
 * Currently wraps JSCAPE's SCP and SFTP clients, exposing a subset of the client methods.
 */
public class ServerSshRouteClient {
    Object sshClient;

    public ServerSshRouteClient(Sftp sftpClient) {
        this.sshClient = sftpClient;
    }

    public ServerSshRouteClient(Scp scpClient) {
        this.sshClient = scpClient;
    }

    public void setTimeout(long timeOut) {
        if (sshClient instanceof Sftp) {
            ((Sftp)sshClient).setTimeout(timeOut);
        }
        // JSCAPE's Scp client does not support timeout
    }

    public void connect() throws ScpException, SftpException {
        if (sshClient instanceof Sftp) {
            ((Sftp)sshClient).connect();
        } else if (sshClient instanceof Scp) {
            ((Scp)sshClient).connect();
        }
    }

    public boolean isConnected() {
        boolean result = false;
        if (sshClient instanceof Sftp) {
            result = ((Sftp)sshClient).isConnected();
        } else if (sshClient instanceof Scp) {
            result = ((Scp)sshClient).isConnected();
        }
        return result;
    }

    public void disconnect() {
        if (sshClient instanceof Sftp) {
            ((Sftp)sshClient).disconnect();
        } else if (sshClient instanceof Scp) {
            ((Scp)sshClient).disconnect();
        }
    }

    public void upload(InputStream in, String remoteDir, String remoteFile) throws IOException {
        if (sshClient instanceof Sftp) {
            if (!StringUtils.isEmpty(remoteDir)) {
                ((Sftp)sshClient).setDir(remoteDir);
            }
            ((Sftp)sshClient).upload(in, remoteFile);
        } else if (sshClient instanceof Scp) {
            ((Scp)sshClient).upload(in, normalizeAndAppendDirectorySeparator(remoteDir), remoteFile);
        }
    }

    public void download(OutputStream out, String remoteDir, String remoteFile) throws IOException {
        if (sshClient instanceof Sftp) {
            if (!StringUtils.isEmpty(remoteDir)) {
                ((Sftp)sshClient).setDir(remoteDir);
            }
            ((Sftp)sshClient).download(out, remoteFile);
        } else if (sshClient instanceof Scp) {
            ((Scp)sshClient).download(out, normalizeAndAppendDirectorySeparator(remoteDir), remoteFile);
        }

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
