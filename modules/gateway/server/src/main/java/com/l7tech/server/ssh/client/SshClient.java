package com.l7tech.server.ssh.client;

import com.jcraft.jsch.JSchException;

import java.io.Closeable;

/**
 * This is a base class for an ssh client. It implements closable so it can be used in java 7 try-with-resources statements.
 *
 * @author Victor Kazakov
 */
public interface SshClient extends Closeable {

    /**
     * Connect the client. Does nothing if the client is already connected.
     * NOTE: close must be called after connection otherwise the connection may remain open.
     *
     * @throws JSchException This is thrown is there was an error establishing the connection.
     */
    public void connect() throws JSchException;

    /**
     * Checks if the client is connected.
     *
     * @return true if the client is connected, false if it is not connected.
     */
    public boolean isConnected();

    /**
     * Close the client connection. Does nothing if the client is not connected.
     */
    public void close();
}
