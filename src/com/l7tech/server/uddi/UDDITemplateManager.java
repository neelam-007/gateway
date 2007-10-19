package com.l7tech.server.uddi;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.beans.XMLDecoder;
import java.lang.reflect.Proxy;

import com.l7tech.server.ServerConfig;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.MappedMethodInvocationHandler;
import com.l7tech.common.uddi.UDDIRegistryInfo;

/**
 * Manager for UDDI templates.
 *
 * <p>A UDDI template specifys configuration for a particular (type of) UDDI
 * registry.</p>
 * 
 * @author Steve Jones
 */
public class UDDITemplateManager {

    //- PUBLIC

    /**
     * Create a new UDDI template manager.
     *
     * @param serverConfig The server configuration to use
     */
    public UDDITemplateManager(final ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.templates = Collections.unmodifiableMap(loadTemplatesFromFile());
    }

    /**
     * Get the (unique) names for the UDDI templates.
     *
     * @return The template names (never null)
     */
    public Collection<String> getTemplateNames() {
        return new ArrayList(templates.keySet());
    }

    /**
     * Get a template by name.
     *
     * @param name The name of the template
     * @return The template Map or null if there is no such template
     */
    public Map<String,Object> getTemplate(final String name) {
        return templates.get(name);
    }

    /**
     * Get the collection of template (Map)
     *
     * @return The collection of templates (never null)
     */
    public Collection<Map<String,Object>> getTemplates() {
        return new ArrayList(templates.values());
    }

    /**
     * Get the collection of UDDI templates as UDDIRegistryInfos
     *
     * @return The collection of UDDIRegistryInfo (never null)
     */
    public Collection<UDDIRegistryInfo> getTemplatesAsUDDIRegistryInfo() {
        return toUDDIRegistryInfo(getTemplates());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(UDDITemplateManager.class.getName());
    private static final int NAME_SUFFIX_LENGTH = 4;
    private static final String PROP_NAME = "name";

    private final ServerConfig serverConfig;
    private final Map<String,Map<String,Object>> templates;

    /**
     *
     */
    private Map<String,Map<String,Object>> loadTemplatesFromFile() {
        Map<String,Map<String,Object>> templates = new TreeMap<String, Map<String,Object>>();

        String rootPath = serverConfig.getPropertyCached(ServerConfig.PARAM_UDDI_TEMPLATES);
        File rootFile = new File(rootPath);
        if ( !rootFile.exists() ) {
            logger.warning("UDDI templates not available.");
        } else {                
            File[] templateFiles = rootFile.listFiles(new XmlFileFilter());
            for (File templateFile : templateFiles) {

                String name = buildName(templateFile.getName());

                try {
                    Map<String, Object> template = loadTemplate(name, templateFile);
                    templates.put(name, template);
                } catch (FileNotFoundException e) {
                    logger.log(Level.WARNING, "Could not open file '" + templateFile.getAbsolutePath() + "'.", e);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Could not read file '" + templateFile.getAbsolutePath() + "'.", e);
                }
            }
        }

        return templates;
    }

    /**
     * Construct a display name from a file name.
     */
    private String buildName(final String filename) {
        String name = filename.substring(0, filename.length() - NAME_SUFFIX_LENGTH);
        return name.replace('_', ' ');
    }

    /**
     *
     */
    private Map<String,Object> loadTemplate(final String name,
                                            final File file) throws IOException {
        Map<String,Object> template = new HashMap();

        FileInputStream fis = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            fis = new FileInputStream(file);
            XMLDecoder decoder = new java.beans.XMLDecoder(fis);

            Map props = (Map<String, Object>) decoder.readObject();
            
            template.putAll(props);
            template.put(PROP_NAME, name);

            logger.config("Loaded UDDI template from file '" + file.getAbsolutePath() + "'.");
        } finally {
            ResourceUtils.closeQuietly(fis);
        }

        return template;
    }

    /**
     *
     */
    private Collection<UDDIRegistryInfo> toUDDIRegistryInfo(Collection<Map<String,Object>> templates) {
        List<UDDIRegistryInfo> registryInfos = new ArrayList();

        for ( Map<String,Object> template : templates ) {
            registryInfos.add(toUDDIRegistryInfo(template));
        }

        return registryInfos;
    }

    /**
     *
     */
    private UDDIRegistryInfo toUDDIRegistryInfo(Map<String,Object> template) {
        return (UDDIRegistryInfo) Proxy.newProxyInstance(
                UDDITemplateManager.class.getClassLoader(),
                new Class[]{UDDIRegistryInfo.class},
                new MappedMethodInvocationHandler(UDDIRegistryInfo.class, template));
    }

    /**
     * FilenameFilter for XML files
     */
    private static class XmlFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            boolean accept = false;

            if (name.length() > NAME_SUFFIX_LENGTH && name.endsWith(".xml")) {
                accept = true;
            }

            return accept;
        }
    }
}
