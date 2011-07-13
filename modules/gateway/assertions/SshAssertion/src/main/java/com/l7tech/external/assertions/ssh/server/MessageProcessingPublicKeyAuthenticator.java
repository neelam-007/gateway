package com.l7tech.external.assertions.ssh.server;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;

/**
 * Stores user public key for the Message Processor.
 */
public class MessageProcessingPublicKeyAuthenticator implements PublickeyAuthenticator {

    private String userName;
    PublicKey publicKey;

   public boolean authenticate(String username, PublicKey key, ServerSession session) {
        this.userName = username;
        this.publicKey = key;

        // allow all access, defer authentication to Gateway policy assertion
        return true;
    }

    public String getUserName() {
        return userName;
    }
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
