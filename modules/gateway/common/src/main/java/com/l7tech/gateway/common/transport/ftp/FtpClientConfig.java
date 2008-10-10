package com.l7tech.gateway.common.transport.ftp;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * Interface that captures most of a FTP client's configuration items.
 *
 * todo: May/should be used by the FTP Routing Assertion?
 *
 * @author jbufu
 * @since SecureSpan 4.6
 */
public interface FtpClientConfig  extends Serializable {

    // FTP constants
    public static final int DEFAULT_TIMEOUT = 10000; // milliseconcds
    public static final int DEFAULT_FTP_PORT = 21;
    public static final int DEFAULT_FTPS_PORT = 990;
    public static final String DEFAULT_ANON_USER = "ftp";

    // connection type
    public FtpClientConfig setSecurity(FtpSecurity security);
    public FtpSecurity getSecurity();

    // common FTP/FTPS parameters
    public FtpClientConfig setHost(String host);
    public String getHost();

    public FtpClientConfig setPort(int port);
    public int getPort();

    public FtpClientConfig setTimeout(int timeout);
    public int getTimeout();

    public FtpClientConfig setUser(String user);
    public String getUser();

    public FtpClientConfig setPass(String pass);
    public String getPass();

    public FtpClientConfig setDirectory(String directory);
    public String getDirectory();

    public FtpClientConfig setDebugStream(PrintStream debugStream);
    public PrintStream getDebugStream();

    // FTPS parameters
    public FtpClientConfig setVerifyServerCert(boolean verify);
    public boolean isVerifyServerCert();

    // authentication
    public FtpClientConfig setUseClientCert(boolean useCert);
    public boolean isUseClientCert();
    
    public FtpClientConfig setCredentialsSource(FtpCredentialsSource credSource);
    public FtpCredentialsSource getCredentialsSource();

    public FtpClientConfig setClientCertId(long id);
    public long getClientCertId();

    public FtpClientConfig setClientCertAlias(String alias);
    public String getClientCertAlias();
}
