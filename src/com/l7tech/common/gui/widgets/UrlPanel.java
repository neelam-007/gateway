/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.gui.widgets;

import com.l7tech.common.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class UrlPanel extends TextEntryPanel {
    private static final Logger logger = Logger.getLogger(UrlPanel.class.getName());

    public UrlPanel() {
        this("URL:", null);
    }

    public UrlPanel(String label, String initialValue) {
        super(label, "url", initialValue);
    }

    protected String getSemanticError(Object model) {
        if (model == null || model.toString().length() == 0) return null;
        InputStream readme = null;
        try {
            URLConnection conn = new URL(model.toString()).openConnection();
            conn.connect();
            readme = conn.getInputStream();
            return null;
        } catch (Exception e) {
            return ExceptionUtils.getMessage(e);
        } finally {
            try {
                if (readme != null) readme.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Couldn't close URL stream", e);
            }
        }
    }

    protected String getSyntaxError(Object model) {
        if (model == null || model.toString().length() == 0) return null;
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
