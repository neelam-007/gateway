/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

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

    public UrlPanel(String label, String initialValue) {
        super(label, "url", initialValue);
    }

    protected String getSemanticError(String text) {
        InputStream readme = null;
        try {
            URLConnection conn = new URL(text).openConnection();
            conn.connect();
            readme = conn.getInputStream();
            return null;
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (readme != null) readme.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Couldn't close URL stream", e);
            }
        }
    }

    protected String getSyntaxError(String text) {
        try {
            URL url = new URL(textField.getText());
            if (url.getHost() == null || url.getHost().length() == 0) {
                return "No host";
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            return e.getMessage();
        }
    }

    protected Object getModel(String text) {
        try {
            return new URL(text);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("PropertyChangeEvent was fired with invalid URL");
        }
    }
}
