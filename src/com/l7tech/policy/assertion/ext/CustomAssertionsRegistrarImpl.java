package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.service.ServiceManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server side CustomAssertionsRegistrar implementation.
 *
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl
  extends ApplicationObjectSupport implements CustomAssertionsRegistrar, InitializingBean {
    static Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());
    protected static boolean initialized = false;
    private ServiceManager serviceManager;

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        Set customAssertionDescriptors = CustomAssertions.getDescriptors();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        final Set customAssertionDescriptors = CustomAssertions.getDescriptors(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     * Note that this method may not be invoked from management console. Server
     * classes may not deserialize into the ssm envirnoment.
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    public CustomAssertionDescriptor getDescriptor(Class a) {
        return CustomAssertions.getDescriptor(a);
    }

    /**
     * Return the <code>CustomAssertionUI</code> class for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion UI class or <b>null</b>
     */
    public CustomAssertionUI getUI(Class a) {
        return CustomAssertions.getUI(a);
    }


    /**
     * Resolve the policy in the xml string format with the custom assertions
     * support. The server is asked will resolve registered custom elements.
     *
     * @param xml the netity header representing the service
     * @return the policy tree
     * @throws java.rmi.RemoteException on remote invocation error
     * @throws IOException              on policy format error
     */
    public Assertion resolvePolicy(String xml) throws RemoteException, IOException {
        return WspReader.parsePermissively(xml);
    }

    private Collection asCustomAssertionHolders(final Set customAssertionDescriptors) {
        Collection result = new ArrayList();
        Iterator it = customAssertionDescriptors.iterator();
        while (it.hasNext()) {
            try {
                CustomAssertionDescriptor cd = (CustomAssertionDescriptor)it.next();
                Class ca = cd.getAssertion();
                CustomAssertionHolder ch = new CustomAssertionHolder();
                final CustomAssertion cas = (CustomAssertion)ca.newInstance();
                ch.setCustomAssertion(cas);
                ch.setCategory(cd.getCategory());
                result.add(ch);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
            }
        }
        return result;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    //-------------- custom assertion loading stuff -------------------------
      private synchronized void init(ServerConfig config) {
        if (initialized) return;
        fileName = config.getProperty(KEY_CONFIG_FILE);
        if (fileName == null) {
            logger.info("'" + KEY_CONFIG_FILE + "' not specified");
            return;
        }
        if (fileName == null) return;
        InputStream in = null;

        try {
            in = getClass().getResourceAsStream(fileName);
            if (in == null) {
                in = new FileInputStream(fileName);
            }
            loadCustomAssertions(in);
            initialized = true;
        } catch (FileNotFoundException e) {
            logger.info("Custom assertions config file '" + fileName + "'not present\n Custom assertions not loaded");
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
        String editorClass = null;
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
                } else if (key.endsWith(".ui")) {
                    editorClass = (String)properties.get(key);
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
            Class eClass = null;
            if (editorClass != null && !"".equals(editorClass)) {
                eClass = Class.forName(editorClass);
            }

            Class sa = Class.forName(serverClass);
            SecurityManager sm = null;
            if (securityManagerClass != null) {
                sm = (SecurityManager)Class.forName(securityManagerClass).newInstance();
            }
            CustomAssertionDescriptor eh = new CustomAssertionDescriptor(baseKey, a, ca, eClass, sa, category, sm);
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

    public void afterPropertiesSet() throws Exception {
        init((ServerConfig)getApplicationContext().getBean("serverConfig"));

        if (serviceManager == null) {
            throw new IllegalArgumentException("Service Manager is required");
        }
    }
}
