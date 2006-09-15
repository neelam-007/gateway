/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.gui.widgets;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.variable.ExpandVariables;

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

    protected String getSemanticError(Object model) {
        if (model == null || model.toString().length() == 0) return null;
        // if the URL contains context variable, you just can't check semantic
        String[] tmp = ExpandVariables.getReferencedNames(model.toString());
        if (tmp != null && tmp.length > 0) {
            return null;
        }
        try {
            URL url = new URL(model.toString());
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(url.getHost());
            return null;
        } catch (Exception e) {
            return ExceptionUtils.getMessage(e);
        }
    }

    protected String getSyntaxError(Object model) {
        if (model == null || model.toString().length() == 0) return null;
        // if the URL contains context variable, you just can't check syntax
        String[] tmp = ExpandVariables.getReferencedNames(model.toString());
        if (tmp != null && tmp.length > 0) {
            return null;
        }
        try {
            URL url = new URL(model.toString());
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
