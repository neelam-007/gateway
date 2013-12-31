/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.external.assertions.ftprouting;

import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.*;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * <code>FtpRoutingAssertion</code> is an assertion that routes the request
 * content to an FTP server.
 *
 * @since SecureSpan 4.0
 * @author rmak
 * @author nilic
 * @author jwilliams
 */
public class FtpRoutingAssertion extends RoutingAssertion implements UsesVariables, SetsVariables, UsesPrivateKeys, UsesEntities {

    private static final String META_INITIALIZED = FtpRoutingAssertion.class.getName() + ".metadataInitialized";

    // ServerConfig property names and cluster property names for our dynamically-registered cluster properties
    public static final String SC_MAX_CONC = "ftpGlobalMaxConcurrency";
    private static final String CP_MAX_CONC = "ftp.globalMaxConcurrency";

    public static final String SC_CORE_CONC = "ftpGlobalCoreConcurrency";
    private static final String CP_CORE_CONC = "ftp.globalCoreConcurrency";

    public static final String SC_MAX_QUEUE = "ftpGlobalMaxWorkQueue";
    private static final String CP_MAX_QUEUE = "ftp.globalMaxWorkQueue";

    public static final String CP_BINDING_TIMEOUT = "ftp.identityBindingTimeout";

    public static final int DEFAULT_FTP_PORT = 21;
    public static final int DEFAULT_FTPS_IMPLICIT_PORT = 990;

    /** Timeout (in milliseconds) when opening connection to FTP server. */
    public static final int DEFAULT_TIMEOUT = 10000;

    private FtpSecurity _security;

    private boolean _verifyServerCert;

    private String ftpCommandVariable;

    private boolean useCommandVariable;

    /** FTP server host name. Can contain context variables. */
    private String _hostName;

    /** Port number. Can contain context variables. */
    private String _port = Integer.toString(DEFAULT_FTP_PORT);

    /** Working directory. Can contain context variables. (Formerly used to specify upload directory.) */
    private String _directory;

    /** Where the file name on server will come from. */
    private FtpFileNameSource _fileNameSource;

//    /** File name pattern if {@link #_fileNameSource} is {@link FtpFileNameSource#PATTERN}; can contain context variables. */
//    private String _fileNamePattern;

    /** Where the login credentials wil come from. */
    private FtpCredentialsSource _credentialsSource;

    /** User name to use if {@link #_credentialsSource} is {@link FtpCredentialsSource#SPECIFIED}. Can contain context variables. */
    private String _userName;

    /** Password to use if {@link #_credentialsSource} is {@link FtpCredentialsSource#SPECIFIED}. Can contain context variables. */
    private String _password;

    /** Password GOID */
    private Goid passwordGoid = null;

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

    /** FTP Command to use to upload or download the file*/
    private FtpCommand ftpCommand;

    private String arguments;

    private MessageTargetableSupport _requestTarget = defaultRequestTarget();

