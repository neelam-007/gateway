package com.l7tech.server.secureconversation;

import com.l7tech.identity.User;
import com.l7tech.common.security.xml.WssProcessor;

import javax.crypto.SecretKey;

/**
 * A secure conversation session used between a client and the ssg.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationSession implements WssProcessor.SecurityContext {
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public SecretKey getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(SecretKey sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getCreation() {
        return creation;
    }

    public void setCreation(long creation) {
        this.creation = creation;
    }

    public User getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(User usedBy) {
        this.usedBy = usedBy;
    }

    private String identifier;
    private SecretKey sharedSecret;
    private long expiration;
    private long creation;
    private User usedBy;
}
