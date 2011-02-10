package com.l7tech.external.assertions.api3scale;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * 
 */
@ProcessesRequest
public class Api3ScaleAuthorizeAssertion extends Assertion implements SetsVariables, UsesVariables {
    protected static final Logger logger = Logger.getLogger(Api3ScaleAuthorizeAssertion.class.getName());
    public static final String PREFIX = "api3Scale";
    public static final String SUFFIX_PLAN = "plan";
    public static final String SUFFIX_USAGE = "usage";
    public static final String SUFFIX_PROVIDER_KEY = "providerKey";
    public static final String SUFFIX_APP_ID = "appId";
    public static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection( Arrays.asList(
        SUFFIX_PLAN,
        SUFFIX_USAGE,
        SUFFIX_PROVIDER_KEY,
        SUFFIX_APP_ID
    ) );

    private String privateKey;
    private String server;
    private String outputPrefix;

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }


    public String getOutputPrefix() {
        return outputPrefix;
    }

    public void setOutputPrefix(String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    public String getPrefixUsed() {
        return (outputPrefix == null || outputPrefix.trim().isEmpty()) ? PREFIX : outputPrefix;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(getPrefixUsed()+"."+SUFFIX_PLAN, false, false, null, false, DataType.STRING),
            new VariableMetadata(getPrefixUsed()+"."+SUFFIX_USAGE, false, false, null, false, DataType.STRING),
            new VariableMetadata(getPrefixUsed()+"."+SUFFIX_PROVIDER_KEY, false, false, null, false, DataType.STRING),
            new VariableMetadata(getPrefixUsed()+"."+SUFFIX_APP_ID, false, false, null, false, DataType.STRING),
            new VariableMetadata(PREFIX+"."+SUFFIX_PROVIDER_KEY, false, false, null, false, DataType.STRING),
        };
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        if (privateKey != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(privateKey)));
        if (server != null) ret.addAll(Arrays.asList(Syntax.getReferencedNames(server)));
        return ret.toArray(new String[ret.size()]);
    }
    
    //
    // Metadata
    //
    private static final String META_INITIALIZED = Api3ScaleAuthorizeAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "3 Scale Authorize");
        meta.put(AssertionMetadata.LONG_NAME, "Forwards request to 3 Scale authorize");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/api3scale/console/resources/3scale16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/api3scale/console/resources/3scale16.gif");


        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Api3Scale" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");//"(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
