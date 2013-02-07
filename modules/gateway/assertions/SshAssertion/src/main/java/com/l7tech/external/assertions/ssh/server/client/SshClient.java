package com.l7tech.external.assertions.ssh.server.client;

import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.SftpException;

/**
 * SSH client methods used for SSH support in the Gateway.
 * Currently intended to make common JSCAPE's SCP and SFTP client methods, which does not implement a common interface.
 */
public interface SshClient {
    public void connect() throws ScpException, SftpException ;
    public boolean isConnected();
    public void disconnect();
}
