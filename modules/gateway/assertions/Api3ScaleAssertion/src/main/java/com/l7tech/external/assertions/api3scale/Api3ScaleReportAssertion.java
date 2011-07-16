package com.l7tech.external.assertions.api3scale;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;

import java.util.*;
import java.util.logging.Logger;

/**
 * 
 */
public class Api3ScaleReportAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(Api3ScaleReportAssertion.class.getName());
    private static final String DEFAULT_APP_ID = "${request.http.header.app_id}";

    private String privateKey;
    private String applicationId = DEFAULT_APP_ID;
    private String server;
    private Map<String,String> transactionUsages = new HashMap<String,String>();

    public void setTransactionUsages(Map<String,String> usages) {
        this.transactionUsages = usages;
    }
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Map<String,String> getTransactionUsages() {
        return transactionUsages;
    }

    public String getApplicationId() {
        return applicationId;
    }

        public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setServer (String server){
        this.server = server;
    }

    public String getServer (){
        return server;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();

        ret.addAll( Arrays.asList( Syntax.getReferencedNames( privateKey, server, applicationId ) ) );

        if ( this.transactionUsages != null ){
            for( final Map.Entry<String,String> entry : transactionUsages.entrySet() ){
                ret.addAll( Arrays.asList( Syntax.getReferencedNames( entry.getKey(), entry.getValue() ) ) );
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = Api3ScaleReportAssertion.class.getName() + ".metadataInitialized";

    @Override
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
        meta.put(AssertionMetadata.SHORT_NAME, "API Report");
        meta.put(AssertionMetadata.LONG_NAME, "Reports managed API application usage");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/api3scale/console/resources/3scale16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/api3scale/console/resources/3scale16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Api3Scale" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }


}
