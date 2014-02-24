package com.l7tech.gateway.common.transport.ftp;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.GoidUpgradeMapper;

import java.io.PrintStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * @author jbufu
 */
public class FtpClientConfigImpl implements FtpClientConfig, Cloneable {

    private static final long serialVersionUID = 8783020368179729829L;

    private FtpSecurity security = FtpSecurity.FTP_UNSECURED;

    // Ftp/Ftps common params
    private String host; // required
    private int port = -1;
    private int timeout = DEFAULT_TIMEOUT; // milliseconds
    private String user = DEFAULT_ANON_USER;
    private String pass = "";
    private String directory;
    // Ftps specific params
    private boolean isVerifyServerCert = false;

    /**
     * Not serializable
     */
    private transient PrintStream debugStream;

    //status
    private boolean enabled = true;

    // authentication
    private boolean useClientCert = false;
    private FtpCredentialsSource credentialsSource = FtpCredentialsSource.SPECIFIED;
    // DO NOT REMOVE!
    // The long clientCertKeystoreId needs to be here for backwards compatibility serialization purposes.
    @SuppressWarnings("UnusedDeclaration")
    private long clientCertKeystoreId = -1;
    private Goid clientCertKeystoreGoid = PersistentEntity.DEFAULT_GOID;
    private String clientCertKeyAlias;

    private FtpClientConfigImpl() {
    }

    public static FtpClientConfig newFtpConfig(String host) {
        FtpClientConfig config = new FtpClientConfigImpl();
        config.setHost(host);
        return config;
    }

    @Override
    public FtpClientConfig setHost(String host) { this.host = host; return this; }
    @Override
    public String getHost() { return this.host; }

    @Override
    public FtpClientConfig setPort(int port) { this.port = port; return this; }
    @Override
    public int getPort() {
        return port != -1 ? port : FtpSecurity.FTP_UNSECURED == security ? DEFAULT_FTP_PORT : DEFAULT_FTPS_PORT;
    }

    @Override
    public FtpClientConfig setTimeout(int timeout) { this.timeout = timeout; return this; }
    @Override
    public int getTimeout() { return timeout; }

    @Override
    public FtpClientConfig setUser(String user) { this.user = user; return this; }
    @Override
    public String getUser() { return user; }

    @Override
    public FtpClientConfig setPass(String pass) { this.pass = pass; return this;}
    @Override
    public String getPass() { return pass; }

    @Override
    public FtpClientConfig setDirectory(String directory) { this.directory = directory; return this; }
    @Override
    public String getDirectory() { return directory; }

    @Override
    public FtpClientConfig setDebugStream(PrintStream debugStream) { this.debugStream = debugStream; return this; }
    @Override
    public PrintStream getDebugStream() { return debugStream; }


    /**
     * Configures connection security type / level.
     *
     * @see com.l7tech.gateway.common.transport.ftp.FtpSecurity
     * @param security      the connection security type
     * @return this config
     */
    @Override
    public FtpClientConfig setSecurity(FtpSecurity security) {this.security = security; return this; }

    @Override
    public FtpSecurity getSecurity() { return this.security; }

    // FTPS parameters
    @Override
    public FtpClientConfig setVerifyServerCert(boolean verify) { this.isVerifyServerCert = verify; return this; }

    @Override
    public boolean isVerifyServerCert() { return this.isVerifyServerCert; }

    // authentication
    @Override
    public FtpClientConfig setUseClientCert(boolean useCert) { this.useClientCert = useCert; return this; }
    @Override
    public boolean isUseClientCert() { return this.useClientCert; }

    /**
     * The source of the credentials.
     * If not {@link com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource#SPECIFIED}
     * the build() methods will throw IllegalStateException.
     *
     * @see com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource
     * @param credSource the credentials source
     * @return this instance
     */
    @Override
    public FtpClientConfig setCredentialsSource(FtpCredentialsSource credSource) {
        this.credentialsSource = credSource;
        return this;
    }

    @Override
    public FtpCredentialsSource getCredentialsSource() { return this.credentialsSource; }

    @Override
    @Deprecated
    public FtpClientConfig setClientCertId(long id) { this.clientCertKeystoreGoid = GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, id); return this; }

    @Override
    public FtpClientConfig setClientCertId(Goid id) { this.clientCertKeystoreGoid = id; return this; }
    @Override
    public Goid getClientCertId() { return this.clientCertKeystoreGoid; }

    @Override
    public FtpClientConfig setClientCertAlias(String alias) { this.clientCertKeyAlias = alias; return this; }
    @Override
    public String getClientCertAlias() { return this.clientCertKeyAlias; }

    @Override
    public FtpClientConfig setEnabled(boolean enabled) { this.enabled = enabled; return this; }
    @Override
    public boolean isEnabled() { return enabled; }

    private void readObject( final ObjectInputStream in) throws IOException, ClassNotFoundException {
        enabled = true;
        in.defaultReadObject();
    }

    /**
     * Everything apart from the printstream can be cloned using default super behaviour. All other primitive
     * non primitive types are immutable classes
     * @return a clone of this instance
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }
}
