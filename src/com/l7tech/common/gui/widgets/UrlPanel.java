/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.gui.widgets;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.variable.Syntax;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author alex
 */
public class UrlPanel extends TextEntryPanel {
    public UrlPanel() {
        this("URL:", null);
    }

    public UrlPanel(String label, String initialValue) {
        super(label, "url", initialValue);
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
        if (model == null || model.length() == 0) return null;
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

}
