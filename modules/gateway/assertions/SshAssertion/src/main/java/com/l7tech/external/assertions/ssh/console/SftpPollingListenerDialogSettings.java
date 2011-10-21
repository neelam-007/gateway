package com.l7tech.external.assertions.ssh.console;

/**
 * SFTP polling listener configuration.
 */
public class SftpPollingListenerDialogSettings {
    private long resId = -1;
    private int version;
    private String name;
    private String hostname;
    private int port = 22;
    private String hostKey;
    private String username;
    private Long passwordOid;
    private Long privateKeyOid;
    private String directory;
    private int pollingInterval = 60;
    private String contentType = "text/xml; charset=UTF-8";
    private boolean active = false;
    private boolean deleteOnReceive = false;
    private boolean enableResponses = true;
    private boolean hardwiredService = false;
    private Long hardwiredServiceId;

    public SftpPollingListenerDialogSettings() {
    }

    public long getResId() {
        return resId;
    }

    public void setResId(long value) {
        this.resId = value;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int value) {
        this.version = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostKey() {
        return hostKey;
    }

    public void setHostKey(String hostKey) {
        this.hostKey = hostKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getPasswordOid() {
        return passwordOid;
    }

    public void setPasswordOid(Long passwordOid) {
        this.passwordOid = passwordOid;
    }

    public Long getPrivateKeyOid() {
        return privateKeyOid;
    }

    public void setPrivateKeyOid(Long privateKeyOid) {
        this.privateKeyOid = privateKeyOid;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDeleteOnReceive() {
        return deleteOnReceive;
    }

    public void setDeleteOnReceive(boolean deleteOnReceive) {
        this.deleteOnReceive = deleteOnReceive;
    }

    public boolean isEnableResponses() {
        return enableResponses;
    }

    public void setEnableResponses(boolean enableResponses) {
        this.enableResponses = enableResponses;
    }

    public boolean isHardwiredService() {
        return hardwiredService;
    }

    public void setHardwiredService(boolean hardwiredService) {
        this.hardwiredService = hardwiredService;
    }

    public Long getHardwiredServiceId() {
        return hardwiredServiceId;
    }

    public void setHardwiredServiceId(Long hardwiredServiceId) {
        this.hardwiredServiceId = hardwiredServiceId;
    }

    public SftpPollingListenerDialogSettings copyPropertiesToResource(SftpPollingListenerDialogSettings copyTo){
        copyTo.setResId(getResId());
        copyTo.setVersion(getVersion());
        copyTo.setName(getName());
        copyTo.setHostname(getHostname());
        copyTo.setPort(getPort());
        copyTo.setHostKey(getHostKey());
        copyTo.setUsername(getUsername());
        copyTo.setPasswordOid(getPasswordOid());
        copyTo.setPrivateKeyOid(getPrivateKeyOid());
        copyTo.setDirectory(getDirectory());
        copyTo.setPollingInterval(getPollingInterval());
        copyTo.setContentType(getContentType());
        copyTo.setActive(isActive());
        copyTo.setDeleteOnReceive(isDeleteOnReceive());
        copyTo.setEnableResponses(isEnableResponses());
        copyTo.setHardwiredService(isHardwiredService());
        copyTo.setHardwiredServiceId(getHardwiredServiceId());

        return copyTo;
    }
}
