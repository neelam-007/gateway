package com.l7tech.policy.assertion;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Allows admins to add comments to a policy.  No Server or Client implementations.
 */
public class CommentAssertion extends Assertion {
    public CommentAssertion() {
    }

    public CommentAssertion(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private final static String baseName = "Add Comment to Policy";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<CommentAssertion>(){
        @Override
        public String getAssertionName( final CommentAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return "Comment: " + assertion.getComment();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Insert a comment at any point in a policy.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/About16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.CommentAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Comment Properties");
        meta.put(PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/About16.gif");
        return meta;
    }

    private String comment;
}
