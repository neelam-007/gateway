package com.l7tech.server.identity.ldap;

import com.l7tech.common.io.IOUtils;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.server.ServerConfig;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ClassUtils;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;

/**
 * Provides partially configured LdapIdenityProviderConfig to simplify the addition of a new connector.
 * <p/>
 * Once the template is chosen, it can be used to create a new config object.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 */
public class LdapConfigTemplateManager {

    //- PUBLIC

    public LdapConfigTemplateManager() {
        initTemplates();
    }

    /**
     * returns the names of the templates
     *
     * @return an array of template names that can be used in getTemplate()
     */
    public String[] getTemplateNames() {
        Set keys = templates.keySet();
        String[] output = new String[keys.size()];
        int i = 0;
        for (Iterator it = keys.iterator(); it.hasNext(); i++) {
            output[i] = (String)it.next();
        }
        return output;
    }

    public LdapIdentityProviderConfig[] getTemplates() {
        Collection values = templates.values();
        LdapIdentityProviderConfig[] output = new LdapIdentityProviderConfig[values.size()];
        int i = 0;
        for (Iterator it = values.iterator(); it.hasNext(); i++) {
            output[i] = (LdapIdentityProviderConfig)it.next();
        }
        return output;
    }

    /**
     * get a template, given its name. the actual template is not returned but a new config object
     * based on this template.
     * 
     * @return the config object if found, null if not found
     */
    public LdapIdentityProviderConfig getTemplate(final String templateName) throws IOException {
        // clone the template, so it's not affected by whatever the user is doing with it
        LdapIdentityProviderConfig cfg = templates.get(templateName);
        if (cfg == null) return null;
        return new LdapIdentityProviderConfig(cfg);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(LdapConfigTemplateManager.class.getName());
    private static Map<String,LdapIdentityProviderConfig> templates;

    private void initTemplates() {
        if ( templates == null ) {
            templates = loadTemplates();
        }
    }

    private Map<String,LdapIdentityProviderConfig> loadTemplates() {
        Map<String,LdapIdentityProviderConfig> standardTemplates = loadTemplateResources();
        Map<String,LdapIdentityProviderConfig> fileTemplates = loadTemplatesFromFile();

        // file templates override standard templates
        standardTemplates.putAll(fileTemplates);

        return standardTemplates;
    }

    private Map<String,LdapIdentityProviderConfig> loadTemplateResources() {
        Map<String,LdapIdentityProviderConfig> templates = new HashMap<String,LdapIdentityProviderConfig>();

        Collection<URL> resourceUrls = Collections.emptyList();
        try {
            resourceUrls = ClassUtils.listResources( LdapConfigTemplateManager.class, "ldapTemplates/templates.index" );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not list UDDI template resources.", e);
        }

        for ( URL resourceUrl : resourceUrls ) {
            try {
                String name = resourceUrl.toString();
                if ( name.endsWith(".xml") ) {
                    name = name.substring(0, name.length() - 4);
                    int index = name.lastIndexOf('/');
                    if ( index >= 0 ) {
                        name = name.substring( index + 1 );
                    }
                    LdapIdentityProviderConfig template = loadTemplate(resourceUrl, name );
                    templates.put(template.getName(), template);
                    logger.config("Loaded LDAP template resource '" + template.getName() + "'.");
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Could not load LDAP template resource '" + resourceUrl.toString() + "'.", ioe);
            }
        }

        return templates;
    }

    private Map<String,LdapIdentityProviderConfig> loadTemplatesFromFile() {
        Map<String,LdapIdentityProviderConfig> templates = new HashMap<String,LdapIdentityProviderConfig>();

        String rootPath = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_LDAP_TEMPLATES);
        if ( rootPath != null ) {
            logger.config("Loading LDAP templates from directory '"+rootPath+"'.");

            File rootFile = new File(rootPath);
            if ( !rootFile.exists() ) {
                logger.warning("templates not available!");
            } else {
                String[] output = rootFile.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".xml");
                    }
                });
                if ( output != null ) {
                    for ( String anOutput : output ) {
                        File templatesFile = new File( rootFile, anOutput );
                        try {
                            String name = anOutput.substring(0, anOutput.length() - 4);
                            LdapIdentityProviderConfig template = loadTemplate( templatesFile.toURI().toURL(), name );
                            templates.put(template.getName(), template);
                            logger.config("Loaded LDAP template '" + template.getName() + "'.");
                        } catch (IOException ioe) {
                            logger.log(Level.WARNING, "Could not load LDAP template file '" + templatesFile.getAbsolutePath() + "'.", ioe);
                        }
                    }
                }
            }
        }

        return templates;
    }

    private LdapIdentityProviderConfig loadTemplate( final URL templateUrl, String name ) throws IOException {
        InputStream is = null;
        try {
            is = templateUrl.openStream();
            byte[] data = IOUtils.slurpStream(is);
            String properties = new String(data);

            LdapIdentityProviderConfig template = new LdapIdentityProviderConfig();
            template.setName(name);
            template.setSerializedProps(properties);
            template.setTemplateName(template.getName());

            return template;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new CausedIOException("Error reading object", aioobe);
        } catch (NoSuchElementException nsee) {
            throw new CausedIOException("Error reading object", nsee);
        } finally {
            ResourceUtils.closeQuietly( is );
        }
    }

}
