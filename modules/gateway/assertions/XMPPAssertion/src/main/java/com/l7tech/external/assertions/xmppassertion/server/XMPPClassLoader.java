package com.l7tech.external.assertions.xmppassertion.server;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 14/06/12
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPClassLoader extends ClassLoader {
    private static final Logger logger = Logger.getLogger(XMPPClassLoader.class.getName());

    private File aarFile;
    private HashMap<String, byte[]> availableClasses = new HashMap<String, byte[]>();
    private HashMap<String, Class> classesFromOtherCL = new HashMap<String, Class>();

    public XMPPClassLoader(ClassLoader parent, HashSet<Class> classesFromOtherCL, String[] aarPackagesToLoad) {
        super(parent);

        for(Class clazz : classesFromOtherCL) {
            this.classesFromOtherCL.put(clazz.getName(), clazz);
        }

        String aarPath = XMPPClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        aarFile = new File(aarPath);

        HashSet<String> packagesToLoad = new HashSet<String>(aarPackagesToLoad.length);
        for(String packageName : aarPackagesToLoad) {
            packagesToLoad.add(packageName.replace('.', '/'));
        }

        JarInputStream aarStream = null;
        try {
            aarStream = new JarInputStream(new FileInputStream(aarFile));
            JarEntry entry = aarStream.getNextJarEntry();
            while(entry != null) {
                if(!entry.isDirectory()) {
                    if(entry.getName().startsWith("AAR-INF/lib") && entry.getName().endsWith(".jar")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];

                        while(true) {
                            int bytesRead = aarStream.read(buffer);
                            if(bytesRead == -1) {
                                break;
                            }

                            baos.write(buffer, 0, bytesRead);
                        }

                        scanNestedJarFile(entry.getName(), baos.toByteArray());
                    } else if(packagesToLoad.contains(entry.getName().substring(0, entry.getName().lastIndexOf('/'))) && entry.getName().endsWith(".class")) {
                        String className = entry.getName().substring(0, entry.getName().length() - 6);
                        className = className.replace('/', '.');

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];

                        while(true) {
                            int bytesRead = aarStream.read(buffer);
                            if(bytesRead == -1) {
                                break;
                            }

                            baos.write(buffer, 0, bytesRead);
                        }

                        availableClasses.put(className, baos.toByteArray());
                    }
                }

                entry = aarStream.getNextJarEntry();
            }
        } catch(FileNotFoundException e) {
            logger.log(Level.WARNING, "Could not open AAR file.", e);
        } catch(IOException e) {
            logger.log(Level.WARNING, "Could not open AAR file.", e);
        } finally {
            if(aarStream != null) {
                try {
                    aarStream.close();
                } catch(IOException e) {
                    // Ignore
                }
            }
        }
    }

    private void scanNestedJarFile(String name, byte[] jarBytes) {
        JarInputStream jarInputStream = null;
        try {
            jarInputStream = new JarInputStream(new ByteArrayInputStream(jarBytes));
            JarEntry entry = jarInputStream.getNextJarEntry();
            while(entry != null) {
                if(!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String className = entry.getName().substring(0, entry.getName().length() - 6);
                    className = className.replace('/', '.');

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];

                    while(true) {
                        int bytesRead = jarInputStream.read(buffer);
                        if(bytesRead == -1) {
                            break;
                        }

                        baos.write(buffer, 0, bytesRead);
                    }

                    availableClasses.put(className, baos.toByteArray());
                }

                entry = jarInputStream.getNextJarEntry();
            }
        } catch(IOException e) {
            // Ignore
        } finally {
            if(jarInputStream != null) {
                try {
                    jarInputStream.close();
                } catch(IOException e) {
                    // Ignore
                }
            }
        }
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        if(classesFromOtherCL.containsKey(name)) {
            return classesFromOtherCL.get(name);
        }

        if(!availableClasses.containsKey(name)) {
            return super.findClass(name);
        }

        Class clazz = defineClass(name, availableClasses.get(name), 0, availableClasses.get(name).length, XMPPClassLoader.class.getProtectionDomain());
        resolveClass(clazz);
        return clazz;
    }
}
