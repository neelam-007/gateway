package com.l7tech.server.uddi;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Collections;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URL;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.MappedMethodInvocationHandler;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ClassUtils;
import com.l7tech.util.Config;
import com.l7tech.uddi.UDDIRegistryInfo;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;

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
     * @param config The configuration to use
     */
    public UDDITemplateManager(final Config config) {
        this.config = config;
        this.templates = Collections.unmodifiableMap(loadTemplates());
    }

    /**
     * Get the (unique) names for the UDDI templates.
     *
     * @return The template names (never null)
     */
    public Collection<String> getTemplateNames() {
        return new ArrayList<String>(templates.keySet());
    }

    /**
     * Get a template by name.
     *
     * @param name The name of the template
     * @return The template Map or null if there is no such template
     */
    public Map<String,Object> getTemplate(final String name) {
        UDDITemplate template = templates.get(name);
        return template != null ? template.toClientMap() : null;
    }

    /**
     * Get a UDDI template by name.
     *
     * @param name The name of the template
     * @return The template Map or null if there is no such template
     */
    public UDDITemplate getUDDITemplate(final String name) {
        return templates.get(name);
    }

    /**
     * Get the collection of template (Map)
     *
     * @return The collection of templates (never null)
     */
    public Collection<Map<String,Object>> getTemplates() {
        List<Map<String,Object>> templateList = new ArrayList<Map<String,Object>>();
        for ( UDDITemplate template : templates.values() ) {
            templateList.add( template.toClientMap() );           
        }
        return templateList;
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

    private final Config config;
    private final Map<String,UDDITemplate> templates;

    private Map<String,UDDITemplate> loadTemplates() {
        Map<String,UDDITemplate> standardTemplates = loadTemplateResources();
        Map<String,UDDITemplate> fileTemplates = loadTemplatesFromFile();

        // file templates override standard templates
        standardTemplates.putAll(fileTemplates);

        return standardTemplates;
    }

    /**
     *
     */
    private Map<String,UDDITemplate> loadTemplateResources() {
        final Map<String,UDDITemplate> templates = new TreeMap<String, UDDITemplate>();

        Collection<URL> resourceUrls = Collections.emptyList();
        try {
            resourceUrls = ClassUtils.listResources( UDDITemplateManager.class, "uddiTemplates/templates.index" );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not list UDDI template resources.", e);
        }

        for ( URL resourceUrl : resourceUrls ) {
            final String filePath = resourceUrl.getPath();
            if ( filePath.endsWith(".xml") ) {
                String name = buildName(filePath);            
                try {
                    UDDITemplate template = loadTemplate(name, resourceUrl);
                    templates.put(name, template);

                    logger.config("Loaded UDDI template resource '" + resourceUrl.getPath() + "'.");
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Could not read file '" + resourceUrl.getPath() + "'.", e);
                }
            }
        }

        return templates;
    }

    /**
     *
     */
    private Map<String,UDDITemplate> loadTemplatesFromFile() {
        final Map<String,UDDITemplate> templates = new TreeMap<String, UDDITemplate>();

        final String rootPath = config.getProperty( ServerConfigParams.PARAM_UDDI_TEMPLATES );
        if ( rootPath != null ) {
            logger.config("Loading UDDI templates from directory '"+rootPath+"'.");

            File rootFile = new File(rootPath);
            if ( !rootFile.exists() ) {
                logger.warning("UDDI templates not available.");
            } else {
                File[] templateFiles = rootFile.listFiles(new XmlFileFilter());
                if ( templateFiles != null ) {
                    for (File templateFile : templateFiles) {
                        String name = buildName(templateFile.getName());
                        try {
                            UDDITemplate template = loadTemplate(name, templateFile.toURI().toURL());
                            templates.put(name, template);

                            logger.config("Loaded UDDI template from file '" + templateFile.getAbsolutePath() + "'.");
                        } catch (FileNotFoundException e) {
                            logger.log(Level.WARNING, "Could not open file '" + templateFile.getAbsolutePath() + "'.", e);
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "Could not read file '" + templateFile.getAbsolutePath() + "'.", e);
                        }
                    }
                }
            }
        }

        return templates;
    }

    /**
     * Construct a display name from a file name.
     */
    private String buildName( final String filename ) {
        String name = filename.substring(0, filename.length() - NAME_SUFFIX_LENGTH);

        if ( name.indexOf('/') >= 0 ) {
            name = name.substring( name.lastIndexOf('/') + 1 );            
        }

        return name.replace('_', ' ');
    }

    /**
     *
     */
    @SuppressWarnings({"unchecked"})
    private UDDITemplate loadTemplate( final String name,
                                       final URL url ) throws IOException {
        UDDITemplate template;

        InputStream is = null;
        try {
            is = url.openStream();

            JAXBContext context = JAXBContext.newInstance("com.l7tech.server.uddi");
            Unmarshaller unmarshaller = context.createUnmarshaller();

            template = (UDDITemplate) unmarshaller.unmarshal( is );
            template.setName( name );
        } catch (JAXBException e) {
            throw new CausedIOException("Error loading template '"+name+"'.", e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }

        return template;
    }

    /**
     *
     */
    private Collection<UDDIRegistryInfo> toUDDIRegistryInfo( final Collection<Map<String,Object>> templates ) {
        final List<UDDIRegistryInfo> registryInfos = new ArrayList<UDDIRegistryInfo> ();

        for ( Map<String,Object> template : templates ) {
            registryInfos.add(toUDDIRegistryInfo(template));
        }

        return registryInfos;
    }

    /**
     *
     */
    private UDDIRegistryInfo toUDDIRegistryInfo( final Map<String,Object> template ) {
        return (UDDIRegistryInfo) Proxy.newProxyInstance(
                UDDITemplateManager.class.getClassLoader(),
                new Class[]{UDDIRegistryInfo.class},
                new MappedMethodInvocationHandler(UDDIRegistryInfo.class, template));
    }

    /**
     * FilenameFilter for XML files
     */
    private static class XmlFileFilter implements FilenameFilter {
        @Override
        public boolean accept( final File dir, final String name ) {
            boolean accept = false;

            if (name.length() > NAME_SUFFIX_LENGTH && name.endsWith(".xml")) {
                accept = true;
            }

            return accept;
        }
    }
}
