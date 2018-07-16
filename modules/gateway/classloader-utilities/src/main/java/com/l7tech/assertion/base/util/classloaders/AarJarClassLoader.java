package com.l7tech.assertion.base.util.classloaders;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The AAR jar class loader. This is the type of class loader that will load jars from the
 * AAR file.
 */
public class AarJarClassLoader extends CustomClassLoader {

    private static final Logger logger = Logger.getLogger(AarJarClassLoader.class.getName());

    private File aarFile;
    private String[] aarPackagesToLoad;

    /**
     * The default constructor which takes as a parameter the AAR packages to load.
     *
     * @param parent the parent class loader
     * @param aarPackagesToLoad the AAR packages to load
     */
    protected AarJarClassLoader(ClassLoader parent, String[] aarPackagesToLoad) {
        super(parent);
        this.aarPackagesToLoad = aarPackagesToLoad;
        initialize();
    }

    /**
     * The initialization method for the AAR jar classloader. This method is responsible for taking the AAR file, and
     * going through the libraries that are included within it and then loading the classes so that they are
     * available for use.
     */
    @Override
    public void initialize() {

        //Get the AAR file path and open the file
        String aarPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        aarFile = new File(aarPath);

        //Go through the package names and sanitize them for use
        HashSet<String> packagesToLoad = new HashSet<String>(aarPackagesToLoad.length);
        for(String packageName : aarPackagesToLoad) {
            packagesToLoad.add(packageName.replace('.', '/'));
        }

        ByteArrayOutputStream baos = null;
        JarInputStream aarStream = null;
        try {
            //Go through the AAR file jar entries, and see what is found
            aarStream = new JarInputStream(new FileInputStream(aarFile));
            JarEntry entry = aarStream.getNextJarEntry();
            while(entry != null) {
                //Do nothing if a directory is found
                if(!entry.isDirectory()) {
                    if(entry.getName().startsWith("AAR-INF/lib") && entry.getName().endsWith(".jar")) {
                        //If the entry is a JAR just copy the stream and scan for anything nested within it
                        baos = new ByteArrayOutputStream();
                        IOUtils.copyStream(aarStream, baos);
                        scanNestedJarFile(entry.getName(), baos.toByteArray());
                        baos.close();
                    } else if(packagesToLoad.contains(entry.getName().substring(0, entry.getName().lastIndexOf('/'))) && entry.getName().endsWith(".class")) {
                        //If the entry is a class, then just copy the entry and add it to your available class bytes
                        String className = entry.getName().substring(0, entry.getName().length() - 6);
                        className = className.replace('/', '.');
                        baos = new ByteArrayOutputStream();
                        IOUtils.copyStream(aarStream, baos);
                        availableClassBytes.put(className, baos.toByteArray());
                        baos.close();
                    }
                }
                entry = aarStream.getNextJarEntry();
            }
        } catch(FileNotFoundException e) {
            logger.log(Level.WARNING, "Could not open AAR file.", e);
        } catch(IOException e) {
            logger.log(Level.WARNING, "Could not open AAR file.", e);
        } finally {
            ResourceUtils.closeQuietly(aarStream);
            ResourceUtils.closeQuietly(baos);
        }
    }

    /**
     * Helper method which will scan for nested JARs within the JAR bytes that are provided. Will add
     * entries to the available class bytes until the JAR entries are all consumed.
     *
     * @param name the JAR name
     * @param jarBytes the JAR bytes
     */
    private void scanNestedJarFile(String name, byte[] jarBytes) {
        ByteArrayOutputStream baos = null;
        JarInputStream jarInputStream = null;
        try {
            //Iterate through the JAR input stream for all entries
            jarInputStream = new JarInputStream(new ByteArrayInputStream(jarBytes));
            JarEntry entry = jarInputStream.getNextJarEntry();
            while(entry != null) {
                //If the entry is not a directory, and is a class
                if(!entry.isDirectory() && entry.getName().endsWith(".class")) {

                    //Add the information to the available class bytes for the AAR JAR class loader
                    String className = entry.getName().substring(0, entry.getName().length() - 6);
                    className = className.replace('/', '.');
                    baos = new ByteArrayOutputStream();
                    IOUtils.copyStream(jarInputStream, baos);
                    availableClassBytes.put(className, baos.toByteArray());
                    baos.close();
                }
                entry = jarInputStream.getNextJarEntry();
            }
        } catch(IOException e) {
            logger.log(Level.WARNING, "There was an exception reading nested JAR", e);
        } finally {
            ResourceUtils.closeQuietly(jarInputStream);
            ResourceUtils.closeQuietly(baos);
        }
    }
}