    @NotNull
    private MessageTargetableSupport _responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);

    private String responseByteLimit;

    public static final int FAIL_ON_TRANSIENT = 0;
    public static final int FAIL_ON_PERMANENT = 1;
    public static final int FAIL_ON_NO_REPLY = 2;

    private int failureMode = FAIL_ON_TRANSIENT;

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

    public void setClientCertKeyAlias(@Nullable String clientCertKeyAlias) {
        _clientCertKeyAlias = clientCertKeyAlias;
    }

    public Goid getClientCertKeystoreId() {
        return _clientCertKeystoreId;
    }

    public void setClientCertKeystoreId(@Nullable Goid clientCertKeystoreId) {
        _clientCertKeystoreId = clientCertKeystoreId;
    }

    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setClientCertKeystoreId(long clientCertKeystoreId) {
        _clientCertKeystoreId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, clientCertKeystoreId);
    }

    public String getDirectory() {
        return _directory;
    }

    public void setDirectory(String directory) {
        _directory = directory;
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

    public void setPassword(@Nullable String password) {
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
    @SuppressWarnings("UnusedDeclaration")
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

    public MessageTargetableSupport get_requestTarget() {
        return _requestTarget != null ? _requestTarget : defaultRequestTarget();
    }

    public void setRequestTarget(MessageTargetableSupport _requestTarget) {
        this._requestTarget = _requestTarget;
    }

    @NotNull
    public MessageTargetableSupport getResponseTarget() {
        return _responseTarget;
    }

    public void setResponseTarget(@NotNull final MessageTargetableSupport responseTarget) {
        this._responseTarget = responseTarget;
    }

    public FtpCommand getFtpCommand() {
        return ftpCommand;
    }

    public void setFtpCommand(@Nullable FtpCommand ftpCommand) {
        this.ftpCommand = ftpCommand;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SECURE_PASSWORD)
    public Goid getPasswordGoid() {
        return passwordGoid;
    }

    public void setPasswordGoid(@Nullable Goid passwordGoid) {
        this.passwordGoid = passwordGoid;
    }

    public boolean isCommandFromVariable() {
        return useCommandVariable;
    }

    public void setCommandFromVariable(boolean useCommandVariable) {
        this.useCommandVariable = useCommandVariable;
    }

    public String getOtherFtpCommand() {
        return ftpCommandVariable;
    }

    public void setFtpCommandVariable(@Nullable String ftpCommandVariable) {
        this.ftpCommandVariable = ftpCommandVariable;
    }

    public String getResponseByteLimit() {
        return responseByteLimit;
    }

    public void setResponseByteLimit(String responseByteLimit) {
        this.responseByteLimit = responseByteLimit;
    }

    public int getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(int failureMode) {
        this.failureMode = failureMode;
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return _requestTarget == null || TargetMessageType.REQUEST == _requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return false;
    }

    @Override
    public boolean needsInitializedResponse() {
        return _requestTarget != null && TargetMessageType.RESPONSE == _requestTarget.getTarget();
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

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();

        props.put(CP_MAX_CONC, new String[] {
                "Maximum number of threads that can be used in the thread pool.  " +
                        "This is a global limit across all such assertions. (default=64)",
                "64"
        });

        props.put(CP_CORE_CONC, new String[] {
                "Core number of threads that can be used in the thread pool.  " +
                        "This is a soft limit that may be temporarily exceeded if necessary. " +
                        "This is a global limit across all such assertions. (default=32)",
                "32"
        });

        props.put(CP_MAX_QUEUE, new String[] {
                "Maximum number of working threads in the thread pool.  " +
                        "This is a global limit across all such assertions. (default=64)",
                "64"
        });

        props.put(CP_BINDING_TIMEOUT, new String[] {
                "Time interval during which if ftp connection is not used, will terminate.  " +
                        "Time out interval for ftp connection. (default=30000)",
                "30000"
        });

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);
        meta.put(WSP_EXTERNAL_NAME, "FtpRoutingAssertion");

        final TypeMapping typeMapping = (TypeMapping) meta.get(WSP_TYPE_MAPPING_INSTANCE);

        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put("FtpRouting", typeMapping);
        }});

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new WspEnumTypeMapping(FtpSecurity.class, "security"),
            new WspEnumTypeMapping(FtpFileNameSource.class, "fileNameSource"),
            new WspEnumTypeMapping(FtpCredentialsSource.class, "credentialsSource"),
            new Java5EnumTypeMapping(FtpCommand.class, "ftpCommand")
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
        final List<String> expressions = new ArrayList<>();

        if (isCommandFromVariable()) {
            expressions.add(Syntax.getVariableExpression(ftpCommandVariable));
        }

        if (_passwordUsesContextVariables) {
            expressions.add(Syntax.getVariableExpression(_password));
        }

        expressions.add(_hostName);
        expressions.add(_port);
        expressions.add(_directory);
        expressions.add(_userName);
        expressions.add(arguments);
        expressions.add(responseByteLimit);

//        Syntax.getReferencedNames( expressions.toArray( new String[ expressions.size() ] ) );

        return _requestTarget.getMessageTargetVariablesUsed()
                .with(_responseTarget.getMessageTargetVariablesUsed())
                .withExpressions(expressions).asArray();
    }

    @Override
    public SsgKeyHeader[] getPrivateKeysUsed() {
        if (isUseClientCert()) {
            return new SsgKeyHeader[] {new SsgKeyHeader(getClientCertKeystoreId() + ":" + getClientCertKeyAlias(),
                    getClientCertKeystoreId(), getClientCertKeyAlias(), getClientCertKeyAlias())};
        }
        return null;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return _requestTarget.getMessageTargetVariablesSet().with(_responseTarget.getMessageTargetVariablesSet()).asArray();
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        if (FtpCredentialsSource.SPECIFIED.equals(_credentialsSource) && passwordGoid != null) {
            return new EntityHeader[]{new SecurePasswordEntityHeader(passwordGoid, EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name())};
        }
        return new EntityHeader[0];
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (FtpCredentialsSource.SPECIFIED.equals(_credentialsSource) && passwordGoid != null) {
            if (Goid.equals(oldEntityHeader.getGoid(), passwordGoid)) {
                passwordGoid = newEntityHeader.getGoid();
            }
        }
    }
}