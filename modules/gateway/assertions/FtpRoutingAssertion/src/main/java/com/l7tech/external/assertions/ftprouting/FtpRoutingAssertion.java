/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.external.assertions.ftprouting;

import com.l7tech.util.Functions;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>FtpRoutingAssertion</code> is an assertion that routes the request
 * content to an FTP server.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpRoutingAssertion extends RoutingAssertion implements UsesVariables {

    public static final int DEFAULT_FTP_PORT = 21;
    public static final int DEFAULT_FTPS_IMPLICIT_PORT = 990;

    /** Timeout (in milliseconds) when opening connection to FTP server. */
    public static final int DEFAULT_TIMEOUT = 10000;

    private FtpSecurity _security;

    private boolean _verifyServerCert;

    /** FTP server host name. */
    private String _hostName;

    /** Port number. */
    private int _port = DEFAULT_FTP_PORT;

    /** Destination directory pattern.  Can contain context variables. */
    private String _directory;

    /** Where the file name on server will come from. */
    private FtpFileNameSource _fileNameSource;

    /** File name pattern if {@link #_fileNameSource} is {@link FtpFileNameSource#PATTERN}; can contain context variables. */
    private String _fileNamePattern;

    /** Where the login credentials wil come from. */
    private FtpCredentialsSource _credentialsSource;

    /** User name to use if {@link #_credentialsSource} is {@link FtpCredentialsSource#SPECIFIED}. */
    private String _userName;

    /** Password to use if {@link #_credentialsSource} is {@link FtpCredentialsSource#SPECIFIED}. */
    private String _password;

    /** Whether to use client cert and private key for authentication. */
    private boolean _useClientCert;

    /** ID of keystore to use if {@link #_useClientCert} is true. */
    private long _clientCertKeystoreId;

    /** Key alias in keystore to use if {@link #_useClientCert} is true. */
    private String _clientCertKeyAlias;

    /** Timeout for opening connection to FTP server (in milliseconds). */
    private int _timeout = DEFAULT_TIMEOUT;

    public FtpRoutingAssertion() {
        // This is because this was the effective default when the Server
        // assertion was not actually checking this property.
        setCurrentSecurityHeaderHandling(LEAVE_CURRENT_SECURITY_HEADER_AS_IS);
    }

    public FtpCredentialsSource getCredentialsSource() {
        return _credentialsSource;
    }

    public void setCredentialsSource(FtpCredentialsSource credentialsSource) {
        _credentialsSource = credentialsSource;
    }

    public boolean isVerifyServerCert() {
        return _verifyServerCert;
    }

    public void setVerifyServerCert(boolean b) {
        _verifyServerCert = b;
    }

    public boolean isUseClientCert() {
        return _useClientCert;
    }

    public void setUseClientCert(boolean useClientCert) {
        _useClientCert = useClientCert;
    }

    public String getClientCertKeyAlias() {
        return _clientCertKeyAlias;
    }

    public void setClientCertKeyAlias(String clientCertKeyAlias) {
        _clientCertKeyAlias = clientCertKeyAlias;
    }

    public long getClientCertKeystoreId() {
        return _clientCertKeystoreId;
    }

    public void setClientCertKeystoreId(long clientCertKeystoreId) {
        _clientCertKeystoreId = clientCertKeystoreId;
    }

    public String getDirectory() {
        return _directory;
    }

    public void setDirectory(String directory) {
        _directory = directory;
    }

    public String getFileNamePattern() {
        return _fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        _fileNamePattern = fileNamePattern;
    }

    public FtpFileNameSource getFileNameSource() {
        return _fileNameSource;
    }

    public void setFileNameSource(FtpFileNameSource fileNameSource) {
        _fileNameSource = fileNameSource;
    }

    public String getHostName() {
        return _hostName;
    }

    public void setHostName(String hostName) {
        _hostName = hostName;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public int getPort() {
        return _port;
    }

    public void setPort(int port) {
        _port = port;
    }

    public FtpSecurity getSecurity() {
        return _security;
    }

    public void setSecurity(FtpSecurity security) {
        _security = security;
    }

    public int getTimeout() {
        return _timeout;
    }

    public void setTimeout(int timeout) {
        _timeout = timeout;
    }

    public String getUserName() {
        return _userName;
    }

    public void setUserName(String userName) {
        _userName = userName;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "FTP(S) Routing Assertion");
        meta.put(LONG_NAME, "Route request to FTP server");

        meta.put(PALETTE_NODE_NAME, "FTP(S) Routing Assertion");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "routing" });

        meta.put(POLICY_NODE_NAME, "Route request to FTP(S) server");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, FtpRoutingAssertion>() {
            public String call(FtpRoutingAssertion assertion) {
                final StringBuilder sb = new StringBuilder("Route request to FTP");
                if (assertion.getSecurity() == FtpSecurity.FTPS_EXPLICIT ||
                    assertion.getSecurity() == FtpSecurity.FTPS_IMPLICIT) {
                    sb.append("S");
                }
                sb.append(" server ");
                sb.append(assertion.getHostName());
                return sb.toString();
            }
        });

        meta.put(WSP_EXTERNAL_NAME, "FtpRoutingAssertion");
        final TypeMapping typeMapping = (TypeMapping)meta.get(WSP_TYPE_MAPPING_INSTANCE);
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put("FtpRouting", typeMapping);
        }});

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new WspEnumTypeMapping(FtpSecurity.class, "security"),
            new WspEnumTypeMapping(FtpFileNameSource.class, "fileNameSource"),
            new WspEnumTypeMapping(FtpCredentialsSource.class, "credentialsSource")
        )));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:FtpRouting" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }

    public String[] getVariablesUsed() {
        Set<String> vars = new HashSet<String>();
        vars.addAll(Arrays.asList(Syntax.getReferencedNames(getFileNamePattern())));
        vars.addAll(Arrays.asList(Syntax.getReferencedNames(getDirectory())));
        return vars.toArray(new String[vars.size()]);
    }
}
