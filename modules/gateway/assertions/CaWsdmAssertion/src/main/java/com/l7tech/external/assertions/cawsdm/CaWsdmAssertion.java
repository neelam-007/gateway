package com.l7tech.external.assertions.cawsdm;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.server.module.ca_wsdm.CaWsdmPropertiesAdaptor;
import com.l7tech.server.module.ca_wsdm.CaWsdmSommaPropertiesClassLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This is a pseudo-assertion that normally would not need to appear in a policy.
 * Its job is to provide a hook for the CaWsdmObserver to install itself as an application listener.
 * <p/>
 * Even though it declares no palette folders and hence does not appear in the palette, this
 * assertion can still be used to set a cluster property that can be used to ensure that the
 * CA WSDM observer is enabled on this system before taking a policy branch that assumes this
 * to be the case (for whatever local poliy reason).
 */
public class CaWsdmAssertion extends Assertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(CaWsdmAssertion.class.getName());
    public static final String VAR_ENABLED = "wsdm.enabled";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(VAR_ENABLED, false, false, null, false, DataType.BOOLEAN),
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CaWsdmAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion.  Behind an inner class because the CaWsdm code will
        // only load within the SSG.  (The SSM uses assertion metadata but never queries for CLUSTER_PROPERTIES.)
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                Map<String, String[]> props = new HashMap<String, String[]>();
                CaWsdmPropertiesAdaptor.addClusterPropertiesMetadata(props);
                return props;
            }
        });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "CA WSDM Observer Status");
        meta.put(AssertionMetadata.DESCRIPTION, "Check the status of the CA WSDM Observer");

        // This is a pseudo-assertion and so should appear in no palette folders
        meta.putNull(AssertionMetadata.PALETTE_FOLDERS);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        // Set required feature set.  We are an optional modular assertion so we depend on set:modularAssertions
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        // Subscribe our Observer to the module loading events so it can set up its application listener
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.server.module.ca_wsdm.CaWsdmObserver");

        // Hook up a classloader delegate so we can produce a virtual WsdmSOMMA_Basic.properties file
        // Behind an inner class because it will only work within the Gateway
        meta.put(AssertionMetadata.MODULE_CLASS_LOADER_DELEGATE_INSTANCE, new MetadataFinder() {
            @Override
            public Object get(AssertionMetadata meta, String key) {
                return new CaWsdmSommaPropertiesClassLoader();
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
