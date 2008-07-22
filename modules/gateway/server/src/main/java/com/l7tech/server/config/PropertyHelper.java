package com.l7tech.server.config;

import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Apr 3, 2006
 * Time: 3:07:07 PM
 */
public class PropertyHelper {
    private static final Logger logger = Logger.getLogger(PropertyHelper.class.getName());


    /**
     * Gets a list of named properties from a given properties file.
     *
     * @param propFileName the fileName for the properties file that will be used to fetch the properties. Must not be null or empty
     * @param propsToFetch a list of property names that should be fetched
     * @return a map of property propName -> property for each property that was found. Never null, but it may be empty
     * @throws IOException if reading the designated property file failed.
     */
    public static Map<String, String> getProperties(String propFileName, String[] propsToFetch) throws IOException {
        if (propFileName == null || propFileName.equalsIgnoreCase("")) {
            throw new IllegalArgumentException("The property file propName cannot be empty");
        }
        if (propsToFetch == null) {
            throw new IllegalArgumentException("List of proprties to fetch cannot be null)");
        }

        PropertiesConfiguration props = new PropertiesConfiguration();
        props.setAutoSave(false);
        props.setListDelimiter((char)0);
        
        FileInputStream fis = null;
        Map<String, String> propsReturned = new HashMap<String, String>();
        try {
            fis = new FileInputStream(propFileName);
            props.load(fis);
            for (String propName : propsToFetch) {
                String propValue = props.getString(propName);
                propsReturned.put(propName, propValue);
            }
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error reading properties from file '"+propFileName+"'.", ce);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return propsReturned;
    }

    /**
     * Edit the specfified properties file, decrypting any encrypted passwords with the specified passwordCrypto's
     * decryptor, and then reencrypting them with the passwordCrypto's encryptor.
     *
     * @param origPropsFile  the properties file to edit. Required.
     *                       If this file doesn't exist, this method returns without taking any action.
     * @param passwordCrypto a passwordCrypto to use to decrypt existing encrypted passwords and to reencrypt
     *                       them again afterwards.
     *                       <p/>
     *                       If this passwordCrypto's decryptor is null, existing encrypted passwords will not
     *                       be decrypted.
     *                       <p/>
     *                       If the encryptor is null, passwords will not be reencrypted after they are decrypted.
     *                       <p/>
     *                       If both are null, the passwords will not be changed.
     * @throws CausedIOException if there is a problem reading or writing the properties file.
     */
    public static void reencryptPasswordsInPlace(File origPropsFile, PasswordPropertyCrypto passwordCrypto)
            throws CausedIOException
    {
        if (origPropsFile == null) throw new IllegalArgumentException("The original property file cannot be null");
        if (passwordCrypto.getEncryptor() == null && passwordCrypto.getDecryptor() == null) {
            logger.fine("Neither encryption or decryption of passwords is possible - leaving properties file unchanged: " + origPropsFile.getName());
            return;
        }

        if (!origPropsFile.exists())
            return;

        PropertiesConfiguration props = new PropertiesConfiguration();
        props.setAutoSave(false);
        props.setListDelimiter((char)0);

        // Read the existing properties
        FileInputStream origFis = null;
        try {
            origFis = new FileInputStream(origPropsFile);
            props.load(origFis);
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error reading properties file '"+origPropsFile+"'.", ce);
        } catch (FileNotFoundException e) {
            throw new CausedIOException("Error reading properties file '"+origPropsFile+"'.", e);
        } finally {
            ResourceUtils.closeQuietly(origFis);
        }


        //now get all the keys and make a new properties object;
        boolean shouldSave = false;
        Iterator allProps = props.getKeys();
        Map<String, String> toMutate = new HashMap<String, String>();
        while(allProps.hasNext()) {
            String propName = (String) allProps.next();
            if (passwordCrypto.isPasswordPropertyName(propName)) {
                Object oldValue = props.getProperty(propName);
                String newValue = passwordCrypto.reencrypt(oldValue);
                if (!newValue.equals(oldValue)) {
                    toMutate.put(propName, newValue);
                }
            }
        }

        for (Map.Entry<String, String> entry : toMutate.entrySet()) {
            String propName = entry.getKey();
            String newValue = entry.getValue();
            logger.info("Re-encrypting password property " + propName);
            props.setProperty(propName, newValue);
            shouldSave = true;
        }

        if (shouldSave) {
            FileOutputStream origFos = null;

            try {
                origFos = new FileOutputStream(origPropsFile);
                props.save(origFos);
            } catch (ConfigurationException ce) {
                throw new CausedIOException("Error writing properties file '"+origPropsFile+"'.", ce);
            } catch (FileNotFoundException e) {
                throw new CausedIOException("Error writing properties file '"+origPropsFile+"'.", e);
            } finally {
                ResourceUtils.closeQuietly(origFos);
            }
        }
    }

    public static void mergePropertiesInPlace(File origPropsFile, File newPropsFile, boolean createIfNew, PasswordPropertyCrypto passwordEncryptor)
            throws CausedIOException, FileNotFoundException
    {
        if (origPropsFile == null) throw new IllegalArgumentException("The original property file cannot be null");
        if (newPropsFile == null) throw new IllegalArgumentException("The new property file cannot be null");

        if (!origPropsFile.exists() && createIfNew) {
            try {
                if (origPropsFile.createNewFile()) {
                    logger.info("Successfully created new properties file: " + origPropsFile.getName());
                }
            } catch (IOException e) {
                logger.info("Error while creating new properties file: " + origPropsFile.getName() + " (" + e.getMessage() + ")");
            }
        }

        PropertiesConfiguration newProps = new PropertiesConfiguration();
        newProps.setAutoSave(false);
        newProps.setListDelimiter((char)0);

        //load the new props
        FileInputStream newFis = null;
        try {
            newFis = new FileInputStream(newPropsFile);
            newProps.load(newFis);
        } catch (FileNotFoundException e) {
            logger.info(newPropsFile.getAbsolutePath() + " does not exist, no need to merge with existing properties");
            return;
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error reading properties file '"+newPropsFile+"'.", ce);
        } finally {
            ResourceUtils.closeQuietly(newFis);
        }

        PropertiesConfiguration origConfiguration = new PropertiesConfiguration();
        origConfiguration.setAutoSave(false);
        origConfiguration.setListDelimiter((char)0);

        //if successful, back a new properties object with these as defaults
        FileInputStream origFis = null;
        try {
            origFis = new FileInputStream(origPropsFile);
            origConfiguration.load(origFis);
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error reading properties file '"+origPropsFile+"'.", ce);
        } finally {
            ResourceUtils.closeQuietly(origFis);
        }


        //now get all the keys and make a new properties object;
        boolean shouldSave = false;
        Iterator allProps = newProps.getKeys();
        while(allProps.hasNext()) {
            String propName = (String) allProps.next();
            if (!origConfiguration.containsKey(propName)) {
                origConfiguration.addProperty(propName, newProps.getProperty(propName));
                shouldSave = true;
            }
            if (passwordEncryptor.isPasswordPropertyName(propName)) {
                Object oldValue = origConfiguration.getProperty(propName);
                String newValue = passwordEncryptor.reencrypt(oldValue);
                if (!newValue.equals(oldValue)) {
                    logger.info("Encrypting password property " + propName);
                    origConfiguration.setProperty(propName, newValue);
                    shouldSave = true;
                }
            }
        }

        if (shouldSave) {
            FileOutputStream origFos = null;

            try {
                origFos = new FileOutputStream(origPropsFile);
                origConfiguration.save(origFos);
            } catch (ConfigurationException ce) {
                throw new CausedIOException("Error writing properties file '"+origPropsFile+"'.", ce);
            } finally {
                ResourceUtils.closeQuietly(origFos);
            }
        }
    }

    /**
     * <p>Merges properties from the properties file with propName "newPropsFileName" into the properies obtained from file
     * "originalPropsFileName".</p>
     *
     * <p>Any properties that exist in both are not replaced, but instead retain the propValue within
     * "originalPropsFileName"</p>
     *
     * @param origPropsFile the original properties file. This is the base list of properties used
     *        in the merge (must not be null or empty)
     *
     * @param newPropsFile the new properties that will be merged. Only new properties from this
     *        object will be added to the list (must not be null or empty).
     * @param createIfNew whether to create the origPropsFile if it is found not to exist
     *
     * @param deleteExistingNewFile
     * @throws FileNotFoundException if the original file could not be found for reading
     * @throws IOException if there was an error while reading one of the properties files
     * @return a new Properties object (possibly empty if the original and the new file could not be found)
     * consisting of all properties contained in the two properies files. This will be equivalent to the original if
     * something failed with the new one.
     */
    public static PropertiesConfiguration mergeProperties(File origPropsFile, File newPropsFile, boolean createIfNew, boolean deleteExistingNewFile) throws IOException {
        if (origPropsFile == null) throw new IllegalArgumentException("The original property file cannot be null");
        if (newPropsFile == null) throw new IllegalArgumentException("The new property file cannot be null");

        if (!origPropsFile.exists() && createIfNew) {
            try {
                if (origPropsFile.createNewFile()) {
                    logger.info("Successfully created new properties file: " + origPropsFile.getName());
                }
            } catch (IOException e) {
                logger.info("Error while creating new properties file: " + origPropsFile.getName() + " (" + e.getMessage() + ")");
            }
        }

        PropertiesConfiguration newProps = new PropertiesConfiguration();
        newProps.setAutoSave(false);
        newProps.setListDelimiter((char)0);

        //load the new props
        FileInputStream newFis = null;
        try {
            newFis = new FileInputStream(newPropsFile);
            newProps.load(newFis);
            // if we've made it this far, the file exists and we should delete it if requested.
            if (deleteExistingNewFile) {
                logger.info(newPropsFile.getAbsoluteFile() + " will be deleted after merging properties");
                 newPropsFile.deleteOnExit();
            }

        } catch (FileNotFoundException e) {
            logger.info(newPropsFile.getAbsolutePath() + " does not exist, no need to merge with existing properties");
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error reading properties file '"+newPropsFile+"'.", ce);
        } finally {
            ResourceUtils.closeQuietly(newFis);
        }

        PropertiesConfiguration origConfiguration = new PropertiesConfiguration();
        origConfiguration.setAutoSave(false);
        origConfiguration.setListDelimiter((char)0);

        //if successful, back a new properties object with these as defaults
        FileInputStream origFis = null;
        try {
            origFis = new FileInputStream(origPropsFile);
            origConfiguration.load(origFis);
        } catch (ConfigurationException ce) {
            throw new CausedIOException("Error reading properties file '"+origPropsFile+"'.", ce);
        } finally {
            ResourceUtils.closeQuietly(origFis);
        }

        //now get all the keys and make a new properties object;
        Iterator allProps = newProps.getKeys();
        while(allProps.hasNext()) {
            String propName = (String) allProps.next();
            if (!origConfiguration.containsKey(propName)) {
                origConfiguration.setProperty(propName, newProps.getProperty(propName));
            }
        }

        return origConfiguration;
    }


    /**
     * <p>Merges properties from the properties file with propName "newPropsFileName" into the properies obtained from file
     * "originalPropsFileName".</p>
     *
     * <p>Any properties that exist in both are not replaced, but instead retain the propValue within
     * "originalPropsFileName"</p>
     *
     * @param originalPropsFileName the path to the original properties file. This is the base list of properties used
     *        in the merge (must not be null or empty)
     *
     * @param newPropsFileName the path to the new properties that will be merged. Only new properties from this
     *        file will be added to the list (must not be null or empty).
     * @param createIfNew whether to create a file called originalPropsFileName if it is found not to exist
     *
     * @param deleteExistingNewFile
     * @return
     * <ul>
     * * <li>a new Properties object (possibly empty if the original and the new file could not be found)
     * consisting of all properties contained in the two properies files. This will be equivalent to the original if
     * something failed with the new one.</li>
     * </ul>
     */
    public static PropertiesConfiguration mergeProperties(String originalPropsFileName, String newPropsFileName, boolean createIfNew, boolean deleteExistingNewFile) throws IOException {
        if (StringUtils.isEmpty(originalPropsFileName))
            throw new IllegalArgumentException("The original property file propName cannot be empty");

        if (StringUtils.isEmpty(newPropsFileName))
            throw new IllegalArgumentException("The new property file propName cannot be empty");

        File origPropsFile = new File(originalPropsFileName);
        File newPropsFile = new File(newPropsFileName);
        return mergeProperties(origPropsFile, newPropsFile,  createIfNew, deleteExistingNewFile);
    }
}
