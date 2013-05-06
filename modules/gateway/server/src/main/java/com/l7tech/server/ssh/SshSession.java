package com.l7tech.server.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.l7tech.server.ssh.client.ScpClient;
import com.l7tech.server.ssh.client.ScpClientImpl;
import com.l7tech.server.ssh.client.SftpClient;
import com.l7tech.server.ssh.client.SftpClientImpl;

/**
 * This is a wrapper for a Session. It enables easy creation of different types of ssh clients.
 *
 * @author Victor Kazakov
 */
public class SshSession implements AutoCloseable {
    private SshSessionKey key;
    private Session session;

    /**
     * Create a new SshSession with the given key and session to use. Note only the session factory should be creating SshSessions
     *
     * @param key     The key of this sshSession
     * @param session The session to use
     */
    protected SshSession(SshSessionKey key, Session session) {
        this.key = key;
        this.session = session;
    }

    /**
     * Returns the session key
     *
     * @return returns the SshSessionKey for this SshSession
     */
    public SshSessionKey getKey() {
        return key;
    }

    /**
     * Creates a new SftpClient to use on this session.
     *
     * @return A new SftpClient that on this session
     * @throws JSchException This is thrown if there was an error creating the client
     */
    public SftpClient getSftpClient() throws JSchException {
        return new SftpClientImpl(session);
    }

    /**
     * Creates a new ScpClient to use on this session.
     *
     * @return A new ScpClient that on this session
     * @throws JSchException This is thrown if there was an error creating the client
     */
    public ScpClient getScpClient() throws JSchException {
        return new ScpClientImpl(session);
    }

    /**
     * Closes the session
     */
    public void close() {
        session.disconnect();
    }

    /**
     * Checks if the session is connected.
     *
     * @return true if the session is connected. False otherwise.
     */
    public boolean isConnected() {
        return session.isConnected();
    }

    /**
     * Connects to the ssh server. Give the connection timeout. 0 for an infinite timeout
     *
     * @param connectionTimeout The connection timeout. 0 for an infinite timeout
     * @throws JSchException This is thrown if there was an error connecting to the ssh server.
     */
    public void connect(int connectionTimeout) throws JSchException {
        session.connect(connectionTimeout);
    }
}
