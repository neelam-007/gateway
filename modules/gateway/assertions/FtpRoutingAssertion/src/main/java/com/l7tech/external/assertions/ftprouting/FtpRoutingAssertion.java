/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.external.assertions.ftprouting;

import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.Arrays;
import java.util.HashMap;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * <code>FtpRoutingAssertion</code> is an assertion that routes the request
 * content to an FTP server.
 *
 * @author rmak
 * @since SecureSpan 4.0
 */
public class FtpRoutingAssertion extends RoutingAssertion implements UsesVariables, UsesPrivateKeys {

    private static final String META_INITIALIZED = FtpRoutingAssertion.class.getName() + ".metadataInitialized";

    public static final int DEFAULT_FTP_PORT = 21;
    public static final int DEFAULT_FTPS_IMPLICIT_PORT = 990;

    /** Timeout (in milliseconds) when opening connection to FTP server. */
    public static final int DEFAULT_TIMEOUT = 10000;

    private FtpSecurity _security;

    private boolean _verifyServerCert;

    /** FTP server host name. Can contain context variables. */
    private String _hostName;

    /** Port number. Can contain context variables. */
    private String _port = Integer.toString(DEFAULT_FTP_PORT);

    /** Destination directory pattern. Can contain context variables. */
    private String _directory;

    /** Where the file name on server will come from. */
    private FtpFileNameSource _fileNameSource;

    /** File name pattern if {@link #_fileNameSource} is {@link FtpFileNameSource#PATTERN}; can contain context variables. */
    private String _fileNamePattern;

    /** Where the login credentials wil come from. */
    private FtpCredentialsSource _credentialsSource;

    /** User name to use if {@link #_credentialsSource} is {@link FtpCredentialsSource#SPECIFIED}. Can contain context variables. */
    private String _userName;

    /** Password to use if {@link #_credentialsSource} is {@link FtpCredentialsSource#SPECIFIED}. Can contain context variables. */
    private String _password;

    /** Explicit flag, in order to allow "${foo}" literal passwords */
    private boolean _passwordUsesContextVariables;

    /** Whether to use client cert and private key for authentication. */
    private boolean _useClientCert;

    /** ID of keystore to use if {@link #_useClientCert} is true. */
    private Goid _clientCertKeystoreId;

    /** Key alias in keystore to use if {@link #_useClientCert} is true. */
    private String _clientCertKeyAlias;

    /** Timeout for opening connection to FTP server (in milliseconds). */
    private int _timeout = DEFAULT_TIMEOUT;

    private MessageTargetableSupport requestTarget = defaultRequestTarget();

    private MessageTargetableSupport defaultRequestTarget() {
        return new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    }

    public FtpRoutingAssertion() {
        // This is because this was the effective default when the Server
        // assertion was not actually checking this property.
        setCurrentSecurityHeaderHandling(CLEANUP_CURRENT_SECURITY_HEADER);
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

    public Goid getClientCertKeystoreId() {
        return _clientCertKeystoreId;
    }

    public void setClientCertKeystoreId(Goid clientCertKeystoreId) {
        _clientCertKeystoreId = clientCertKeystoreId;
    }

    @Deprecated
    public void setClientCertKeystoreId(long clientCertKeystoreId) {
        _clientCertKeystoreId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, clientCertKeystoreId);
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

    @WspSensitive
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.VARIABLE)
    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public boolean isPasswordUsesContextVariables() {
        return _passwordUsesContextVariables;
    }

    public void setPasswordUsesContextVariables(boolean passwordUsesContextVariables) {
        _passwordUsesContextVariables = passwordUsesContextVariables;
    }

    public String getPort() {
        return _port;
    }

    public void setPort(String port) {
        _port = port;
    }

    /** @deprecated Used for deserialization of pre-bug5987 assertions */
    @Deprecated
    public void setPort(int port) {
        _port = Integer.toString(port);
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

    public MessageTargetableSupport getRequestTarget() {
        return requestTarget != null ? requestTarget : defaultRequestTarget();
    }

    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget;
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return requestTarget == null || TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return false;
    }

    @Override
    public boolean needsInitializedResponse() {
        return requestTarget != null && TargetMessageType.RESPONSE == requestTarget.getTarget();
    }

    private final static String baseName = "Route via FTP(S)";

    @Override
    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Route requests from the Gateway to a backend FTP(S) server, using passive mode FTP.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "routing" });

        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<FtpRoutingAssertion>() {
            @Override
            public String getAssertionName(final FtpRoutingAssertion assertion, final boolean decorate) {
                if(!decorate) return baseName;

                final StringBuilder sb = new StringBuilder("Route via FTP");
                if (assertion.getSecurity() == FtpSecurity.FTPS_EXPLICIT ||
                    assertion.getSecurity() == FtpSecurity.FTPS_IMPLICIT) {
                    sb.append("S");
                }
                sb.append(" Server ");
                sb.append(assertion.getHostName());
                
                return AssertionUtils.decorateName(assertion, sb.toString());
            }
        });

        meta.put(PROPERTIES_ACTION_NAME, "FTP(S) Routing Properties");
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

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return requestTarget.getMessageTargetVariablesUsed().withExpressions(
            _hostName,
            _port,
            _directory,
            _userName,
            _passwordUsesContextVariables ? _password : null,
            _fileNamePattern
        ).asArray();
    }

    @Override
    public SsgKeyHeader[] getPrivateKeysUsed() {
        if (isUseClientCert()) {
            return new SsgKeyHeader[]{new SsgKeyHeader(getClientCertKeystoreId() + ":" + getClientCertKeyAlias(), getClientCertKeystoreId(), getClientCertKeyAlias(), getClientCertKeyAlias())};
        }
        return null;
    }
}
