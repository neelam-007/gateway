/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.ext;

import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The boot process class that registers the <code>ExtensibilitytAssertions</code> that are specified
 * in the the service property <i>custom.assertions.file</i>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CustomAssertionsBootProcess implements ServerComponentLifecycle {
    private final Logger logger = Logger.getLogger(CustomAssertionsBootProcess.class.getName());

    public CustomAssertionsBootProcess() {
    }

    public String toString() {
        return "Custom Assertions Boot Process";
    }

    public void init(ComponentConfig config) throws LifecycleException {
        fileName = config.getProperty(KEY_CONFIG_FILE);
        if (fileName == null) {
            logger.info("'" + KEY_CONFIG_FILE + "' not specified");
        }
    }

    /**
     *
     */
    public void start() {
        if (fileName == null) return;
        InputStream in = null;

        try {
            in = getClass().getResourceAsStream(fileName);
            if (in == null) {
                in = new FileInputStream(fileName);
            }
            loadExtensibilityAssertions(in);
        } catch (FileNotFoundException e) {
            logger.warning("Extensibility assertions config file '" + fileName + "'not found");
        } catch (IOException e) {
            logger.log(Level.WARNING, "I/O error reading config file '" + fileName + "'", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // swallow
                }
            }
        }
    }

    private void loadExtensibilityAssertions(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        props.keys();
        for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            String key = o.toString();
            if (key.endsWith(".class")) {
                loadSingleExtensibilityAssertion(key.substring(0, key.indexOf(".class")), props);
            }
        }
    }

    private void loadSingleExtensibilityAssertion(String baseKey, Properties properties) {
        String clientClass = null;
        String serverClass = null;
        String assertionClass = null;
        String securityManagerClass = null;
        Category category = Category.UNFILLED;

        assertionClass = (String)properties.get(baseKey + ".class");

        for (Iterator iterator = properties.keySet().iterator(); iterator.hasNext();) {
            String key = (String)iterator.next();
            if (key.startsWith(baseKey)) {
                if (key.endsWith(".client")) {
                    clientClass = (String)properties.get(key);
                } else if (key.endsWith(".server")) {
                    serverClass = (String)properties.get(key);
                } else if (key.endsWith(".security.manager")) {
                    securityManagerClass = (String)properties.get(key);
                } else if (key.endsWith(".category")) {
                    Category c = Category.asCategory((String)properties.get(key));
                    if (c != null) {
                        category = c;
                    }
                }
            }
        }

        if (serverClass == null || assertionClass == null) {
            StringBuffer sb = new StringBuffer("Incomplete extensibility assertion, skipping\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",client class=" + clientClass);
            sb.append(",server class=" + serverClass + "]");
            logger.warning(sb.toString());
            return;
        }
        try {
            Class a = Class.forName(assertionClass);
            Class ca = null;
            if (clientClass != null && !"".equals(clientClass)) {
                ca = Class.forName(clientClass);
            }
            Class sa = Class.forName(serverClass);
            SecurityManager sm = null;
            if (securityManagerClass != null) {
                sm = (SecurityManager)Class.forName(securityManagerClass).newInstance();
            }
            CustomAssertionDescriptor eh = new CustomAssertionDescriptor(baseKey, a, ca, sa, category, sm);
            CustomAssertions.register(eh);
            logger.info("Registered custom assertion " + eh);
        } catch (ClassNotFoundException e) {
            StringBuffer sb = new StringBuffer("Cannot load class(es) for extensibility assertion, skipping...\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",client class=" + clientClass);
            sb.append(",server class=" + serverClass + "]");
            logger.log(Level.WARNING, sb.toString(), e);
        } catch (Exception e) {
            StringBuffer sb = new StringBuffer("Invalid extensibility assertion, skipping...\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",client class=" + clientClass);
            sb.append(",server class=" + serverClass + "]");
            logger.log(Level.WARNING, sb.toString(), e);
        }
    }

    public void stop() {
        // nothing here
    }

    public void close() {
        // nothing here
    }

    private String fileName;
    final static String KEY_CONFIG_FILE = "custom.assertions.file";
}
