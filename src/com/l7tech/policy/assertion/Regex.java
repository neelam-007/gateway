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
    public static final int MAX_LENGTH = 1024 * 512;

    private String regex;
    private String replacement;
    private boolean caseInsensitive;
    private boolean replace;
    /**
     * Which MIME part to do the replacement in. Use null to indicate the first (SOAP)
     * part or any other Integer (zero-based) for a specific MIME part.
     */
    private int mimePart = 0;

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

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Test whether the replace has been requested
     * @return boolean true if replace requested, false otherwise
     */
    public boolean isReplace() {
        return replace;
    }

    /**
     * Set hte boolean tooggle that enables replace
     * @param replace
     */
    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    /**
     * @return the mime part of the this regex will match against
     */
    public int getMimePart() {
        return mimePart;
    }

    /**
     * Set the mime part index of the message that this regex will match against
     * @param mimePart
     */
    public void setMimePart(int mimePart) {
        this.mimePart = mimePart;
    }

}
