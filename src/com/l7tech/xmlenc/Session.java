package com.l7tech.xmlenc;

import com.l7tech.identity.User;

import java.security.Key;

/**
 * User: flascell
 * Date: Aug 25, 2003
 * Time: 3:43:45 PM
 *
 * XML-ENC session
 */
public class Session {

    /**
     * get a session from the SessionManager
     */
    Session() {
        creationTimestamp = System.currentTimeMillis();
    }

    public long getHighestSeq() {
        return highestSeq;
    }

    public void setHighestSeq(long highestSeq) {
        this.highestSeq = highestSeq;
    }

    public byte[] getKeyIn() {
        return keyin;
    }

    public void setKeyIn(byte[] key) {
        this.keyin = key;
    }

    public byte[] getKeyOut() {
        return keyout;
    }

    public void setKeyOut(byte[] key) {
        this.keyout = key;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getNrRequestsUsed() {
        return nrRequestsUsed;
    }

    public long incrementRequestsUsed() {
        return ++nrRequestsUsed;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private long id = 0;
    private long highestSeq = 0;
    private byte[] keyin = null;
    private byte[] keyout = null;
    private long creationTimestamp = 0;
    private long nrRequestsUsed = 0;
}
