package com.l7tech.policy.assertion.ext;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.wsp.ClassLoaderUtil;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.ModuleClassLoader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

    //- PUBLIC

    public byte[] getAssertionClass(String className) throws RemoteException {
        byte[] classData = null;

        // ensure a name such as "com.something.MyClass"
        if (className != null && className.lastIndexOf('.') > className.indexOf('.')) {
            String classAsResource = className.replace('.','/') + ".class";
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Serving custom assertion class '"+className+"' as resource '"+classAsResource+"'.");
            if (customAssertionClassloader != null) {
                InputStream classIn = null;
                try {
                    if (isCustomAssertionResource(classAsResource)) {
                        classIn = customAssertionClassloader.getResourceAsStream(classAsResource);
                        if (classIn != null)
                            classData = HexUtils.slurpStream(classIn);
                    }
                }
                catch(IOException ioe) {
                    logger.log(Level.WARNING, "Error loading custom assertion class '"+className+"'.", ioe);
                }
                finally {
                    ResourceUtils.closeQuietly(classIn);
                }
            }
        }

        return classData;
    }

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
     * @param assertionClassName the assertion class name
     * @return the custom assertion UI class or <b>null</b>
     */
    public CustomAssertionUI getUI(String assertionClassName) {
        return CustomAssertions.getUI(assertionClassName);
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

    /**
     *
     */
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * Perform bean initialization after properties are set.
     */
    public void afterPropertiesSet() throws Exception {
        if (serverConfig == null) {
            throw new IllegalArgumentException("Server Config is required");
        }

        init();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());
    private static final String KEY_CONFIG_FILE = "custom.assertions.file";
    private static final String KEY_CUSTOM_MODULES = "custom.assertions.modules";
    private static final String KEY_CUSTOM_MODULES_TEMP = "custom.assertions.temp";

    private boolean initialized = false;
    private ServerConfig serverConfig;
    private ClassLoader customAssertionClassloader;

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
                ch.setDescriptionText(cd.getDescription());
                result.add(ch);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
            }
        }
        return result;
    }

    /**
     * Custom assertion loading stuff
     */
    private void init() {
        if (initialized) return;

        String fileName = serverConfig.getPropertyCached(KEY_CONFIG_FILE);
        if (fileName == null) {
            logger.config("'" + KEY_CONFIG_FILE + "' not specified");
            return;
        }

        String moduleDirectory = serverConfig.getPropertyCached(KEY_CUSTOM_MODULES);
        String moduleWorkDirectory = serverConfig.getPropertyCached(KEY_CUSTOM_MODULES_TEMP);
        ClassLoader caClassLoader = getClass().getClassLoader();
        if (moduleDirectory == null) {
            logger.config("'" + KEY_CUSTOM_MODULES + "' not specified");
        }
        else {
            caClassLoader = new ModuleClassLoader(
                ModuleClassLoader.class.getClassLoader().getParent(),
                "customassertion",
                fileName,
                new File(moduleDirectory),
                new File(moduleWorkDirectory),
                caClassLoader);

            ClassLoaderUtil.setClassloader(caClassLoader);
        }

        customAssertionClassloader = caClassLoader;

        try {
            // Support multiple config files, so there can be one per CA Jar
            Enumeration propfileUrls = caClassLoader.getResources(fileName);
            if (propfileUrls.hasMoreElements()) {
                while(propfileUrls.hasMoreElements()) {
                    URL resourceUrl = (URL) propfileUrls.nextElement();
                    if (initFromUrl(resourceUrl, caClassLoader))
                        initialized = true;
                }
            }
            else {
                File configFile = new File(fileName);
                URL fileUrl = configFile.toURL();
                if (initFromUrl(fileUrl, caClassLoader))
                    initialized = true;
            }
        }
        catch(IOException ioe) {
            logger.log(Level.WARNING, "I/O error locating config file '" + fileName + "'", ioe);
        }
    }

    private boolean initFromUrl(URL customAssertionConfigUrl, ClassLoader classLoader) {
        boolean loaded = false;
        InputStream in = null;
        try {
            in = customAssertionConfigUrl.openStream();
            loadCustomAssertions(in, classLoader);
            loaded = true;
        }
        catch (FileNotFoundException e) {
            logger.info("Custom assertions config file '" + customAssertionConfigUrl + "'not present\n Custom assertions not loaded");
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "I/O error reading config file '" + customAssertionConfigUrl + "'", e);
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }

        return loaded;
    }

    private void loadCustomAssertions(InputStream in, ClassLoader classLoader) throws IOException {
        Properties props = new Properties();
        props.load(in);
        props.keys();

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(null);
            for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();) {
                Object o = (Object)iterator.next();
                String key = o.toString();
                if (key.endsWith(".class")) {
                    loadSingleCustomAssertion(key.substring(0, key.indexOf(".class")), props, classLoader);
                }
            }
        }
        finally {
            if (contextClassLoader != null) Thread.currentThread().setContextClassLoader(contextClassLoader);            
        }
    }

    private void loadSingleCustomAssertion(String baseKey, Properties properties, ClassLoader classLoader) {
        String clientClass = null;
        String serverClass = null;
        String assertionClass = null;
        String editorClass = null;
        String securityManagerClass = null;
        String optionalDescription = null;
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
                } else if (key.endsWith(".description")) {
                    optionalDescription = (String) properties.get(key);
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
            Class a = Class.forName(assertionClass, true, classLoader);
            Class ca = null;
            if (clientClass != null && !"".equals(clientClass)) {
                ca = Class.forName(clientClass, true, classLoader);
            }
            Class eClass = null;
            if (editorClass != null && !"".equals(editorClass)) {
                eClass = Class.forName(editorClass, true, classLoader);
            }

            Class sa = Class.forName(serverClass, true, classLoader);
            SecurityManager sm = null;
            if (securityManagerClass != null) {
                sm = (SecurityManager)Class.forName(securityManagerClass, true, classLoader).newInstance();
            }
            CustomAssertionDescriptor eh = new CustomAssertionDescriptor(baseKey, a, ca, eClass, sa, category, optionalDescription, sm);
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

    /**
     * Check if the given resource is within a known custom assertion package.
     *
     * @param resourcePath The path to check
     * @return true for a CA resource
     */
    private boolean isCustomAssertionResource(final String resourcePath) {
        boolean isCustomAssertionRes = false;

        Set descriptors = CustomAssertions.getAllDescriptors();
        for (Iterator iterator = descriptors.iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor customAssertionDescriptor = (CustomAssertionDescriptor) iterator.next();

            // only check classes relevant to the SSM (not SSB or SSG)
            Class beanClass = customAssertionDescriptor.getAssertion();
            Class uiClass = customAssertionDescriptor.getUiClass();

            if (beanClass != null && resourcePath.startsWith(beanClass.getPackage().getName().replace('.', '/'))) {
                isCustomAssertionRes = true;
                break;
            }
            if (uiClass != null && resourcePath.startsWith(uiClass.getPackage().getName().replace('.', '/'))) {
                isCustomAssertionRes = true;
                break;
            }
        }

        return isCustomAssertionRes;
    }
}
