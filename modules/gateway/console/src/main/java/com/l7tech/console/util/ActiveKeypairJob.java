package com.l7tech.console.util;

import com.l7tech.gateway.common.AsyncAdminMethods;

import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * helper class to encapsulate Active Keypair job elements
 * used in creating private keys
 */
public class ActiveKeypairJob implements Serializable {
    public static final int DEFAULT_POLL_INTERVAL = 1011; //magic number

    private AsyncAdminMethods.JobId<X509Certificate> keypairJobId = null;
    private String activeKeypairJobAlias = "";
    private long lastJobPollTime = 0;
    private long minJobPollInterval = DEFAULT_POLL_INTERVAL;

    /**
     * default constructor
     */
    public ActiveKeypairJob() {
    }

    public AsyncAdminMethods.JobId<X509Certificate> getKeypairJobId() {
        return keypairJobId;
    }

    public void setKeypairJobId(AsyncAdminMethods.JobId<X509Certificate> keypairJobId) {
        this.keypairJobId = keypairJobId;
    }

    public String getActiveKeypairJobAlias() {
        return activeKeypairJobAlias;
    }

    public void setActiveKeypairJobAlias(String activeKeypairJobAlias) {
        this.activeKeypairJobAlias = activeKeypairJobAlias;
    }

    public long getLastJobPollTime() {
        return lastJobPollTime;
    }

    public void setLastJobPollTime(long lastJobPollTime) {
        this.lastJobPollTime = lastJobPollTime;
    }

    public long getMinJobPollInterval() {
        return minJobPollInterval;
    }

    public void setMinJobPollInterval(long minJobPollInterval) {
        this.minJobPollInterval = minJobPollInterval;
    }
}