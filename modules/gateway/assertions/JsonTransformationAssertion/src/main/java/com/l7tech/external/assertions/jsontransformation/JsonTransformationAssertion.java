package com.l7tech.external.assertions.jsontransformation;

import com.l7tech.external.assertions.jsontransformation.server.JsonTransformationAdminImpl;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.*;


@ProcessesRequest
public class JsonTransformationAssertion  extends MessageTargetableAssertion {
	
	static public enum Transformation {
        XML_to_JSON, JSON_to_XML
    }

    private String rootTagString;
    private MessageTargetableSupport destinationMessageTarget;


    private Transformation transform = JsonTransformationAssertion.Transformation.XML_to_JSON;

    public JsonTransformationAssertion() {
        super(TargetMessageType.RESPONSE, false);
        destinationMessageTarget = new MessageTargetableSupport(TargetMessageType.REQUEST, true);
    }

	public void setRootTagString(String rootTagString) {
		this.rootTagString = rootTagString;
	}

	public String getRootTagString() {
		return rootTagString;
	}

    public MessageTargetableSupport getDestinationMessageTarget() {
        return destinationMessageTarget;
    }

    public void setDestinationMessageTarget(MessageTargetableSupport requestTarget) {
        this.destinationMessageTarget = requestTarget;
        if (destinationMessageTarget != null)
            destinationMessageTarget.setTargetModifiedByGateway(true);
    }

    public Transformation getTransformation() {
        return transform;
    }

    public void setTransformation(Transformation transform) {
        this.transform = transform;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( rootTagString );
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().with( destinationMessageTarget==null ? null : destinationMessageTarget.getMessageTargetVariablesSet() );
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = JsonTransformationAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Apply JSON Transformation");
        meta.put(AssertionMetadata.LONG_NAME, "Transform messages from XML to JSON, or from JSON to XML." );

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME,"JSON Transformation Properties");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");


        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:JsonTransformation" rather than "set:modularAssertions"   "(fromClass)"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        Collection<TypeMapping> otherMappings = new ArrayList<TypeMapping>();
        otherMappings.add(new Java5EnumTypeMapping(JsonTransformationAssertion.Transformation.class, "transformation"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(otherMappings));

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary< Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final ExtensionInterfaceBinding<JsonTransformationAdmin> binding = new ExtensionInterfaceBinding<JsonTransformationAdmin>(
                        JsonTransformationAdmin.class,
                        null,
                        new JsonTransformationAdminImpl());
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        return meta;
    }


}
