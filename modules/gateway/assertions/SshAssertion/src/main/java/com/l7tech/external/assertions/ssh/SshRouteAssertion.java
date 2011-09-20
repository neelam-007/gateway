package com.l7tech.external.assertions.ssh;

import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.ssh.console.SftpPollingListenersWindow;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE;

/**
 *  Route outbound SCP & SFTP to external SSH server.
 */
public class SshRouteAssertion extends RoutingAssertion implements UsesVariables, SetsVariables {

    public static final int DEFAULT_CONNECT_TIMEOUT = 10000;  // Timeout (in milliseconds) when opening SSH Connection.
    public static final int DEFAULT_READ_TIMEOUT = 1000 * 60;   // Timeout (in milliseconds) when reading a remote file.
    public static final int DEFAULT_SSH_PORT = 22;   // Default port for SSH

    private static final String baseName = "Route via SSH2";

    private String username;   // Username. Can contain context variables.
    private String privateKey;   // privateKey. Can contain context variables.
    private Long passwordOid = null;   // password. Can contain context variables.
    private String hostName;   // SSH server host name. Can contain context variables.
    private String port = Integer.toString(DEFAULT_SSH_PORT);   // Port number. Can contain context variables.
    private String directory;   // Destination directory. Can contain context variables.
    private String fileName;   // Destination file name. Can contain context variables.
    private String sshPublicKey;   // SSH Public Key. Can contain context variables.
    private boolean usePrivateKey = false;
    private boolean usePublicKey = false;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;   // Timeout for opening connection to SFTP server (in milliseconds).
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    private String downloadContentType;
    private boolean isScpProtocol;   // SCP? if not, assume SFTP
    private boolean isCredentialsSourceSpecified;   // login credentials specified?  if not, assume pass through
    private boolean isDownloadCopyMethod;   // download copy method?  if not, assume upload

    private MessageTargetableSupport requestTarget = defaultRequestTarget();

    private MessageTargetableSupport defaultRequestTarget() {
        return new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    }
    //
    // Metadata
    //
    private static final String META_INITIALIZED = SshRouteAssertion.class.getName() + ".metadataInitialized";
    protected static final Logger logger = Logger.getLogger(SshRouteAssertion.class.getName());

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // wire up the SFTP Polling Listener here as well (to avoid using a dummy assertion)
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.ssh.server.sftppollinglistener.SftpPollingListenerModuleLoadListener");
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { getClass().getName() + "$SftpPollingListenerCustomAction" });

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
        return requestTarget.getMessageTargetVariablesSet().asArray();
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

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }
    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
    }


    public int getReadTimeout() {
        return readTimeout;
    }
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public MessageTargetableSupport getRequestTarget() {
        return requestTarget != null ? requestTarget : defaultRequestTarget();
    }

    public void setRequestTarget(MessageTargetableSupport requestTarget) {
        this.requestTarget = requestTarget;
    }

    public String getDownloadContentType() {
        return downloadContentType;
    }
    public void setDownloadContentType(String downloadContentType) {
        this.downloadContentType = downloadContentType;
    }

    public boolean isScpProtocol() {
        return isScpProtocol;
    }
    public void setScpProtocol(boolean scpProtocol) {
        isScpProtocol = scpProtocol;
    }

    public boolean isCredentialsSourceSpecified() {
        return isCredentialsSourceSpecified;
    }
    public void setCredentialsSourceSpecified(boolean credentialsSourceSpecified) {
        isCredentialsSourceSpecified = credentialsSourceSpecified;
    }

    public boolean isDownloadCopyMethod() {
        return isDownloadCopyMethod;
    }
    public void setDownloadCopyMethod(boolean downloadCopyMethod) {
        isDownloadCopyMethod = downloadCopyMethod;
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
        return requestTarget.getMessageTargetVariablesUsed().withExpressions(
            hostName,
            port,
            directory,
            username,
            fileName
        ).asArray();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class SftpPollingListenerCustomAction extends AbstractAction {
        public SftpPollingListenerCustomAction() {
            super("Manage SFTP Polling Listeners", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Bean16.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SftpPollingListenersWindow splw = new SftpPollingListenersWindow(TopComponents.getInstance().getTopParent());
            splw.pack();
            Utilities.centerOnScreen(splw);
            DialogDisplayer.display(splw);
        }
    }
}
