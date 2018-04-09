package com.l7tech.external.assertions.email;

import com.l7tech.external.assertions.email.server.EmailAdminImpl;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.*;
import com.l7tech.search.Dependency;
import com.l7tech.util.*;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * An assertion that sends an email base64message.
 */
public class EmailAssertion extends Assertion implements UsesVariables {

    public static final String DEFAULT_HOST = "mail";
    public static final String DEFAULT_SUBJECT = "CA API Gateway Email";
    public static final String DEFAULT_FROM = "L7SSG@NOMAILBOX";

    private String targetEmailAddress = "";
    private String targetCCEmailAddress = "";
    private String targetBCCEmailAddress = "";
    private String sourceEmailAddress = DEFAULT_FROM;
    private String smtpHost = DEFAULT_HOST;
    private String smtpPort = Integer.toString(EmailProtocol.PLAIN.getDefaultSmtpPort());
    private String subject = DEFAULT_SUBJECT;
    private String base64message = "";
    private EmailProtocol protocol = EmailProtocol.PLAIN;
    private EmailFormat format = EmailFormat.PLAIN_TEXT;
    private List<EmailAttachment> attachments = new ArrayList<>();
    private boolean authenticate = false;
    private boolean contextVarPassword = false;
    private String authUsername;
    private String authPassword;
    private boolean isTestBean = false;

    public EmailAssertion() {
    }

    public boolean isTestBean(){
        return isTestBean;
    }

    public void setIsTestBean(boolean isTestBean){
        this.isTestBean = isTestBean;
    }

    public String getTargetEmailAddress() {
        return targetEmailAddress;
    }

    public void setTargetEmailAddress(String targetEmailAddress) {
        if (targetEmailAddress == null) throw new NullPointerException();
        this.targetEmailAddress = targetEmailAddress;
    }

    public String getTargetCCEmailAddress() {
        return targetCCEmailAddress;
    }

    public void setTargetCCEmailAddress(String targetCCEmailAddress) {
        if (targetCCEmailAddress == null) throw new NullPointerException();
        this.targetCCEmailAddress = targetCCEmailAddress;
    }

    public String getTargetBCCEmailAddress() {
        return targetBCCEmailAddress;
    }

