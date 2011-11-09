package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.external.assertions.ssh.SftpPollingListenerDialogSettings;

/**
 * SFTP polling listener resource
 */
public class SftpPollingListenerResource {
    private static final String SEP = "; ";

    private SftpPollingListenerDialogSettings configuration;
    private String password;
    private String privateKey;

    public SftpPollingListenerResource(SftpPollingListenerDialogSettings configuration) {
        this.configuration = configuration;
    }

    public long getResId() {
        return configuration.getResId();
    }

    public int getVersion() {
        return configuration.getVersion();
    }

    public String getName() {
        return configuration.getName();
    }

    public String getHostname() {
        return configuration.getHostname();
    }

    public int getPort() {
        return configuration.getPort();
    }

    public String getHostKey() {
        return configuration.getHostKey();
    }

    public String getUsername() {
        return configuration.getUsername();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getDirectory() {
        return configuration.getDirectory();
    }

    public int getPollingInterval() {
        return configuration.getPollingInterval();
    }

    public String getContentType() {
        return configuration.getContentType();
    }

    public boolean isActive() {
        return configuration.isActive();
    }

    public boolean isDeleteOnReceive() {
        return configuration.isDeleteOnReceive();
    }

    public boolean isEnableResponses() {
        return configuration.isEnableResponses();
    }

    public boolean isHardwiredService() {
        return configuration.isHardwiredService();
    }

    public Long getHardwiredServiceId() {
        return configuration.getHardwiredServiceId();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("SFTP polling listener resource settings:");
        sb.append(getResId()).append(SEP);
        sb.append(getVersion()).append(SEP);
        sb.append(getName()).append(SEP);
        sb.append(getHostname()).append(SEP);
        sb.append(getPort()).append(SEP);
        sb.append(getUsername()).append(SEP);
        sb.append(getPrivateKey() != null ? "use private key" : "do not use private key").append(SEP);
        sb.append(getDirectory()).append(SEP);
        sb.append(getPollingInterval()).append(SEP);
        sb.append(getContentType()).append(SEP);
        sb.append(isActive() ? "started" : "stopped").append(SEP);
        sb.append(isDeleteOnReceive() ? "delete on receive" : "do not delete on receive").append(SEP);
        sb.append(isEnableResponses() ? "return responses" : "drop responses").append(SEP);
        sb.append(isHardwiredService() ? "route to service (" + getHardwiredServiceId() + ")" : "use namespace resolution").append(".");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if( ! (obj instanceof SftpPollingListenerResource))
         return super.equals(obj);

        SftpPollingListenerResource queue = (SftpPollingListenerResource) obj;

        return this.getResId() == queue.getResId() &&
               this.getVersion() == queue.getVersion() ;
    }
}
