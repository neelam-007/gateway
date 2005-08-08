package com.l7tech.policy.assertion;

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

    private String comment;
}
