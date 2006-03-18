/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author alex
 */
public class RegexPanel extends TextEntryPanel {
    public RegexPanel(String label, String initialValue) {
        super(label, "pattern", initialValue);
    }

    protected String getSyntaxError(String text) {
        try {
            Pattern.compile(text);
            return null;
        } catch (PatternSyntaxException e) {
            return e.toString();
        }
    }

    protected String getSemanticError(String text) {
        // No distinction between syntax and semantics for regex
        return null;
    }
}
