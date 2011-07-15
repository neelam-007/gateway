package com.l7tech.external.assertions.ssh;

import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE;

/**
 *  Route outbound SCP & SFTP to external SSH server.
 */
public class SshRouteAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {

    public static final String LISTEN_PROP_ENABLE_SCP = "l7.ssh.enableScp";
    public static final String LISTEN_PROP_ENABLE_SFTP = "l7.ssh.enableSftp";
    public static final String LISTEN_PROP_HOST_PRIVATE_KEY = "l7.ssh.hostPrivateKey";
    public static final String LISTEN_PROP_MAX_CONCURRENT_SESSIONS_PER_USER = "l7.ssh.maxConcurrentSessionsPerUser";

    /** Timeout (in milliseconds) when opening SSH Connection. */
    public static final int DEFAULT_TIMEOUT = 10000;

    /** Default port for SSH */
    public static final int DEFAULT_SSH_PORT = 22;

    /** Username. Can contain context variables. */
    private String username;

    /** privateKey. Can contain context variables. */
    private String privateKey;

    /** password. Can contain context variables. */
    private Long passwordOid = null;

    /** SSH server host name. Can contain context variables. */
    private String hostName;

    /** Port number. Can contain context variables. */
    private String port = Integer.toString(DEFAULT_SSH_PORT);

     /** Destination directory pattern. Can contain context variables. */
    private String directory;

    /** SSH Public Key. Can contain context variables. */
    private String sshPublicKey;

    private boolean usePrivateKey = false;
    private boolean usePublicKey = false;

    /** Where the file name on server will come from. */
    private FtpFileNameSource fileNameSource;

    /** File name pattern if {@link #fileNameSource} is {@link com.l7tech.gateway.common.transport.ftp.FtpFileNameSource#PATTERN}; can contain context variables. */
    private String fileNamePattern;

    /** Timeout for opening connection to FTP server (in milliseconds). */
    private int timeout = DEFAULT_TIMEOUT;

    private final static String baseName = "Route via SSH2";

    private MessageTargetableSupport requestTarget = defaultRequestTarget();

    private MessageTargetableSupport defaultRequestTarget() {
        return new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    }
    //
    // Metadata
    //
    private static final String META_INITIALIZED = SshRouteAssertion.class.getName() + ".metadataInitialized";
    protected static final Logger logger = Logger.getLogger(SshRouteAssertion.class.getName());

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.ssh.server.SshServerModuleLoadListener");

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Route requests from the Gateway to another server, using SSH. This assertion supports the SSH2 protocol for SCP and SFTP transfer.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ssh.console.SshRouteAssertionPropertiesPanel");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SSH Route Assertion Properties");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<SshRouteAssertion>() {
            @Override
            public String getAssertionName(final SshRouteAssertion assertion, final boolean decorate) {
                if(!decorate) return baseName;

                final StringBuilder sb = new StringBuilder("Route via SSH2 to Server ");
                sb.append(assertion.getHost());
                return AssertionUtils.decorateName(assertion, sb.toString());
            }
        });
        meta.put(WSP_EXTERNAL_NAME, "SshRouteAssertion");
        final TypeMapping typeMapping = (TypeMapping)meta.get(WSP_TYPE_MAPPING_INSTANCE);
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put("SshRoute", typeMapping);
        }});

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new WspEnumTypeMapping(FtpSecurity.class, "security"),
            new WspEnumTypeMapping(FtpFileNameSource.class, "fileNameSource"),
            new WspEnumTypeMapping(FtpCredentialsSource.class, "credentialsSource")
        )));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:SshRouting" rather than "set:modularAssertions"
        // meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return requestTarget.getVariablesSet();
    }

     public String getPropertiesDialogTitle() {
        return String.valueOf(meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME));
    }

    public String getHost() {
        return hostName;
    }

    public void setHost(String host) {
        this.hostName = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public boolean isUsePrivateKey() {
        return usePrivateKey;
    }

    public void setUsePrivateKey(boolean usePrivateKey) {
        this.usePrivateKey = usePrivateKey;
    }

    public boolean isUsePublicKey() {
        return usePublicKey;
    }

    public void setUsePublicKey(boolean usePublicKey) {
        this.usePublicKey = usePublicKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

     public String getSshPublicKey() {
        return sshPublicKey;
    }

    public void setSshPublicKey(String sshPublicKey) {
        this.sshPublicKey = sshPublicKey;
    }

    public Long getPasswordOid() {
        return passwordOid;
    }

    public void setPasswordOid(Long passwordOid) {
        this.passwordOid = passwordOid;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public FtpFileNameSource getFileNameSource() {
        return fileNameSource;
    }

    public void setFileNameSource(FtpFileNameSource fileNameSource) {
        this.fileNameSource = fileNameSource;
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

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        Set<String> vars = new HashSet<String>();
        if (hostName != null) vars.addAll(Arrays.asList(Syntax.getReferencedNames(hostName)));
        if (port != null) vars.addAll(Arrays.asList(Syntax.getReferencedNames(port)));
        if (directory != null) vars.addAll(Arrays.asList(Syntax.getReferencedNames(directory)));
        if (username != null) vars.addAll(Arrays.asList(Syntax.getReferencedNames(username)));
        if (fileNamePattern != null) vars.addAll(Arrays.asList(Syntax.getReferencedNames(fileNamePattern)));
        vars.addAll(Arrays.asList(requestTarget.getVariablesUsed()));
        
        return vars.toArray(new String[vars.size()]);
    }
}
