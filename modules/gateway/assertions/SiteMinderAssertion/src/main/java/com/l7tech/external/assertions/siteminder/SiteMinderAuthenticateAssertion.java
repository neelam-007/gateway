package com.l7tech.external.assertions.siteminder;

import com.l7tech.external.assertions.siteminder.util.SiteMinderAssertionUtil;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 *
 */
public class SiteMinderAuthenticateAssertion extends Assertion implements MessageTargetable, UsesVariables, SetsVariables {

    public static final String DEFAULT_PREFIX = "siteminder";

    private String cookieSourceVar;
    private boolean useSMCookie;
    private String prefix;
    private boolean isLastCredential = true;
    private String namedUser;
    private String namedCertificate;
    private String ssoZoneName;
    protected final MessageTargetableSupport messageTargetableSupport;
    private boolean sendUsernamePasswordCredential = true;
    private boolean sendX509CertificateCredential = false;
    private boolean createSsoToken = true;
    private String namedJsonWebToken;
    private boolean sendJWT = false;

    public SiteMinderAuthenticateAssertion() {
        this( TargetMessageType.REQUEST );
    }

    public SiteMinderAuthenticateAssertion(TargetMessageType defaultTargetMessageType) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType, false);
    }

    public boolean isLastCredential() {
        return isLastCredential;
    }

    public void setLastCredential(boolean lastCredential) {
        isLastCredential = lastCredential;
    }

    public String getCookieSourceVar() {
        return cookieSourceVar;
    }

    public void setCookieSourceVar(String cookieSourceVar) {
        this.cookieSourceVar = cookieSourceVar;
    }

    public boolean isUseSMCookie() {
        return useSMCookie;
    }

    public void setUseSMCookie(boolean useSMCookie) {
        this.useSMCookie = useSMCookie;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isSendUsernamePasswordCredential() { return this.sendUsernamePasswordCredential; }

    public void setSendUsernamePasswordCredential(boolean sendUnPw) { this.sendUsernamePasswordCredential = sendUnPw; }

    public boolean isSendX509CertificateCredential() { return this.sendX509CertificateCredential; }

    public void setSendX509CertificateCredential(boolean sendX509 ) { this.sendX509CertificateCredential = sendX509; }

    public String getNamedUser() { return this.namedUser; }

    public void setNamedUser(String username) { this.namedUser = username; }

    public String getNamedCertificate() { return this.namedCertificate; }

    public void setNamedCertificate(String certificateName) { this.namedCertificate = certificateName; }

    public boolean isSendJWT() {
        return this.sendJWT;
    }

    public void setSendJWT(boolean sendJWT) {
        this.sendJWT = sendJWT;
    }

    public String getNamedJsonWebToken() {
        return this.namedJsonWebToken;
    }

    public void setNamedJsonWebToken(String jsonWebToken) {
        this.namedJsonWebToken = jsonWebToken;
    }

    public String getSsoZoneName() {
        return ssoZoneName;
    }

    public void setSsoZoneName(String ssoZoneName) {
        this.ssoZoneName = ssoZoneName;
    }

    // For compatibility: read old policy with Login element
    public void setLogin(String login) { setNamedUser(login); }

    //bug SSG-13072
    public boolean isCreateSsoToken() {
        return createSsoToken;
    }

    public void setCreateSsoToken(boolean createSsoToken) {
        this.createSsoToken = createSsoToken;
    }


    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> varsUsed = new ArrayList<>();
        varsUsed.add(prefix + ".smcontext");

        if (useSMCookie && StringUtils.isNotEmpty(cookieSourceVar)) {
            varsUsed.add(cookieSourceVar);
        }

        if (!isLastCredential) {
            String[] refNames = Syntax.getReferencedNames(namedUser);
            varsUsed.addAll(Arrays.asList(refNames));
            refNames = Syntax.getReferencedNames(namedCertificate);
            varsUsed.addAll(Arrays.asList(refNames));
            varsUsed.addAll(Arrays.asList(Syntax.getReferencedNames(namedJsonWebToken)));
        }

        if (createSsoToken && StringUtils.isNotEmpty(ssoZoneName)) {
            varsUsed.addAll(Arrays.asList(Syntax.getReferencedNames(ssoZoneName)));
        }

        return varsUsed.toArray(new String[varsUsed.size()]);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = SiteMinderAuthenticateAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Authenticate user against CA Single Sign-On Policy Server");

        // Add to palette folder
        //   accessControl,
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/user16.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/user16.png");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:SiteMinder" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.siteminder.console.SiteMinderAuthenticationPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, baseName + " Properties");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertionValidator");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /**
     * Get a description of the variables this assertion sets.  The general expectation is that these
     * variables will exist and be assigned values after the server assertion's checkRequest method
     * has returned.
     * <p/>
     * If an assertion requires a variable to already exist, but modifies it in-place, it should delcare it
     * in both SetsVariables and {@link com.l7tech.policy.assertion.UsesVariables}.
     * <p/>
     * The following example changes <strong>are not</strong> considered as Message modifications for the purposes of this contract:
     * <ul>
     * <li>Changes to a Message's associated AuthenticationContext within the ProcessingContext.</li>
     * <li>Read-only access to a Message's MIME body or parts, even if this might internally require reading and stashing the message bytes.</li>
     * <li>Read-only access to an XML Message, even if this might internally require parsing the document.</li>
     * <li>Reading transport level headers or pending response headers.</li>
     * <li>Checking current pending decoration requirements .</li>
     * <li>Matching an XPath against a document, or validating its schema.</li>
     * </ul>
     * <p/>
     * The following example changes <strong>are</strong> considered as Message modifications for the purposes of this contract:
     * <ul>
     * <li>Changes to the message content type, MIME body, or parts, including by total replacement</li>
     * <li>Changes to an XML document</li>
     * <li>Addition of pending decoration requirements, even if decoration is not performed immediately.</li>
     * <li>Applying an XSL transformation.</li>
     * </ul>
     *
     * @return an array of VariableMetadata instances.  May be empty, but should never be null.
     * @throws com.l7tech.policy.variable.VariableNameSyntaxException
     *          (unchecked) if one of the variable names
     *          currently configured on this object does not use the correct syntax.
     */
    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {new VariableMetadata(getPrefix() + "." + SiteMinderAssertionUtil.SMCONTEXT, true, false, null, false, DataType.BINARY)};
    }

    /**
     * The type of message this assertion targets.  Defaults to {@link com.l7tech.policy.assertion.TargetMessageType#REQUEST}. Never null.
     */
    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    /**
     * The type of message this assertion targets.  Defaults to {@link com.l7tech.policy.assertion.TargetMessageType#REQUEST}. Never null.
     */
    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    /**
     * If {@link #getTarget} is {@link com.l7tech.policy.assertion.TargetMessageType#OTHER}, the name of some other message-typed variable to use as
     * this assertion's target.
     */
    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    /**
     * If {@link #getTarget} is {@link com.l7tech.policy.assertion.TargetMessageType#OTHER}, the name of some other message-typed variable to use as
     * this assertion's target.
     */
    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherMessageVariable);
    }

    /**
     * A short, descriptive name for the target, i.e. "request", "response" or {@link #getOtherTargetMessageVariable()}
     * <p/>
     * <p>Almost all MessageTargetable implementations will never return null,
     * in a few null is necessary for backwards compatibility.</p>
     *
     * @return the target name or null if no target is set.
     */
    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    /**
     * @return true if the target message might be modified; false if the assertion only reads the target message.
     */
    @Override
    public boolean isTargetModifiedByGateway() {
        return false;
    }

    private final static String baseName = "Authenticate Against CA Single Sign-On";
    private static final int MAX_DISPLAY_LENGTH = 80;

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SiteMinderAuthenticateAssertion>(){
        @Override
        public String getAssertionName( final SiteMinderAuthenticateAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuffer name = new StringBuffer(assertion.getTargetName() + ": " + baseName + ": [");
            name.append(assertion.getPrefix());
            name.append(']');
            if(name.length() > MAX_DISPLAY_LENGTH) {
                name = name.replace(MAX_DISPLAY_LENGTH - 1, name.length() - 1, "...");
            }
            return name.toString();
        }
    };
}
