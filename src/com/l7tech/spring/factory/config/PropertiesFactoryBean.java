/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.l7tech.spring.factory.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Arrays;

import org.springframework.core.io.Resource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Based on Spring <code>PropertiesFactoryBean</code>. This class differs
 * from the Spring version by supporting the resources that are not found
 * (it does not throw IOException that terminates application context initialization.
 *
 * Tis is done by setting the property <code>allowNonExistentResources</code> to
 * true (defaults to true).
 *
 * If no resources have been found the FileNotFoundException is raised.
 *
 * @author Emil Marceta
 */
public class PropertiesFactoryBean extends AbstractFactoryBean {

    private Properties properties;

    private Resource[] locations;

    private String fileEncoding;

    private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

    private boolean allowNonExistentResources = true;


    /**
     * Set local properties, e.g. via the "props" tag in XML bean definitions.
     * These can be considered defaults, to be overridden by properties
     * loaded from files.
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Set a location of a properties file to be loaded.
     */
    public void setLocation(Resource location) {
        this.locations = new Resource[]{location};
    }

    /**
     * Set locations of properties files to be loaded.
     */
    public void setLocations(Resource[] locations) {
        this.locations = locations;
    }

    /**
     * Set the encoding to use for parsing properties files.
     * Default is none, using java.util.Properties' default encoding.
     *
     * @see org.springframework.util.PropertiesPersister#load
     */
    public void setFileEncoding(String encoding) {
        this.fileEncoding = encoding;
    }

    /**
     * Set the PropertiesPersister to use for parsing properties files.
     * The default is DefaultPropertiesPersister.
     *
     * @see org.springframework.util.DefaultPropertiesPersister
     */
    public void setPropertiesPersister(PropertiesPersister propertiesPersister) {
        this.propertiesPersister = propertiesPersister;
    }


    public Class getObjectType() {
        return Properties.class;
    }

    protected Object createInstance() throws Exception {
        return mergeProperties();
    }

    /**
      * Set the flag whether the not found resources are allowed.
      * Default is true.
      *
      * @see org.springframework.util.PropertiesPersister#load
      */
     public void setAllowNonExistentResources(boolean allowNonExistentResources) {
         this.allowNonExistentResources = allowNonExistentResources;
     }


    /**
     * Return a merged Properties instance containing both the
     * loaded properties and properties set on this FactoryBean.
     */
    protected Properties mergeProperties() throws IOException {
        if (this.properties == null && this.locations == null) {
            throw new IllegalArgumentException("Either properties or location(s) must be set");
        }

        Properties result = new Properties();

        if (this.properties != null) {
            // use propertyNames enumeration to also catch default properties
            for (Enumeration en = this.properties.propertyNames(); en.hasMoreElements();) {
                String key = (String)en.nextElement();
                result.setProperty(key, this.properties.getProperty(key));
            }
        }

        if (this.locations != null) {
            loadProperties(result);
        }

        return result;
    }

    /**
     * Load properties into the given instance.
     *
     * @throws java.io.IOException in case of I/O errors
     * @see #setLocations
     */
    protected void loadProperties(Properties props) throws IOException {
        boolean atLeastOneExists = false;
        for (int i = 0; i < this.locations.length; i++) {
            Resource location = this.locations[i];
            if (!location.exists() && allowNonExistentResources) {
                if (logger.isInfoEnabled()) {
                    logger.info("properties " + location+ " does not exist, skipping");
                }
                continue;
            }
            atLeastOneExists = true;
            if (logger.isInfoEnabled()) {
                logger.info("Loading properties file from " + location);
            }
            InputStream is = location.getInputStream();
            try {
                if (this.fileEncoding != null) {
                    this.propertiesPersister.load(props, new InputStreamReader(is, this.fileEncoding));
                } else {
                    this.propertiesPersister.load(props, is);
                }
            } finally {
                is.close();
            }
        }
        if (!atLeastOneExists) {
            throw new FileNotFoundException("All the resources are missing "+Arrays.asList(locations));
        }
    }
}
