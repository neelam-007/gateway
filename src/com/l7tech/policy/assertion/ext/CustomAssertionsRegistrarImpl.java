package com.l7tech.policy.assertion.ext;

import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.server.ComponentConfig;
import com.l7tech.server.ServerConfig;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.ServerException;
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
        // todo: pass the server config in ctor - em
        init(ServerConfig.getInstance());
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

    /**
     * Resolve the policy with the custom assertions support for a
     * given service. The server is asked will resolve registered
     * custom elements.
     *
     * @param eh the netity header representing the service
     * @return the policy tree
     * @throws RemoteException         on remote invocation error
     * @throws ObjectNotFoundException if the service cannot be found
     */
    public Assertion resolvePolicy(EntityHeader eh)
      throws RemoteException, ObjectNotFoundException {
        if (!EntityType.SERVICE.equals(eh.getType())) {
            throw new IllegalArgumentException("type " + eh.getType());
        }
        ServiceManager sm = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        if (sm == null) {
            throw new IllegalStateException("Cannot get service manager");
        }
        try {
            PublishedService svc = sm.findByPrimaryKey(eh.getOid());
            if (svc == null) {
                throw new ObjectNotFoundException("service not found, " + eh);
            }
            return WspReader.parse(svc.getPolicyXml());
        } catch (FindException e) {
            ServerException se = new ServerException("Internal server error " + e.getMessage());
            se.initCause(e);
            throw se;
        } catch (IOException e) {
            ServerException se = new ServerException("Internal server error " + e.getMessage());
            se.initCause(e);
            throw se;
        }
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
    private void init(ComponentConfig config) {
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
        } catch (FileNotFoundException e) {
            logger.info("Custom assertions config file '" + fileName + "'not present\nNo custom assertions will be loaded");
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
