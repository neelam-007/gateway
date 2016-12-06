package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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
public class ExtensibleSocketConnectorClassLoader extends ClassLoader {
    private static final Logger logger = Logger.getLogger(ExtensibleSocketConnectorClassLoader.class.getName());

    private File aarFile;
    private HashMap<String, byte[]> availableClasses = new HashMap<String, byte[]>();
    private HashMap<String, byte[]> availableResourceBytes = new HashMap<String, byte[]>();
    private HashMap<String, Class> classesFromOtherCL = new HashMap<String, Class>();
    HashSet<String> packagesToLoad = null;
    private HashMap<String, Package> loadedPackages = new HashMap<String, Package>();
    private ClassLoader parent;

    public ExtensibleSocketConnectorClassLoader(ClassLoader parent, HashSet<Class> classesFromOtherCL, String[] aarPackagesToLoad) {
        super(parent.getParent());
        this.parent = parent;

        for (Class clazz : classesFromOtherCL) {
            this.classesFromOtherCL.put(clazz.getName(), clazz);
        }

        packagesToLoad = new HashSet<String>(aarPackagesToLoad.length);
        for (String packageName : aarPackagesToLoad) {
            packagesToLoad.add(packageName.replace('.', '/'));
        }
    }

    public void initialize() {

        String aarPath = ExtensibleSocketConnectorClassLoader.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        aarFile = new File(aarPath);

        JarInputStream aarStream = null;
        try {
            aarStream = new JarInputStream(new FileInputStream(aarFile));
            JarEntry entry = aarStream.getNextJarEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    if (entry.getName().startsWith("AAR-INF/lib") && entry.getName().endsWith(".jar")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];

                        while (true) {
                            int bytesRead = aarStream.read(buffer);
                            if (bytesRead == -1) {
                                break;
                            }

                            baos.write(buffer, 0, bytesRead);
                        }

                        scanNestedJarFile(entry.getName(), baos.toByteArray());
                    } else if (packagesToLoad.contains(entry.getName().substring(0, entry.getName().lastIndexOf('/'))) && entry.getName().endsWith(".class")) {
                        String className = entry.getName().substring(0, entry.getName().length() - 6);
                        className = className.replace('/', '.');

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];

                        while (true) {
                            int bytesRead = aarStream.read(buffer);
                            if (bytesRead == -1) {
                                break;
                            }

                            baos.write(buffer, 0, bytesRead);
                        }

                        availableClasses.put(className, baos.toByteArray());
                    } else {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];

                        while (true) {
                            int bytesRead = aarStream.read(buffer);
                            if (bytesRead == -1) {
                                break;
                            }

                            baos.write(buffer, 0, bytesRead);
                        }

                        availableResourceBytes.put(entry.getName(), baos.toByteArray());
                    }
                }

                entry = aarStream.getNextJarEntry();
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Could not open AAR file.", e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not open AAR file.", e);
        } finally {
            if (aarStream != null) {
                try {
                    aarStream.close();
                } catch (IOException e) {
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
            while (entry != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String className = entry.getName().substring(0, entry.getName().length() - 6);
                    className = className.replace('/', '.');

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];

                    while (true) {
                        int bytesRead = jarInputStream.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }

                        baos.write(buffer, 0, bytesRead);
                    }

                    availableClasses.put(className, baos.toByteArray());

                    // Define the package
                    String packageName = className.substring(0, className.lastIndexOf('.'));
                    if (!loadedPackages.containsKey(packageName) && getPackage(packageName) == null) {
                        Package p = definePackage(packageName, null, null, null, null, null, null, null);
                        loadedPackages.put(packageName, p);
                    }
                } else if (!entry.isDirectory()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];

                    while (true) {
                        int bytesRead = jarInputStream.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }

                        baos.write(buffer, 0, bytesRead);
                    }

                    availableResourceBytes.put(entry.getName(), baos.toByteArray());
                }

                entry = jarInputStream.getNextJarEntry();
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            if (jarInputStream != null) {
                try {
                    jarInputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (classesFromOtherCL.containsKey(name)) {
            return classesFromOtherCL.get(name);
        }

        if (!availableClasses.containsKey(name)) {

            try {
                return super.findClass(name);
            } catch (Exception e) {
                return Class.forName(name, true, parent);
            }
        }

        Class clazz = defineClass(name, availableClasses.get(name), 0, availableClasses.get(name).length, ExtensibleSocketConnectorClassLoader.class.getProtectionDomain());
        resolveClass(clazz);
        return clazz;
    }

    private static class XMPPURLConnection extends URLConnection {
        byte[] bytes;

        public XMPPURLConnection(byte[] bytes) {
            super(null);
            this.bytes = bytes;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void connect() throws IOException {
            // Ignore
        }
    }

    public URL getResource(final String name) {
        if (availableResourceBytes.containsKey(name)) {
            try {
                return new URL(null, "axiomatics-jar:/" + name, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        return new XMPPURLConnection(availableResourceBytes.get(name));
                    }
                });
            } catch (MalformedURLException e) {
                return null;
            }
        } else {
            return super.getResource(name);
        }
    }

    public void initializeWrapperClasses() {
        try {
            AttributeKeyFactory.initialize(this);
            ConnectFutureWrapper.initialize(this);
            DefaultIoFilterChainBuilderWrapper.initialize(this);
            DefaultIoFilterChainWrapper.initialize(this);
            ExecutorFilterWrapper.initialize(this);
            IdleStatusWrapper.initialize(this);
            IoFilterChainEntryWrapper.initialize(this);
            IoSessionConfigWrapper.initialize(this);
            IoSessionWrapper.initialize(this);
            NioSocketAcceptorWrapper.initialize(this);
            NioSocketConnectorWrapper.initialize(this);
            ProtocolCodecFilterWrapper.initialize(this);
            SslFilterWrapper.initialize(this);
            WriteFutureWrapper.initialize(this);
            CloseFutureWrapper.initialize(this);
            ReadFutureWrapper.initialize(this);
            InboundIoHandlerAdapterWrapper.initialize(this);
            OutboundIoHandlerAdapterWrapper.initialize(this);
        } catch (Exception e) {
            logger.warning("Unable to initialize the Extensible Socket Connector ClassLoader.");
        }
    }
}
