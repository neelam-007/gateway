package com.l7tech.server.config;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * User: megery
 * Date: Apr 3, 2006
 * Time: 3:07:07 PM
 */
public class PropertyHelper {
    public static Map getProperties(String propFileName, String[] propsToFetch) throws IOException {
        if (propFileName == null || propFileName.equalsIgnoreCase("")) {
            throw new IllegalArgumentException("The property file name cannot be empty");
        }
        if (propsToFetch == null) {
            throw new IllegalArgumentException("List of proprties to fetch cannot be null)");
        }

        Properties props = new Properties();
        FileInputStream fis = null;
        Map propsReturned = null;
        try {
            fis = new FileInputStream(propFileName);
            props.load(new FileInputStream(propFileName));
            propsReturned = new HashMap();
            for (int i = 0; i < propsToFetch.length; i++) {
                String s = propsToFetch[i];
                propsReturned.put(s, props.getProperty(s));
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
}
