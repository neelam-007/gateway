package com.l7tech.external.assertions.ssh.server.client;

import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * SSH client methods used for SSH support in the Gateway.
 * Currently intended to make common JSCAPE's SCP and SFTP client methods, which does not implement a common interface.
 */
public interface SshClient {
    public void setTimeout(long timeOut);
    public void connect() throws ScpException, SftpException ;
    public boolean isConnected();
    public void disconnect();
    public void upload(InputStream in, String remoteDir, String remoteFile) throws IOException;
    public void download(OutputStream out, String remoteDir, String remoteFile) throws IOException;
}
