package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.ProcessesMultipart;

/**
 * Extracts MIME parts from the current request and transforms them into fields from an HTML form submission.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current request and replaces it
 * with new content!
 */
@ProcessesMultipart
@ProcessesRequest
public class InverseHttpFormPost extends Assertion {
    private String[] fieldNames = new String[0];

    public String[] getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }

    private final static String baseName = "Translate MIME to HTTP Form";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<InverseHttpFormPost>(){
        @Override
        public String getAssertionName( final InverseHttpFormPost assertion, final boolean decorate) {
            return baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Translate one or more parts of a Message into an HTTP Form submission.");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/network.gif");

        //not needed right now, but adding in case the assertion's name will need to be decorated later
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.InverseHttpFormPostPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/network.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "MIME to HTTP Form Translation Properties");

        return meta;
    }
}
