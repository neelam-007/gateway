package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl extends RemoteService implements CustomAssertionsRegistrar {
    static Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());

    public CustomAssertionsRegistrarImpl(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        super(options, lifeCycle);
        try {
            // todo: pass the server config in ctor - em
            init(ServerConfig.getInstance());
            start();
        } catch (LifecycleException e) {
            logger.log(Level.WARNING, "Error loading custom assertions", e);
        }
    }

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        Set customAssertionDescriptors = CustomAssertions.getAssertions();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        final Set customAssertionDescriptors = CustomAssertions.getAssertions(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    private Collection asCustomAssertionHolders(final Set customAssertionDescriptors) {
        Collection result = new ArrayList();
        Iterator it = customAssertionDescriptors.iterator();
        while (it.hasNext()) {
            try {
                Class ca = (Class)it.next();
                CustomAssertionHolder ch = new CustomAssertionHolder();
                final CustomAssertion cas = (CustomAssertion)ca.newInstance();
                ch.setCustomAssertion(cas);
                result.add(ch);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
            }
        }
        return result;
    }


    //-------------- custom assertion loading stuff -------------------------

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
            loadCustomAssertions(in);
        } catch (FileNotFoundException e) {
            logger.warning("Custom assertions config file '" + fileName + "'not found");
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

    private void loadCustomAssertions(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        props.keys();
        for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            String key = o.toString();
            if (key.endsWith(".class")) {
                loadSingleCustomAssertion(key.substring(0, key.indexOf(".class")), props);
            }
        }
    }

    private void loadSingleCustomAssertion(String baseKey, Properties properties) {
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
            StringBuffer sb = new StringBuffer("Incomplete custom assertion, skipping\n");
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
            StringBuffer sb = new StringBuffer("Cannot load class(es) for custom assertion, skipping...\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",client class=" + clientClass);
            sb.append(",server class=" + serverClass + "]");
            logger.log(Level.WARNING, sb.toString(), e);
        } catch (Exception e) {
            StringBuffer sb = new StringBuffer("Invalid custom assertion, skipping...\n");
            sb.append("[ assertion class=" + assertionClass);
            sb.append(",client class=" + clientClass);
            sb.append(",server class=" + serverClass + "]");
            logger.log(Level.WARNING, sb.toString(), e);
        }
    }

    private String fileName;
    final static String KEY_CONFIG_FILE = "custom.assertions.file";
}
