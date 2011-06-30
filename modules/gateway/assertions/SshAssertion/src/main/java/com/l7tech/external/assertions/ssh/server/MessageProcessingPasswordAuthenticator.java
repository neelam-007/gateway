package com.l7tech.external.assertions.ssh.server;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 * Stores user name and password to pass to the Message Processor.
 */
public class MessageProcessingPasswordAuthenticator implements PasswordAuthenticator {

    private String userName;
    private String password;

    public boolean authenticate(String userName, String password, ServerSession session) {
        this.userName = userName;
        this.password = password;

        // allow all access, defer authentication to Gateway policy assertion
        return true;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
