/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.gui.widgets.TextEntryPanel;
import com.l7tech.policy.variable.Syntax;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author alex
 */
public class UrlPanel extends TextEntryPanel {
    private boolean emptyUrlAllowed;

    public UrlPanel() {
        this("URL:", null, true);
    }

    public UrlPanel(String label, String initialValue) {
        this(label, initialValue, true);
    }

    public UrlPanel(String label, String initialValue, boolean emptyUrlAllowed) {
        super(label, "url", initialValue);
        this.emptyUrlAllowed = emptyUrlAllowed;
    }

    protected String getSemanticError(String model) {
        if (model == null || model.length() == 0) return null;
        // if the URL contains context variable, you just can't check semantic
        String[] tmp = Syntax.getReferencedNames(model);
        if (tmp != null && tmp.length > 0) {
            return null;
        }
        try {
            URL url = new URL(model);
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(url.getHost());
            return null;
        } catch (SecurityException se) {
            // if we are not permitted to resolve the address don't show
            // this as an error
            return null;
        } catch (Exception e) {
            return ExceptionUtils.getMessage(e);
        }
    }

    protected String getSyntaxError(String model) {
        if (model == null || model.trim().length() == 0) {
            if (emptyUrlAllowed) return null;
            else return "empty URL";
        }
        // if the URL contains context variable, you just can't check syntax
        String[] tmp = Syntax.getReferencedNames(model);
        if (tmp != null && tmp.length > 0) {
            return null;
        }
        try {
            URL url = new URL(model);
            if (url.getHost() == null || url.getHost().length() == 0) {
                return "no host";
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            return ExceptionUtils.getMessage(e);
        }
    }

    public boolean isEmptyUrlAllowed() {
        return emptyUrlAllowed;
    }

    public void setEmptyUrlAllowed(boolean emptyUrlAllowed) {
        this.emptyUrlAllowed = emptyUrlAllowed;
    }
}
