/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion;

/**
 * @author emil
 * @version Mar 21, 2005
 */
public class Regex extends Assertion {
    private String regex;
    private String replacement;


    /**
     * Test whether the assertion is a credential source. The <code>RegexAssertion</code>
     * is never an credential source assertion.
     *
     * @return always false
     */
    public final boolean isCredentialSource() {
        return false;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}