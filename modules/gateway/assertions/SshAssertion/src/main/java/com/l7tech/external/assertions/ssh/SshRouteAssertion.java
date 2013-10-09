package com.l7tech.external.assertions.ssh;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import com.l7tech.message.CommandKnob;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurePasswordEntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_EXTERNAL_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE;

/**
 *  Route outbound SCP & SFTP to external SSH server.
 */
public class SshRouteAssertion extends RoutingAssertion implements UsesVariables, SetsVariables, UsesEntities {

    public static final int DEFAULT_CONNECT_TIMEOUT = 10;  // Timeout (in seconds) when opening SSH Connection.
    public static final int DEFAULT_READ_TIMEOUT = 60;   // Timeout (in seconds) when reading a remote file.
    public static final int DEFAULT_SSH_PORT = 22;   // Default port for SSH
    public static final long DEFAULT_FILE_OFFSET = 0;   // Default file offset. Read/write to the begining of the file.
    public static final int DEFAULT_FILE_LENGTH = -1;   // Default file length. Read/write to the end of the stream by default.
    public static final CommandKnob.CommandType DEFAULT_COMMAND_TYPE = CommandKnob.CommandType.PUT; //The default command type to select

    private static final String baseName = "Route via SSH2";
    private static final String META_INITIALIZED = SshRouteAssertion.class.getName() + ".metadataInitialized";
    private static final Logger logger = Logger.getLogger(SshRouteAssertion.class.getName());

    private String username;   // Username. Can contain context variables.
    private Goid privateKeyGoid;   // privateKey.
    private Goid passwordGoid = null;   // password.
    private String hostName;   // SSH server host name. Can contain context variables.
    private String port = Integer.toString(DEFAULT_SSH_PORT);   // Port number. Can contain context variables.
    private String directory;   // Destination directory. Can contain context variables.
    private String fileName;   // Destination file name. Can contain context variables.
    private String fileOffset = Long.toString(DEFAULT_FILE_OFFSET); // The file offset to read from or write to.
    private String fileLength = Integer.toString(DEFAULT_FILE_LENGTH); // The file length to read up to.
    private String newFileName; // The new file name to move the file to.
    private String sshPublicKey;   // SSH Public Key. Can contain context variables.
    private boolean usePrivateKey = false;
    private boolean usePublicKey = false;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;   // Timeout for opening connection to SFTP server (in seconds).
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    private String downloadContentType;
    private boolean isScpProtocol;   // SCP? if not, assume SFTP
    private boolean isCredentialsSourceSpecified;   // login credentials specified?  if not, assume pass through
    private CommandKnob.CommandType commandType = DEFAULT_COMMAND_TYPE; // The command type to execute.
    private boolean retrieveCommandTypeFromVariable = false; //should the command type to execute come from a context variable?
    private String commandTypeVariableName; //The command type context variable.
    private boolean failIfFileExists = false; //valid for PUT commands only. If true the assertion will fail if the file already exists.
    private boolean setFileSizeToContextVariable = false;
    private String saveFileSizeContextVariable="";

    private String responseByteLimit;

    private boolean isPreserveFileMetadata;

