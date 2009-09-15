package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * Asserts that a message being processed is identifiable as for a specific operation based on a service's WSDL.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 */
@RequiresSOAP()
public class Operation extends Assertion {
    private String operationName;

    public Operation() {
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    private final static String baseName = "Evaluate WSDL Operation";
    
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<Operation>(){
        @Override
        public String getAssertionName( final Operation assertion, final boolean decorate) {
            if(!decorate) return baseName;
            final String wsdlName = (assertion.getOperationName() != null)? assertion.getOperationName(): "undefined";
            return baseName + " \'" + wsdlName + "\'";
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Enforce a particular operation as defined in the service WSDL.");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Information16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.OperationPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WSDL Operation Properties");
        return meta;
    }
}
