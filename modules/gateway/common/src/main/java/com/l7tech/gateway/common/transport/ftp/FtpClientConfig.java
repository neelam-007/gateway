package com.l7tech.gateway.common.transport.ftp;

import com.l7tech.objectmodel.Goid;

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
public interface FtpClientConfig extends Serializable, Cloneable {

    // FTP constants
    static final int DEFAULT_TIMEOUT = 10000; // milliseconcds
    static final int DEFAULT_FTP_PORT = 21;
    static final int DEFAULT_FTPS_PORT = 990;
    static final String DEFAULT_ANON_USER = "ftp";

    // connection type
    FtpClientConfig setSecurity(FtpSecurity security);
    FtpSecurity getSecurity();

    // common FTP/FTPS parameters
    FtpClientConfig setHost(String host);
    String getHost();

    FtpClientConfig setPort(int port);
    int getPort();

    FtpClientConfig setTimeout(int timeout);
    int getTimeout();

    FtpClientConfig setUser(String user);
    String getUser();

    FtpClientConfig setPass(String pass);
    String getPass();

    FtpClientConfig setDirectory(String directory);
    String getDirectory();

    FtpClientConfig setDebugStream(PrintStream debugStream);
    PrintStream getDebugStream();

    // FTPS parameters
    FtpClientConfig setVerifyServerCert(boolean verify);
    boolean isVerifyServerCert();

    // authentication
    FtpClientConfig setUseClientCert(boolean useCert);
    boolean isUseClientCert();
    
    FtpClientConfig setCredentialsSource(FtpCredentialsSource credSource);
    FtpCredentialsSource getCredentialsSource();

    FtpClientConfig setClientCertId(long id);
    FtpClientConfig setClientCertId(Goid id);
    Goid getClientCertId();

    FtpClientConfig setClientCertAlias(String alias);
    String getClientCertAlias();

    // status
    FtpClientConfig setEnabled(boolean enabled);
    boolean isEnabled();

    public Object clone() throws CloneNotSupportedException;
}
