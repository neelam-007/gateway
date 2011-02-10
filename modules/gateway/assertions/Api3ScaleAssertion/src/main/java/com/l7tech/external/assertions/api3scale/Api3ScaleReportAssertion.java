package com.l7tech.external.assertions.api3scale;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * 
 */
public class Api3ScaleReportAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(Api3ScaleReportAssertion.class.getName());

    private Api3ScaleTransaction[] transactions = new Api3ScaleTransaction[0];
    
    public Api3ScaleTransaction[] getTransactions(){
        return transactions;
    }

    public void setTransactions(Api3ScaleTransaction[] transactions){
        this.transactions = transactions;    
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        for(Api3ScaleTransaction transaction : transactions){
            ret.addAll(Arrays.asList(Syntax.getReferencedNames(transaction.getAppId())));
            ret.addAll(Arrays.asList(Syntax.getReferencedNames(transaction.getTimestamp())));
            Set <String> keys = transaction.getMetrics().keySet();
            for(String key: keys )
            {
                ret.addAll(Arrays.asList(Syntax.getReferencedNames(key)));
                ret.addAll(Arrays.asList(Syntax.getReferencedNames(transaction.getMetrics().get(key))));
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = Api3ScaleReportAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "3 Scale Report");
        meta.put(AssertionMetadata.LONG_NAME, "Forwards request to 3 Scale report");

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
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");// "(fromClass)");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new ArrayTypeMapping(new Api3ScaleTransaction[0], "transactions"));
        othermappings.add(new BeanTypeMapping(Api3ScaleTransaction.class, "transaction"));
        othermappings.add(new MapTypeMapping());
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public Api3ScaleReportAssertion clone() {
        Api3ScaleReportAssertion cloned = (Api3ScaleReportAssertion) super.clone();
        cloned.transactions = cloned.transactions.clone();
        for ( int i=0; i<cloned.transactions.length; i++  ) {
            cloned.transactions[i] = cloned.transactions[i].clone();
        }
        return cloned;
    }

}
