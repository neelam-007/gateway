package com.l7tech.external.assertions.ipm.server;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Utility for compilation of in-memory Java code stored as a String to an in-memory Class instance, using
 * no temporary files and requiring no disk write access.
 * <p/>
 * Relies on Java 6 JavaCompiler support being available in the current environment.
 * @noinspection ClassLoaderInstantiation
 */
public class Codegen {
    protected static final Logger logger = Logger.getLogger(Codegen.class.getName());

    private final String className;
    private final String javaSource;
    private DiagnosticCollector<JavaFileObject> diagnostics = null;
    private List<MemoryJavaFile> extraCompilationUnits = new ArrayList<MemoryJavaFile>();

    /**
     * Create a Codegen that will compile the specified Java source, which is expected to contain the complete
     * definition of the specified fully-qualified classname.
     * @param className a fully qualified classname, ie "MyDefaultPackageClass", or "com.yoyodyne.Blah"
     * @param javaSource the content of the .java file that would be used to represent a complete definition
     *                   of className.  If the className isn't in the default package, must include a package declaration.
     */
    public Codegen(String className, String javaSource) {
        this.className = className;
        this.javaSource = javaSource;
    }

    public Iterable<? extends JavaFileObject> getCompilationUnits(MemoryJavaFile file) {
        List<MemoryJavaFile> ret = new ArrayList<MemoryJavaFile>();
        ret.add(file);
        ret.addAll(extraCompilationUnits);
        return ret;
    }

    public void addJavaFile(String classname, String javaSource) {
        extraCompilationUnits.add(new MemoryJavaFile(classname, javaSource));
    }

    /** Exception thrown if there is a problem compiling some dynamic code. */
    public static class CompileException extends Exception {
        public CompileException(Throwable cause) {
            super(cause);
        }

        public CompileException(String message) {
            super(message);
        }
    }

    /**
     * Compile the class and return a ready-to-use Class instance.
     * 
     * @param parentClassLoader parent class loader to use to control what is visible to the generated code.  If null, uses system class loader.
     * @return the compiled Class.  Never null.
     * @throws Codegen.CompileException if there is a problem compiling the class.  Call
     *                                                        {@link #getDiagnostics()} for more information.
     * @throws ClassNotFoundException if the input compiled successfully but did not produce the expected className
     *                                among its output.
     *
     * @throws IllegalStateException if compile() has already been called on this instance
     * @throws UnsupportedOperationException  if no JavaCompiler service is available in this environment
     */
    public Class compile(ClassLoader parentClassLoader) throws CompileException, ClassNotFoundException, IllegalStateException, UnsupportedOperationException {
        if (parentClassLoader == null) parentClassLoader = ClassLoader.getSystemClassLoader();
        if (diagnostics != null)
            throw new IllegalStateException("Compilation has already been attempted");
        diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            throw new UnsupportedOperationException("No JavaCompiler available");
        MemoryJavaFile file = new MemoryJavaFile(className, javaSource);
        Iterable<? extends JavaFileObject> compilationUnits = getCompilationUnits(file);
        ConcurrentHashMap<String, MemoryJavaClass> bytecodes = new ConcurrentHashMap<String, MemoryJavaClass>();
        JavaFileManager fileManager = makeFileManager(compiler, diagnostics, bytecodes);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
        boolean success = task.call();

        if (!success)
            throw new CompileException("Compilation failed; check diagnostics");

        ClassLoader classLoader = new BytecodesClassLoader(parentClassLoader, bytecodes.values());
        return classLoader.loadClass(className);
    }

    /**
     * Get the diagnostic information from the last compile attempt.
     *
     * @return the diagnostics.  Never null.
     * @throws IllegalStateException if {@link #compile} has not yet been called.
     */
    public List<? extends Diagnostic> getDiagnostics() throws IllegalStateException {
        if (diagnostics == null) throw new IllegalStateException("No compile attempted yet");
        return diagnostics.getDiagnostics();
    }

