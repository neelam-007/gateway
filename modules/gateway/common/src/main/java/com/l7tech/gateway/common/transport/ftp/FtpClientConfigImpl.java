package com.l7tech.gateway.common.transport.ftp;

import java.io.PrintStream;

/**
 * @author jbufu
 */
public class FtpClientConfigImpl implements FtpClientConfig {

    private FtpSecurity security = FtpSecurity.FTP_UNSECURED;

    // Ftp/Ftps common params
    private String host; // required
    private int port = -1;
    private int timeout = DEFAULT_TIMEOUT; // milliseconds
    private String user = DEFAULT_ANON_USER;
    private String pass = "";
    String directory;

    /**
     * Not serializable; clients should set their own trust manager after deserialization.
     */
    private transient PrintStream debugStream;


    private FtpClientConfigImpl() {
    }

    public static FtpClientConfig newFtpConfig(String host) {
        FtpClientConfig config = new FtpClientConfigImpl();
        config.setHost(host);
        return config;
    }

    public FtpClientConfig setHost(String host) { this.host = host; return this; }
    public String getHost() { return this.host; }

    public FtpClientConfig setPort(int port) { this.port = port; return this; }
    public int getPort() {
        return port != -1 ? port : FtpSecurity.FTP_UNSECURED == security ? DEFAULT_FTP_PORT : DEFAULT_FTPS_PORT;
    }

    public FtpClientConfig setTimeout(int timeout) { this.timeout = timeout; return this; }
    public int getTimeout() { return timeout; }

    public FtpClientConfig setUser(String user) { this.user = user; return this; }
    public String getUser() { return user; }

    public FtpClientConfig setPass(String pass) { this.pass = pass; return this;}
    public String getPass() { return pass; }

    public FtpClientConfig setDirectory(String directory) { this.directory = directory; return this; }
    public String getDirectory() { return directory; }

    public FtpClientConfig setDebugStream(PrintStream debugStream) { this.debugStream = debugStream; return this; }
    public PrintStream getDebugStream() { return debugStream; }


    /**
     * Configures connection security type / level.
     *
     * @see com.l7tech.gateway.common.transport.ftp.FtpSecurity
     * @param security      the connection security type
     * @return this config
     */
    public FtpClientConfig setSecurity(FtpSecurity security) {this.security = security; return this; }

    public FtpSecurity getSecurity() { return this.security; }

    // Ftps specific params
    private boolean isVerifyServerCert = false;

    // FTPS parameters
    public FtpClientConfig setVerifyServerCert(boolean verify) { this.isVerifyServerCert = verify; return this; }

    public boolean isVerifyServerCert() { return this.isVerifyServerCert; }


    // authentication
    private boolean useClientCert = false;
    private FtpCredentialsSource credentialsSource = FtpCredentialsSource.SPECIFIED;
    private long clientCertKeystoreId = -1;
    private String clientCertKeyAlias;

    // authentication
    public FtpClientConfig setUseClientCert(boolean useCert) { this.useClientCert = useCert; return this; }
    public boolean isUseClientCert() { return this.useClientCert; }

    /**
     * The source of the credentials.
     * If not {@link com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource.SPECIFIED}
     * the build() methods will throw IllegalStateException.
     *
     * @see com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource
     * @param credSource
     * @return
     */
    public FtpClientConfig setCredentialsSource(FtpCredentialsSource credSource) {
        this.credentialsSource = credSource;
        return this;
    }

    public FtpCredentialsSource getCredentialsSource() { return this.credentialsSource; }

    public FtpClientConfig setClientCertId(long id) { this.clientCertKeystoreId = id; return this; }
    public long getClientCertId() { return this.clientCertKeystoreId; }

    public FtpClientConfig setClientCertAlias(String alias) { this.clientCertKeyAlias = alias; return this; }
    public String getClientCertAlias() { return this.clientCertKeyAlias; }

    private FtpFileNameSource fileNameSource;
    private String pattern;

    public FtpClientConfig setFileNameSource(FtpFileNameSource filenameSource) { this.fileNameSource = filenameSource; return this; }
    public FtpFileNameSource getFilenameSource() { return this.fileNameSource; }

    public FtpClientConfig setFileNamePattern(String pattern) { this.pattern = pattern; return this; }
    public String getFileNamePattern() { return this.pattern; }
}
