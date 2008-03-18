package com.l7tech.external.assertions.kerberosmapping;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Assertion to define mappings between Kerberos REALM and UPN Suffix.
 *
 * <p>This assertion will set variables for use later in a policy:</p>
 *
 * <ul>
 *  <li><code>kerberos.realm</code> - The REALM part of the Kerberos Principal (Client)</li>
 *  <li><code>kerberos.enterprisePrincipal</code> - "true" for enterprise principals, else "false"</li>
 * </ul>
 */
public class KerberosMappingAssertion extends Assertion implements SetsVariables, UsesVariables {

    //- PUBLIC

    public KerberosMappingAssertion() {            
    }

    public String[] getMappings() {
        return mappings;
    }

    public void setMappings( String[] mappings ) {
        this.mappings = mappings;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata("kerberos.realm", false, false, "kerberos.realm", false, DataType.STRING),
            new VariableMetadata("kerberos.enterprisePrincipal", false, false, "kerberos.enterprisePrincipal", false, DataType.STRING)
        };
    }

    public String[] getVariablesUsed() {
        return new String[0]; 
    }

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
        meta.put(AssertionMetadata.SHORT_NAME, "Kerberos Mapping");
        meta.put(AssertionMetadata.LONG_NAME, "Map Kerberos principal");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME,
                "com.l7tech.external.assertions.kerberosmapping.server.CachingLdapGroupManagerController" );

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:KerberosMapping" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    // Metadata
    private static final String META_INITIALIZED = KerberosMappingAssertion.class.getName() + ".metadataInitialized";

    // Data
    private String[] mappings;
}
