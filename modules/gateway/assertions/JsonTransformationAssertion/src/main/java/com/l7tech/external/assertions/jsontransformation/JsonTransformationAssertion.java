package com.l7tech.external.assertions.jsontransformation;

import com.l7tech.external.assertions.jsontransformation.server.JsonTransformationAdminImpl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.*;

public class JsonTransformationAssertion extends MessageTargetableAssertion {

    public enum Transformation {
        XML_to_JSON,
        JSON_to_XML
    }

    public enum TransformationConvention {
        /**
         * The standard convention for converting XML to JSON.  The resulting JSON will not contain attributes and namespaces.
         *
         */
        STANDARD,

        /**
         * The JSONML convention, this allows for a full round trip of XML to JSON back to XML losslessly.
         */
        JSONML
    }

    private String rootTagString;
    private MessageTargetableSupport destinationMessageTarget;
    private Transformation transform = JsonTransformationAssertion.Transformation.XML_to_JSON;

    private TransformationConvention convention = TransformationConvention.STANDARD;

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

    public TransformationConvention getConvention() {
        return convention;
    }

    public void setConvention(final TransformationConvention convention) {
        this.convention = convention;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(rootTagString);
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().with(destinationMessageTarget == null ? null : destinationMessageTarget.getMessageTargetVariablesSet());
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = JsonTransformationAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Apply JSON Transformation");
        meta.put(AssertionMetadata.LONG_NAME, "Transform messages from XML to JSON, or from JSON to XML.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "JSON Transformation Properties");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");


        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:JsonTransformation" rather than "set:modularAssertions"   "(fromClass)"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, JsonTransformationAssertion.Validator.class.getName());

        Collection<TypeMapping> otherMappings = new ArrayList<TypeMapping>();
        otherMappings.add(new Java5EnumTypeMapping(JsonTransformationAssertion.Transformation.class, "transformation"));
        otherMappings.add(new Java5EnumTypeMapping(JsonTransformationAssertion.TransformationConvention.class, "convention"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(otherMappings));

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
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

    /**
     * The assertion validator.
     */
    public static class Validator implements AssertionValidator {
        private final JsonTransformationAssertion assertion;

        public Validator(final JsonTransformationAssertion assertion) {
            if (assertion == null) {
                throw new IllegalArgumentException("assertion is required");
            }
            this.assertion = assertion;
        }

        @Override
        public void validate(final AssertionPath path, final PolicyValidationContext pvc, final PolicyValidatorResult result) {
            if(assertion.getTarget() == TargetMessageType.RESPONSE && assertion.getDestinationMessageTarget().getTarget() == TargetMessageType.REQUEST){
                result.addError(new PolicyValidatorResult.Error(assertion,
                    "Destination can not be Request when Response is selected as Source.", null));
            }
        }
    }
}
