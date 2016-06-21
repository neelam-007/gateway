package com.l7tech.server.processcontroller;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link PatcherProperties}
 */
public class PatcherPropertiesImpl implements PatcherProperties {
    private static final Logger logger = Logger.getLogger(PatcherPropertiesImpl.class.getName());

    private static final String PROPERTIES_FILE = "patcher.properties";

    private final File patcherPropertyFile;
    private static final Properties defaultPatcherProperties = new Properties() {{
        setProperty(PROP_L7P_AUTO_DELETE, String.valueOf(PROP_L7P_AUTO_DELETE_DEFAULT_VALUE));
        // add new properties here
    }};

    public PatcherPropertiesImpl(final File propertyFileLocation) {
        if (propertyFileLocation == null || !(propertyFileLocation.exists() && propertyFileLocation.isDirectory())) {
            throw new IllegalStateException("Patcher properties location is invalid: " + (propertyFileLocation != null ? propertyFileLocation.getAbsolutePath() : "<null>"));
        }
        this.patcherPropertyFile = new File(propertyFileLocation, PROPERTIES_FILE);
        if (this.patcherPropertyFile.exists() && !this.patcherPropertyFile.isFile()) {
            throw new IllegalStateException("Patcher properties file is invalid: " + this.patcherPropertyFile.getAbsolutePath());
        }
    }

    @Override
    public String getProperty(final String propertyName, final String defaultValue) throws IOException {
        final Properties props = loadPatcherProperties();
        return props.getProperty(propertyName, defaultValue);
    }

    @Override
    public synchronized String setProperty(final String propertyName, final String propertyValue) throws IOException {
        final Properties props = loadPatcherProperties();
        final Object prevValue = props.setProperty(propertyName, propertyValue);
        savePatcherProperties(props);
        return prevValue != null ? prevValue.toString() : null;
    }

    @Override
    public int getIntProperty(final String propertyName, final int defaultValue) throws IOException {
        final Properties props = loadPatcherProperties();
        final Object val = props.get(propertyName);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Bad value for patcher property: " + propertyName + ": " + ExceptionUtils.getMessage(nfe));
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanProperty(final String propertyName, final boolean defaultValue) throws IOException {
        final Properties props = loadPatcherProperties();
        boolean value = defaultValue;
        String val = props.getProperty(propertyName);
        if ( val != null ) {
            if ( "true".equalsIgnoreCase(val) ) {
                value = true;
            } else if ( "false".equalsIgnoreCase(val) ) {
                value = false;
            } else {
                logger.warning("Ignoring invalid property value ('"+val+"') for '"+propertyName+"'.");
            }
        }

        return value;
    }

    private void savePatcherProperties(final Properties patcherProperties) throws IOException {
        FileUtils.saveFileSafely(patcherPropertyFile.getPath(), true, new FileUtils.Saver() {
            @Override
            public void doSave(final FileOutputStream fos) throws IOException {
                patcherProperties.store(fos, "Patcher Properties: " + new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(new Date()));
            }
        });
    }

    private synchronized Properties loadPatcherProperties() throws IOException {
        if (!patcherPropertyFile.exists()) {
            // if the properties file doesn't exist then create a default one using default values.
            savePatcherProperties(defaultPatcherProperties);
        }

        final FileInputStream is = FileUtils.loadFileSafely(patcherPropertyFile.getPath());
        final Properties patcherProps;
        try {
            patcherProps = new Properties();
            patcherProps.load(is);
        } catch (IOException e) {
            throw new IOException("Couldn't load " + patcherPropertyFile.getAbsolutePath(), e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }

        return patcherProps;
    }
}
