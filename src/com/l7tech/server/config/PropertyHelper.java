package com.l7tech.server.config;

import org.apache.commons.lang.StringUtils;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

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

        Properties props = new Properties();
        FileInputStream fis = null;
        Map<String, String> propsReturned = new HashMap<String, String>();
        try {
            fis = new FileInputStream(propFileName);
            props.load(new FileInputStream(propFileName));
            for (String propName : propsToFetch) {
                String propValue = props.getProperty(propName);
                propsReturned.put(propName, propValue);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
        }
        return propsReturned;
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
    public static Properties mergeProperties(File origPropsFile, File newPropsFile, boolean createIfNew, boolean deleteExistingNewFile) throws IOException {
        if (origPropsFile == null) throw new IllegalArgumentException("The original property file cannot be null");
        if (newPropsFile == null) throw new IllegalArgumentException("The new property file cannot be null");

        if (createIfNew) {
            try {
                if (origPropsFile.createNewFile()) {
                    logger.info("Successfully create new properties file: " + origPropsFile.getName());
                }
            } catch (IOException e) {
                logger.info("Error while creating new properties file: " + origPropsFile.getName() + " (" + e.getMessage() + ")");
            }
        }

        Properties mergedProps = new Properties();
        Properties origProps = null;

        FileInputStream newFis = null;

        Properties newProps = new Properties();

        //load the new props
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
        } finally {
            if (newFis != null) {
                try {newFis.close();} catch (IOException e) {}
            }
        }

        //if successful, back a new properties object with these as defaults
        origProps = new Properties(newProps);

        FileInputStream origFis = null;
        try {

            origFis = new FileInputStream(origPropsFile);

            //origProps could be null, if it didn't get initialized above with newProps as defaults.
            //In this case, just return the original ones
            if (origProps == null) origProps = new Properties();
            origProps.load(origFis);

            //now get all the keys and make a new properties object;
            Enumeration allProps = origProps.propertyNames();
            while(allProps.hasMoreElements()) {
                String propName = (String) allProps.nextElement();
                mergedProps.setProperty(propName, origProps.getProperty(propName));
            }
        } finally {
            if (origFis != null) {
                try {origFis.close();} catch (IOException e) {}
            }
        }

        return mergedProps;
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
    public static Properties mergeProperties(String originalPropsFileName, String newPropsFileName, boolean createIfNew, boolean deleteExistingNewFile) throws IOException {
        if (StringUtils.isEmpty(originalPropsFileName))
            throw new IllegalArgumentException("The original property file propName cannot be empty");

        if (StringUtils.isEmpty(newPropsFileName))
            throw new IllegalArgumentException("The new property file propName cannot be empty");

        File origPropsFile = new File(originalPropsFileName);
        File newPropsFile = new File(newPropsFileName);
        return mergeProperties(origPropsFile, newPropsFile,  createIfNew, deleteExistingNewFile);
    }
}
