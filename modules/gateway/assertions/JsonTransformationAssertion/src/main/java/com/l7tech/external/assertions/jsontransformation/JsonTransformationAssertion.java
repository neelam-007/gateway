package com.l7tech.external.assertions.jsontransformation;

import com.l7tech.external.assertions.jsontransformation.server.JsonTransformationAdminImpl;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

public class JsonTransformationAssertion extends MessageTargetableAssertion {
    /**
     * Pattern to verify the root tag element name according to it's naming rule.
     */
    public static final Pattern ROOT_TAG_VERIFIER = Pattern.compile("(?i)\\A(?!XML)\\p{Alpha}\\p{Graph}*");

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

    private boolean prettyPrint;

    private boolean arrayForm;

    private boolean useNumbersWhenPossible = false;

    private TransformationConvention convention = TransformationConvention.STANDARD;

    public JsonTransformationAssertion() {
        super(TargetMessageType.RESPONSE, false);
        destinationMessageTarget = new MessageTargetableSupport(TargetMessageType.RESPONSE, true);
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

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(final boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public boolean isArrayForm() {
        return arrayForm;
    }

    public void setArrayForm(final boolean arrayForm) {
        this.arrayForm = arrayForm;
    }

    public boolean isUseNumbersWhenPossible() {
        return useNumbersWhenPossible;
    }

    public void setUseNumbersWhenPossible( boolean useNumbersWhenPossible ) {
        this.useNumbersWhenPossible = useNumbersWhenPossible;
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
}
