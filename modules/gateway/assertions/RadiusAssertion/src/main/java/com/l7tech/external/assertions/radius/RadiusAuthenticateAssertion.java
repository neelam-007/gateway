package com.l7tech.external.assertions.radius;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.VariableUseSupport.expressions;


/**
 *
 */
public class RadiusAuthenticateAssertion extends Assertion implements MessageTargetable, UsesVariables, SetsVariables, UsesEntities {
    private final static String baseName = "Authenticate Against Radius Server";
    protected static final Logger logger = Logger.getLogger(RadiusAuthenticateAssertion.class.getName());

    public static final String DEFAULT_PREFIX = "radius";

    //Server Configuration
    private String host;
    private Goid secretGoid;
    private String authPort;
    private String acctPort;
    private String timeout;

    //Authenticate Setting
    private Map<String, String> attributes = new HashMap<String, String>();
    private String authenticator;
    private String prefix;
    protected final MessageTargetableSupport messageTargetableSupport;


    public RadiusAuthenticateAssertion() {
        this( TargetMessageType.REQUEST );
    }

    public RadiusAuthenticateAssertion(TargetMessageType defaultTargetMessageType) {
        this.messageTargetableSupport = new MessageTargetableSupport(defaultTargetMessageType, false);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Goid getSecretGoid() {
        return secretGoid;
    }

    public void setSecretGoid(Goid secretGoid) {
        this.secretGoid = secretGoid;
    }

    public String getAuthPort() {
        return authPort;
    }

    public void setAuthPort(String authPort) {
        this.authPort = authPort;
    }

    public String getAcctPort() {
        return acctPort;
    }

    public void setAcctPort(String acctPort) {
        this.acctPort = acctPort;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String[] getVariablesUsed() {

        final List<String> expressions = new ArrayList<String>();

        if (!attributes.isEmpty()) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                expressions.add(attribute.getValue());
            }
        }
        expressions.add(host);
        expressions.add(authPort);
        expressions.add(timeout);

        return expressions(expressions.toArray(new String[expressions.size()])).asArray();

    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        if(secretGoid!=null){
            return new EntityHeader[]{new EntityHeader(secretGoid, EntityType.SECURE_PASSWORD,null,null)};
        }
        return new EntityHeader[0];
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(secretGoid!=null &&
            oldEntityHeader.getType().equals(EntityType.SECURE_PASSWORD) &&
            oldEntityHeader.getGoid().equals(secretGoid) &&
            newEntityHeader.getType().equals(EntityType.SECURE_PASSWORD))
        {
            secretGoid = newEntityHeader.getGoid();
        }

    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    //
// Metadata
//
    private static final String META_INITIALIZED = RadiusAuthenticateAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, baseName);

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/user16.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/user16.png");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Radius" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.radius.console.RadiusAuthenticationPropertiesDialog");

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.radius.server.RadiusModuleLoadListener");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.radius.console.RadiusAuthenticateAssertionValidator");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (getPrefix() != null) {
            return new VariableMetadata[]{
                new VariableMetadata(getPrefix(), true, false, null, false, DataType.STRING)};
        } else {
            return new VariableMetadata[0];
        }
    }

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RadiusAuthenticateAssertion>(){
        @Override
        public String getAssertionName( final RadiusAuthenticateAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder name = new StringBuilder(assertion.getTargetName() + ": " + baseName);
            name.append(" " + assertion.getHost());
            name.append(" [");
            name.append(assertion.getPrefix());
            name.append(']');
            return name.toString();
        }
    };

    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherMessageVariable);
    }

    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return false;
    }
}