    @NotNull
    private MessageTargetableSupport requestTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    @NotNull
    private MessageTargetableSupport responseTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // wire up the SFTP Polling Listener here as well (to avoid using a dummy assertion)
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.ssh.server.sftppollinglistener.SftpPollingListenerModuleLoadListener");
        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.ssh.console.SftpPollingListenerCustomAction" });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Route requests from the Gateway to another server, using SSH. This assertion supports the SSH2 protocol for SCP and SFTP transfer.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.ssh.SshRouteAssertionAdvice");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ssh.console.SshRouteAssertionPropertiesPanel");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SSH2 Routing Properties");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<SshRouteAssertion>() {
            @Override
            public String getAssertionName(final SshRouteAssertion assertion, final boolean decorate) {
                if(!decorate) return baseName;

                final StringBuilder sb = new StringBuilder();
                sb.append(assertion.isRetrieveCommandTypeFromVariable()?"Route":assertion.getCommandType());
                sb.append(" via ");
                sb.append(assertion.isScpProtocol()?"SCP":"SFTP");
                sb.append(" to Server ");
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
            new WspEnumTypeMapping(FtpCredentialsSource.class, "credentialsSource"),
            new Java5EnumTypeMapping(CommandKnob.CommandType.class, "commandType")
        )));

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        MessageTargetableSupport.VariablesSet variables = requestTarget.getMessageTargetVariablesSet().with(responseTarget.getMessageTargetVariablesSet());
        if(setFileSizeToContextVariable){
            variables.withVariables(new VariableMetadata(saveFileSizeContextVariable));
        }
        return variables.asArray();
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

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SECURE_PASSWORD)
    public Goid getPrivateKeyGoid() {
        return privateKeyGoid;
    }

    public void setPrivateKeyGoid(@Nullable Goid privateKeyGoid) {
        this.privateKeyGoid = privateKeyGoid;
    }

    /**
     * @deprecated Secure passwords use goids now. Needed for backwards compatibility
     */
    @Deprecated
    public void setPrivateKeyOid(@Nullable Long privateKeyOid) {
        this.privateKeyGoid = GoidUpgradeMapper.mapOid(EntityType.SECURE_PASSWORD, privateKeyOid);
    }

     public String getSshPublicKey() {
        return sshPublicKey;
    }

    public void setSshPublicKey(@Nullable String sshPublicKey) {
        this.sshPublicKey = sshPublicKey;
    }

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SECURE_PASSWORD)
    public Goid getPasswordGoid() {
        return passwordGoid;
    }

    public void setPasswordGoid(@Nullable Goid passwordGoid) {
        this.passwordGoid = passwordGoid;
    }

    /**
     * @deprecated Secure passwords use goids now. Needed for backwards compatibility
     */
    @Deprecated
    public void setPasswordOid(@Nullable Long passwordOid) {
        this.passwordGoid = GoidUpgradeMapper.mapOid(EntityType.SECURE_PASSWORD, passwordOid);
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
    
    @NotNull
    public MessageTargetableSupport getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget( @NotNull final MessageTargetableSupport requestTarget ) {
        this.requestTarget = requestTarget;
    }

    @NotNull
    public MessageTargetableSupport getResponseTarget() {
        return responseTarget;
    }

    public void setResponseTarget( @NotNull final MessageTargetableSupport responseTarget ) {
        this.responseTarget = responseTarget;
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

    /**
     * @deprecated Previous versions of this assertion only allowed either upload or download. The Goatfish version
     * allows many different operations so this is deprecated.
     */
    @Deprecated
    public void setDownloadCopyMethod(boolean downloadCopyMethod) {
        commandType = downloadCopyMethod ? CommandKnob.CommandType.GET : CommandKnob.CommandType.PUT;
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return TargetMessageType.REQUEST == requestTarget.getTarget();
    }

    @Override
    public boolean initializesResponse() {
        return (CommandKnob.CommandType.GET.equals(commandType) || CommandKnob.CommandType.LIST.equals(commandType) || CommandKnob.CommandType.STAT.equals(commandType)) && TargetMessageType.RESPONSE == responseTarget.getTarget();
    }

    @Override
    public boolean needsInitializedResponse() {
        return false;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return requestTarget.getMessageTargetVariablesUsed()
        .with( responseTarget.getMessageTargetVariablesUsed() )
        .withExpressions(
            hostName,
            port,
            directory,
            username,
            fileName,
            sshPublicKey,
            responseByteLimit,
            fileOffset,
            fileLength,
            newFileName
        ).withVariables(commandTypeVariableName).asArray();
    }

    public String getResponseByteLimit() {
        return responseByteLimit;
    }

    public void setResponseByteLimit(String responseByteLimit) {
        this.responseByteLimit = responseByteLimit;
    }

    public boolean isPreserveFileMetadata() {
        return isPreserveFileMetadata;
    }

    public void setPreserveFileMetadata(final boolean preserveFileMetadata) {
        isPreserveFileMetadata = preserveFileMetadata;
    }

    public CommandKnob.CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandKnob.CommandType commandType) {
        this.commandType = commandType;
    }

    public boolean isRetrieveCommandTypeFromVariable() {
        return retrieveCommandTypeFromVariable;
    }

    public void setRetrieveCommandTypeFromVariable(boolean retrieveCommandTypeFromVariable) {
        this.retrieveCommandTypeFromVariable = retrieveCommandTypeFromVariable;
    }

    public String getCommandTypeVariableName() {
        return commandTypeVariableName;
    }

    public void setCommandTypeVariableName(String commandTypeVariableName) {
        this.commandTypeVariableName = commandTypeVariableName;
    }

    public boolean isFailIfFileExists() {
        return failIfFileExists;
    }

    public void setFailIfFileExists(boolean failIfFileExists) {
        this.failIfFileExists = failIfFileExists;
    }

    public String getFileOffset() {
        return fileOffset;
    }

    public void setFileOffset(String fileOffset) {
        this.fileOffset = fileOffset;
    }

    public String getFileLength() {
        return fileLength;
    }

    public void setFileLength(String fileLength) {
        this.fileLength = fileLength;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public void setSetFileSizeToContextVariable(boolean setFileSizeToContextVariable) {
        this.setFileSizeToContextVariable = setFileSizeToContextVariable;
    }

    public void setSaveFileSizeContextVariable(String saveFileSizeContextVariable) {
        this.saveFileSizeContextVariable = saveFileSizeContextVariable;
    }

    public boolean isSetFileSizeToContextVariable() {
        return setFileSizeToContextVariable;
    }

    public String getSaveFileSizeContextVariable() {
        return saveFileSizeContextVariable;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        if (isCredentialsSourceSpecified()) {
            if (usePrivateKey) {
                return new EntityHeader[]{new SecurePasswordEntityHeader(privateKeyGoid, EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY.name())};
            } else {
                return new EntityHeader[]{new SecurePasswordEntityHeader(passwordGoid, EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name())};
            }
        }
        return new EntityHeader[0];
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (isCredentialsSourceSpecified()) {
            if (Goid.equals(oldEntityHeader.getGoid(), privateKeyGoid)) {
                privateKeyGoid = newEntityHeader.getGoid();
            } else if (Goid.equals(oldEntityHeader.getGoid(), passwordGoid)) {
                passwordGoid = newEntityHeader.getGoid();
            }
        }
    }
}