    public void setTargetBCCEmailAddress(String targetBCCEmailAddress) {
        if (targetBCCEmailAddress == null) throw new NullPointerException();
        this.targetBCCEmailAddress = targetBCCEmailAddress;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        if (smtpHost == null) throw new NullPointerException();
        this.smtpHost = smtpHost;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    @Deprecated
    public void setSmtpPort(int smtpPort) {
        this.smtpPort = Integer.toString(smtpPort);
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        if (subject == null) subject = "";
        this.subject = subject;
    }

    public String getBase64message() {
        return base64message;
    }

    public void setBase64message(String message) {
        if (message == null) message = "";
        base64message = message;
    }

    /**
     * This method exists to maintain the backward compatibility for old style email assertion
     * @param text body text
     */
    @Deprecated
    public void setMessage(final String text) {
        messageString(text);
    }

    public String messageString() {
        return new String(HexUtils.decodeBase64(base64message, true), Charsets.UTF8);
    }

    public void messageString(String text) {
        setBase64message(HexUtils.encodeBase64(HexUtils.encodeUtf8(text), true));
    }

    /**
     * @return the source email address.  May be empty but never null.
     */
    public String getSourceEmailAddress() {
        return sourceEmailAddress;
    }

    public void setSourceEmailAddress(String sourceEmailAddress) {
        if (sourceEmailAddress == null) sourceEmailAddress = DEFAULT_FROM;
        this.sourceEmailAddress = sourceEmailAddress;
    }

    public EmailProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(EmailProtocol protocol) {
        this.protocol = protocol;
    }

    public EmailFormat getFormat() {
        return format;
    }

    public void setFormat(final EmailFormat format) {
        this.format = format;
    }
  
    public List<EmailAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public void setAttachments(final List<EmailAttachment> attachments) {
        this.attachments.clear();
        if (attachments != null) {
            this.attachments.addAll(attachments);
        }
    }
  
    public boolean isAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(boolean authenticate) {
        this.authenticate = authenticate;
    }

    public boolean isContextVarPassword() {
        return contextVarPassword;
    }

    public void setContextVarPassword(boolean contextVarPassword) {
        this.contextVarPassword = contextVarPassword;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    @WspSensitive
    @Dependency(methodReturnType = Dependency.MethodReturnType.VARIABLE, type = Dependency.DependencyType.SECURE_PASSWORD)
    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        final List<String> varsUsedList = new ArrayList<>();

        addUsedVariablesTo(varsUsedList,
                Syntax.getReferencedNames(
                        messageString(),
                        smtpPort,
                        smtpHost,
                        targetEmailAddress,
                        targetBCCEmailAddress,
                        targetCCEmailAddress,
                        subject,
                        sourceEmailAddress,
                        authUsername,
                        contextVarPassword ? authPassword : null
                ));
        addUsedVariablesInAttachmentsTo(varsUsedList);

        return varsUsedList.toArray(new String[varsUsedList.size()]);
    }

    private void addUsedVariablesInAttachmentsTo(final List<String> varsUsedList) {
        for (EmailAttachment item : attachments) {
            addUsedVariablesTo(varsUsedList, Syntax.getReferencedNames(item.getName()));
            varsUsedList.add(item.getSourceVariable());
        }
    }

    private void addUsedVariablesTo(final List<String> varsUsedList, final String[] vars) {
        for (String item : vars) {
            varsUsedList.add(item);
        }
    }

    private static final String META_INITIALIZED = EmailAssertion.class.getName() + ".metadataInitialized";
    private static final String ASSERTION_NAME = "Send Email";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<EmailAssertion>() {
        /**
         * The Assertion name will be seen as "Send Email in HTML Format with 1 attachment" in the Policy Manager.
         * @param assertion Assertion to generate a name for. Cannot be null
         * @param decorate if true, the implementation can decorate the name with values which are specific to the state
         * of the Assertion and it's location in a policy
         * @return String Display name
         */
        @Override
        public String getAssertionName(final EmailAssertion assertion, final boolean decorate) {
            if(!decorate) return ASSERTION_NAME;

            final StringBuilder name = new StringBuilder(ASSERTION_NAME + " in ");
            name.append(assertion.getFormat().getDescription());
            name.append(" format");
            if (!assertion.getAttachments().isEmpty()) {
                name.append(" with ").append(assertion.getAttachments().size());
                if(assertion.getAttachments().size() == 1) {
                    name.append(" attachment");
                } else {
                    name.append(" attachments");
                }
            }
            return AssertionUtils.decorateName(assertion, name);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, ASSERTION_NAME);
        meta.put(DESCRIPTION, "Send an email message to predetermined recipients.");

        meta.put(PALETTE_FOLDERS, new String[]{"audit"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.email.console.EmailPropertiesDialog");
        meta.put(PROPERTIES_ACTION_NAME, "Email Properties");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(EmailProtocol.class, "Protocol"),
                new Java5EnumTypeMapping(EmailFormat.class, "Format"),
                new CollectionTypeMapping(List.class, EmailAttachment.class, ArrayList.class, "Attachments"),
                new BeanTypeMapping(EmailAttachment.class, "Attachment")
        )));

        meta.put(EXTENSION_INTERFACES_FACTORY,
                (Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>) appContext ->
                        Collections.singletonList(new ExtensionInterfaceBinding<>(EmailAdmin.class, null, new EmailAdminImpl())));

        // Old External name must be handled for backward compatibility.
        final Map<String, TypeMapping> typeMap = new HashMap<>();
        typeMap.put("EmailAlert", (TypeMapping) meta.get(WSP_TYPE_MAPPING_INSTANCE));
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, typeMap);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
