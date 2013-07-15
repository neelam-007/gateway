package com.ca.siteminder;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */
public class SiteMinderAgentApiClassLoader extends ClassLoader {
    private static final Logger logger = Logger.getLogger(SiteMinderAgentApiClassLoader.class.getName());

    protected static ProtectionDomain cryptoj_pd;
    protected Map<String, byte[]> availableClasses = new HashMap<String, byte[]>();
    protected Map<String, byte[]> availableProperties = new HashMap<String, byte[]>();

    public SiteMinderAgentApiClassLoader(ClassLoader parent) {
        String currentJarFile = null;
        cryptoj_pd = com.rsa.cryptoj.c.A.class.getProtectionDomain(); // Retrieve Protection Domain from any class within cryptoj.jar.

        String[] jarPath = {"smagentapi-12.5.jar", "smjavasdk2-12.5.jar", "cryptoj-5.0.jar", "SmJavaApi-12.5.jar"};

        try {
            for(String jarFile : jarPath) {
                currentJarFile = jarFile;
                String s = cryptoj_pd.getCodeSource().getLocation().getFile();
                int i = s.lastIndexOf('/');
                String path = s.substring(0, i);

                InputStream is = new FileInputStream(path + File.separator + jarFile);
                 scanNestedJarFile(new JarInputStream(is));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load " + currentJarFile);
        }

    }

    public SiteMinderAgentApiClassLoader (ClassLoader parent, String[] jarPackagesToLoad) {
        super(parent);

        String jarPath = SiteMinderAgentApiClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        File jarFile = new File(jarPath);
        cryptoj_pd = com.rsa.cryptoj.c.A.class.getProtectionDomain(); // Retrieve Protection Domain from any class within cryptoj.jar.

        HashSet<String> packagesToLoad = new HashSet<String>(jarPackagesToLoad.length);
        for(String packageName : jarPackagesToLoad) {
            packagesToLoad.add(packageName.replace('.', '/'));
        }

        JarInputStream jarStream = null;
        try {
            jarStream = new JarInputStream(new FileInputStream(jarFile));
            JarEntry entry = jarStream.getNextJarEntry();
            while(entry != null) {
                if(!entry.isDirectory()) {
                    if(entry.getName().startsWith("lib") && entry.getName().endsWith(".jar")) {
                        byte[] bytes = read(jarStream);
                        scanNestedJarFile(bytes);
                    } else if(entry.getName().endsWith(".class") && packagesToLoad.contains(entry.getName().substring(0, entry.getName().lastIndexOf('/')))) {
                        byte[] bytes = read(jarStream);
                        String className = entry.getName().substring(0, entry.getName().length() - 6);
                        className = className.replace('/', '.');
                        availableClasses.put(className, bytes);
                    } else if(entry.getName().endsWith(".properties")) {
                        byte[] bytes = read(jarStream);
                        availableProperties.put(entry.getName(), bytes);
                    }
                }

                entry = jarStream.getNextJarEntry();
            }
        } catch(FileNotFoundException e) {
            logger.log(Level.WARNING, "Could not open JAR file.", e);
        } catch(IOException e) {
            logger.log(Level.WARNING, "Could not open JAR file.", e);
        } finally {
            if(jarStream != null) {
                try {
                    jarStream.close();
                } catch(IOException e) {
                    // Ignore
                }
            }
        }
    }

    protected void scanNestedJarFile(byte[] jarBytes) {
        JarInputStream jarInputStream = null;
        try {
            jarInputStream = new JarInputStream(new ByteArrayInputStream(jarBytes));
            JarEntry entry = jarInputStream.getNextJarEntry();
            while(entry != null) {
                if(!entry.isDirectory()) {
                    if (entry.getName().endsWith(".class")) {
                        byte[] bytes = read(jarInputStream);
                        String className = entry.getName().substring(0, entry.getName().length() - 6);
                        className = className.replace('/', '.');
                        availableClasses.put(className, bytes);
                    } else if (entry.getName().endsWith(".properties")) {
                        byte[] bytes = read(jarInputStream);
                        availableProperties.put(entry.getName(), bytes);
                    }
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

    protected void scanNestedJarFile(JarInputStream jarInputStream) {
        try {
            JarEntry entry = jarInputStream.getNextJarEntry();
            while(entry != null) {
                if(!entry.isDirectory()) {
                    if (entry.getName().endsWith(".class")) {
                        byte[] bytes = read(jarInputStream);
                        String className = entry.getName().substring(0, entry.getName().length() - 6);
                        className = className.replace('/', '.');
                        availableClasses.put(className, bytes);
                    } else if (entry.getName().endsWith(".properties")) {
                        byte[] bytes = read(jarInputStream);
                        availableProperties.put(entry.getName(), bytes);
                    }
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

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if(!availableClasses.containsKey(name)) {
            return super.findClass(name);
        } else {
            byte[] classBytes = availableClasses.get(name);
            Class clazz = defineClass(name, classBytes, 0, classBytes.length, cryptoj_pd);
            resolveClass(clazz);
            return clazz;
        }
    }

    @Override
    protected URL findResource(String name) {
        // Note: No need to override protected Enumeration<URL> findResources(String name) because it is not
        // called for this class loader.

        if(!availableProperties.containsKey(name)) {
            return super.findResource(name);
        } else {
            try {
                byte[] resourceBytes = availableProperties.get(name);
                return makeResourceUrl(name, resourceBytes);
            } catch(IOException e) {
                return null;
            }
        }
    }

    private URL makeResourceUrl(String name, final byte[] resourceBytes) throws MalformedURLException {
        return new URL("file", null, -1, name, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException { }
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new ByteArrayInputStream(resourceBytes);
                    }
                };
            }
        });
    }

    private byte[] read(JarInputStream jarInputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];

        while(true) {
            int bytesRead = jarInputStream.read(buffer);
            if(bytesRead == -1) {
                break;
            }
            baos.write(buffer, 0, bytesRead);
        }

        return baos.toByteArray();
    }

}