    /**
     * A ClassLoader that finds class bytecode in the specified collection of {@link MemoryJavaClass} instances.
     *
     * @noinspection CustomClassloader
     */
    private static class BytecodesClassLoader extends ClassLoader {
        private final Map<String, byte[]> bytecodes;

        BytecodesClassLoader(ClassLoader parent, Collection<MemoryJavaClass> classfiles) {
            super(parent);
            if (parent == null) throw new IllegalArgumentException();

            Map<String, byte[]> bytes = new HashMap<String, byte[]>();
            for (MemoryJavaClass mjc : classfiles)
                bytes.put(mjc.getClassName(), mjc.getBytes());
            this.bytecodes = bytes;
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytecode = bytecodes.get(name);
            if (bytecode == null)
                throw new ClassNotFoundException(name);
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }

    /**
     * Create a JavaFileManager that will collect .class file output in the specified ConcurrentHashMap.
     *
     * @param compiler the compiler that will be using this
     * @param diagnostics  diagnostics collector to pass to StandardFileManager
     * @param bytecodeOutput  map in which to collect output .class files. Required
     * @return the new JavaFileManager.  Never null.
     */
    private static JavaFileManager makeFileManager(JavaCompiler compiler,
                                                   DiagnosticCollector<JavaFileObject> diagnostics,
                                                   final ConcurrentHashMap<String, MemoryJavaClass> bytecodeOutput)
    {
        final StandardJavaFileManager sfm = compiler.getStandardFileManager(diagnostics, null, null);
        return new ForwardingJavaFileManager<StandardJavaFileManager>(sfm) {
            public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
                logger.finest("getJavaFileForInput: location=" + location + "  className=" + className + "  kind=" + kind);
                if (JavaFileObject.Kind.CLASS.equals(kind))
                    return getMemoryJavaClass(className);
                return super.getJavaFileForInput(location, className, kind);
            }

            public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
                logger.finest("getJavaFileForOutput: location=" + location + "  className=" + className + "  kind=" + kind);
                if (JavaFileObject.Kind.CLASS.equals(kind))
                    return getMemoryJavaClass(className);
                throw new IllegalArgumentException("Unable to output file of kind " + kind);
            }

            private MemoryJavaClass getMemoryJavaClass(String className) {
                MemoryJavaClass c = bytecodeOutput.get(className);
                if (c != null)
                    return c;
                c = new MemoryJavaClass(className);
                MemoryJavaClass old = bytecodeOutput.putIfAbsent(className, c);
                return old != null ? old : c;
            }

            public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
                logger.finest("getFileForInput: location=" + location + "  packageName=" + packageName + "  relativeName=" + relativeName);
                return super.getFileForInput(location, packageName, relativeName);
            }

            public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
                logger.finest("getFileForOutput: location=" + location + "  packageName=" + packageName + "  relativeName=" + relativeName);
                throw new IllegalArgumentException("Unable to output file with no kind");
            }
        };
    }

    /**
     * Represents a virtual .class file.  Supports writing as bytes or characters, and reading previously-written
     * data back as bytes or characters.
     */
    private static class MemoryJavaClass extends SimpleJavaFileObject {
        private final String className;
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public MemoryJavaClass(String className) {
            super(URI.create("string:///" + className.replace('.','/') + Kind.CLASS.extension),Kind.CLASS);
            this.className = className;
        }

        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(baos.toByteArray());
        }

        public OutputStream openOutputStream() throws IOException {
            baos = new ByteArrayOutputStream();
            return baos;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return baos.toString();
        }

        public String getClassName() {
            return className;
        }

        public byte[] getBytes() {
            return baos.toByteArray();
        }
    }

    /**
     * Represents a virtual .java file.  Supports reading as characters.
     */
    private static class MemoryJavaFile extends SimpleJavaFileObject {
        final String source;

        MemoryJavaFile(String name, String source) {
            super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}