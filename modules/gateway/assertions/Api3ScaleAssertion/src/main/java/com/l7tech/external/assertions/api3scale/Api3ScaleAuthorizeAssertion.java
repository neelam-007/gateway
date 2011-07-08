package com.l7tech.external.assertions.api3scale;

import com.l7tech.external.assertions.api3scale.server.Api3ScaleAdminImpl;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * 
 */
@ProcessesRequest
public class Api3ScaleAuthorizeAssertion extends Assertion implements SetsVariables, UsesVariables {
    protected static final Logger logger = Logger.getLogger(Api3ScaleAuthorizeAssertion.class.getName());

    private static final String DEFAULT_APP_KEY = "${request.http.header.app_key}";
    private static final String DEFAULT_APP_ID = "${request.http.header.app_id}";

    private String privateKey;


    private String server;
    private String outputPrefix = "apiAuthorize";
    private String applicationKey = DEFAULT_APP_KEY;
    private String applicationID = DEFAULT_APP_ID;
    private Map<String,String> usage = null;

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }

    public Map<String,String> getUsage() {
        return usage;
    }

    public void setUsage(Map<String,String> usage) {
        this.usage = usage;
    }

    public String getOutputPrefix() {
        return outputPrefix;
    }

    public void setOutputPrefix(String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    public String getPrefix() {
        return outputPrefix;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(getPrefix(), false, false, null, false, DataType.MESSAGE)
            };
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();

        String[] serverRefNames;
        if (this.server != null){
            serverRefNames = Syntax.getReferencedNames(this.server);
            if(serverRefNames!=null){
                ret.addAll(Arrays.asList(serverRefNames));
            }
        }

        String[] privateKeyRefNames;
        if (this.privateKey != null){
            privateKeyRefNames = Syntax.getReferencedNames(this.privateKey);
            if(privateKeyRefNames!=null){
                ret.addAll(Arrays.asList(privateKeyRefNames));
            }
        }

        String[] applicationKeyRefNames;
        if (this.applicationKey != null){
            applicationKeyRefNames = Syntax.getReferencedNames(this.applicationKey);
            if(applicationKeyRefNames!=null){
                ret.addAll(Arrays.asList(applicationKeyRefNames));
            }
        }

        String[] applicationIDRefNames;
        if (this.applicationID != null){
            applicationIDRefNames = Syntax.getReferencedNames(this.applicationID);
            if(applicationIDRefNames!=null){
                ret.addAll(Arrays.asList(applicationIDRefNames));
            }
        }

        if (this.usage != null){
            for(String key: usage.keySet()){
                String[] keyRefNames;
                keyRefNames = Syntax.getReferencedNames(key);
                if(keyRefNames!=null){
                    ret.addAll(Arrays.asList(keyRefNames));
                }
                String[] valueRefNames;
                valueRefNames = Syntax.getReferencedNames(usage.get(key));
                if(valueRefNames!=null){
                    ret.addAll(Arrays.asList(valueRefNames));
                }
            }
        }

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
        meta.put(AssertionMetadata.SHORT_NAME, "API Authorize");
        meta.put(AssertionMetadata.LONG_NAME, "Authorizes managed API application ");

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
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary< Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding(Api3ScaleAdmin.class, null, new Api3ScaleAdminImpl());
                return Collections.singletonList(binding);
            }
        });


        return meta;
    }

}
