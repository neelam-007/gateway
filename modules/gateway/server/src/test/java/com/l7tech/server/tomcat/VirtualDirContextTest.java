package com.l7tech.server.tomcat;

import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.common.io.IOUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;

import javax.naming.*;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class VirtualDirContextTest extends TestCase {
    private static final Logger log = Logger.getLogger(VirtualDirContextTest.class.getName());
    private static final String TEST_DIR = "_VirtualDirContextTest_";
    private static final String FILE_CONTENT_WEBXML = "<webxml>this is a web.xml file</webxml>";
    private static final String FILE_CONTENT_INDEXHTML = "<html><head><title>This is index.html</title></head><body></body></html>";
    private static final String FILE_CONTENT_BLAHHTML = "<html><head><title>This is blah.html</title></head><body></body></html>";
    private static final String FILE_CONTENT_FOOJAR = "this is a jarfile";

    public VirtualDirContextTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(VirtualDirContextTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private void createFile(File file, String contents) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(contents.getBytes());
        } finally {
            ResourceUtils.closeQuietly(os);
        }
    }

    /**
     * Makes a test filesystem on disk in the current directory that looks like this:
     * <pre>
     *   _VirtualDirContextTest_/
     *     WEB-INF/
     *       lib/
     *         foo.jar
     *       web.xml
     *     ssg/
     *       index.html
     *       blah.html
     * </pre>
     *
     * @return the test filesystem
     * @throws java.io.IOException if there is a problem building the test directory
     */
    private DirContext makeTestRealFilesystem() throws IOException {
        File temp = File.createTempFile("tmp", ".tmp");
        temp.deleteOnExit();
        File tempDir = temp.getParentFile();
        File root = new File(tempDir, TEST_DIR);
        root.mkdir();
        root.deleteOnExit();

        File webinf = new File(root, "WEB-INF");
        webinf.mkdir();
        webinf.deleteOnExit();

        File webxml = new File(webinf, "web.xml");
        createFile(webxml, FILE_CONTENT_WEBXML);
        webxml.deleteOnExit();

        File lib = new File(webinf, "lib");
        lib.mkdir();
        lib.deleteOnExit();

        File foojar = new File(lib, "foo.jar");
        createFile(foojar, FILE_CONTENT_FOOJAR);
        foojar.deleteOnExit();

        FileDirContext dc = new FileDirContext();
        dc.setDocBase(root.getAbsolutePath());

        File ssgdir = new File(root, "ssg");
        ssgdir.mkdir();
        ssgdir.deleteOnExit();

        File indexhtml = new File(ssgdir, "index.html");
        createFile(indexhtml, FILE_CONTENT_INDEXHTML);
        indexhtml.deleteOnExit();

        File blahhtml = new File(ssgdir, "blah.html");
        createFile(blahhtml, FILE_CONTENT_BLAHHTML);
        blahhtml.deleteOnExit();

        return dc;
    }

    /**
     * Makes a virtual test filesystem that looks like this:
     * <pre>
     *   (root)/
     *     WEB-INF/
     *       lib/
     *         foo.jar
     *       web.xml
     *     ssg/
     *       index.html
     *       blah.html
     * </pre>
     *
     * @return the test filesystem
     */
    private DirContext makeTestVirtualFilesystem() {
        VirtualDirEntry foojar = new VirtualDirEntryImpl("foo.jar", FILE_CONTENT_FOOJAR.getBytes());
        VirtualDirContext lib = new VirtualDirContext("lib", foojar);
        VirtualDirEntry webxml = new VirtualDirEntryImpl("web.xml", FILE_CONTENT_WEBXML.getBytes());
        VirtualDirContext webinf = new VirtualDirContext("WEB-INF", webxml, lib);
        VirtualDirEntry indexhtml = new VirtualDirEntryImpl("index.html", FILE_CONTENT_INDEXHTML.getBytes());
        VirtualDirEntry blahhtml = new VirtualDirEntryImpl("blah.html", FILE_CONTENT_BLAHHTML.getBytes());
        VirtualDirContext ssgdir = new VirtualDirContext("ssg", indexhtml, blahhtml);
        VirtualDirContext root = new VirtualDirContext("", webinf, ssgdir);
        root.setDocBase("");
        return root;
    }

    /**
     * A test filesystem as above but where the WEB-INF directory is virtual but the ssg directory comes from disk.
     *
     * @return the test filesystem
     * @throws java.io.IOException if theres a problem making a file
     */
    private DirContext makeTestHybridFilesystem() throws IOException {
        VirtualDirEntry foojar = new VirtualDirEntryImpl("foo.jar", FILE_CONTENT_FOOJAR.getBytes());
        VirtualDirContext lib = new VirtualDirContext("lib", foojar);
        VirtualDirEntry webxml = new VirtualDirEntryImpl("web.xml", FILE_CONTENT_WEBXML.getBytes());
        VirtualDirContext webinf = new VirtualDirContext("WEB-INF", webxml, lib);

        // Splice in some real files
        makeTestRealFilesystem();
        File temp = File.createTempFile("tmp", ".tmp");
        temp.deleteOnExit();
        File tempDir = temp.getParentFile();
        File fsroot = new File(tempDir, TEST_DIR);
        File ssg = new File(fsroot, "ssg");
        FileDirContext ssgContext = new FileDirContext();
        ssgContext.setDocBase(ssg.getAbsolutePath());
        VirtualDirContext ssgdir = new VirtualDirContext("ssg", ssgContext);

        VirtualDirContext root = new VirtualDirContext("", webinf, ssgdir);
        root.setDocBase("");
        return root;
    }

    private <T> List<T> toList(NamingEnumeration<T> namingEnumeration) {
        List<T> ret = new ArrayList<T>();
        while (namingEnumeration.hasMoreElements()) {
            T ncp = namingEnumeration.nextElement();
            ret.add(ncp);
        }
        return ret;
    }

    private boolean isFile(NameClassPair ncp) throws Exception {
        String classname = ncp.getClassName();
        return isFile(classname);
    }

    private boolean isFile(String classname) throws ClassNotFoundException {
        Class c = Class.forName(classname);
        return Resource.class.isAssignableFrom(c);
    }

    private boolean isDirectory(NameClassPair ncp) throws Exception {
        String classname = ncp.getClassName();
        return isDirectory(classname);
    }

    private boolean isDirectory(String classname) throws ClassNotFoundException {
        Class c = Class.forName(classname);
        return DirContext.class.isAssignableFrom(c);
    }

    // Check the filsystem root
    private void checkFilesystem(DirContext dc) throws Exception {
        log.info("Base: " + dc.getNameInNamespace());
        assertLookupFails(dc, "NONEXISTENT");
        assertLookupFails(dc, "WEB-INF/blat");
        assertLookupFails(dc, "ssg/wee");
        assertLookupFails(dc, "WEE-INF/web.xml");
        assertLookupSucceeds(dc, "WEB-INF/web.xml");
        assertFileContains(dc, "WEB-INF/web.xml", FILE_CONTENT_WEBXML);
        assertFileContains(dc, "WEB-INF/lib/foo.jar", FILE_CONTENT_FOOJAR);
        assertFileContains(dc, "ssg/index.html", FILE_CONTENT_INDEXHTML);
        assertFileContains(dc, "ssg/blah.html", FILE_CONTENT_BLAHHTML);

        {
            List<NameClassPair> webInfList = toList(dc.list("WEB-INF"));
            checkWebInfListing(webInfList);
        }

        {
            List<NameClassPair> selfList = toList(dc.list(new CompositeName()));
            assertEquals(2, selfList.size());

            NameClassPair webinf = selfList.get(0);
            assertEquals("WEB-INF", webinf.getName());
            assertTrue(isDirectory(webinf));

            NameClassPair ssg = selfList.get(1);
            assertEquals("ssg", ssg.getName());
            assertTrue(isDirectory(ssg));

            // descend into the WEB-INF directory
            Object webinfObj = dc.lookup("WEB-INF");
            assertTrue(webinfObj instanceof DirContext);
            checkWebInfDir((DirContext)webinfObj);

            // descend into the ssg directory
            Object ssgObj = dc.lookup("ssg");
            assertTrue(ssgObj instanceof DirContext);
            checkSsgDir((DirContext)ssgObj);
        }

        {
            List<Binding> slb = toList(dc.listBindings(""));
            assertEquals(2, slb.size());

            Binding webinfBinding = slb.get(0);
            assertEquals("WEB-INF", webinfBinding.getName());
            assertTrue(isDirectory(webinfBinding.getClassName()));
            assertTrue(webinfBinding.getObject() instanceof DirContext);

            Binding ssgBinding = slb.get(1);
            assertEquals("ssg", ssgBinding.getName());
            assertTrue(isDirectory(ssgBinding.getClassName()));
            assertTrue(ssgBinding.getObject() instanceof DirContext);
        }
    }

    private byte[] getStreamedContent(Resource resource) throws IOException {
        InputStream is = null;
        try {
            is = resource.streamContent();
            return IOUtils.slurpStream(is);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    private void assertFileContains(DirContext dc, String name, String content) throws Exception {
        Object got = dc.lookup(name);
        assertNotNull(got);
        assertTrue(got instanceof Resource);
        Resource resource = (Resource)got;
        byte[] resourceBytes = getStreamedContent(resource);
        assertNotNull(resourceBytes);
        assertEquals(new String(resourceBytes), content);

        ResourceAttributes attrs = (ResourceAttributes)dc.getAttributes(name);
        Attribute nameAttr = attrs.get(ResourceAttributes.NAME);
        Attribute collAttr = attrs.get(ResourceAttributes.COLLECTION_TYPE);
        Attribute lenAttr = attrs.get(ResourceAttributes.CONTENT_LENGTH);

        assertTrue(name.endsWith(nameAttr.get().toString()));
        assertTrue(collAttr == null);
        assertTrue(lenAttr.get().equals((long)content.getBytes().length));
    }

    private void checkWebInfListing(List<NameClassPair> webInfList) throws Exception {
        assertEquals(2, webInfList.size());
        NameClassPair lib = webInfList.get(0);
        assertEquals("lib", lib.getName());
        assertTrue(isDirectory(lib));
        NameClassPair webxml = webInfList.get(1);
        assertEquals("web.xml", webxml.getName());
        assertTrue(isFile(webxml));
    }

    private void assertLookupFails(DirContext context, String name) {
        try {
            context.lookup(name);
            fail("expected exception not thrown: NamingException not thrown when looking up \"" + name + "\" in " + context.getClass().getSimpleName() + ": " + context.getNameInNamespace());
        } catch (NamingException ne) {
            // Ok
        }
    }

    private void assertLookupSucceeds(DirContext context, String name) throws Exception {
        Object got = context.lookup(name);
        assertNotNull(got);
        assertTrue(got instanceof Resource || got instanceof DirContext);
    }

    // Check the WEB-INF subdirectory
    private void checkWebInfDir(DirContext webinf) throws Exception {
        ResourceAttributes attr = (ResourceAttributes)webinf.getAttributes("");
        assertTrue(attr.isCollection());

        List<NameClassPair> webInfList = toList(webinf.list(""));
        checkWebInfListing(webInfList);
        assertLookupFails(webinf, "NONEXISTENT");
        assertLookupSucceeds(webinf, "web.xml");
        assertLookupFails(webinf, "foo/bar");
        assertFileContains(webinf, "web.xml", FILE_CONTENT_WEBXML);
        assertFileContains(webinf, "lib/foo.jar", FILE_CONTENT_FOOJAR);
    }

    // Check the ssg subdirectory
    private void checkSsgDir(DirContext ssg) throws Exception {
        ResourceAttributes attr = (ResourceAttributes)ssg.getAttributes("");
        assertTrue(attr.isCollection());

        List<NameClassPair> ssgList = toList(ssg.list(""));
        assertEquals(2, ssgList.size());

        NameClassPair blahhtml = ssgList.get(0);
        assertTrue(isFile(blahhtml));
        assertEquals("blah.html", blahhtml.getName());

        NameClassPair indexhtml = ssgList.get(1);
        assertTrue(isFile(indexhtml));
        assertEquals("index.html", indexhtml.getName());

        assertLookupFails(ssg, "NONEXISTENT");
        assertLookupSucceeds(ssg, "index.html");
        assertLookupSucceeds(ssg, "blah.html");
        assertLookupFails(ssg, "foo/bar");
        assertFileContains(ssg, "index.html", FILE_CONTENT_INDEXHTML);
        assertFileContains(ssg, "blah.html", FILE_CONTENT_BLAHHTML);
    }

    public void testRealFilesystem() throws Exception {
        DirContext testdc = makeTestRealFilesystem();
        checkFilesystem(testdc);
    }

    public void testVirtualFilesystem() throws Exception {
        DirContext root = makeTestVirtualFilesystem();
        assertTrue(root.getNameInNamespace().equals(""));
        checkFilesystem(root);
    }

    public void testHybridFilesystem() throws Exception {
        DirContext root = makeTestHybridFilesystem();
        checkFilesystem(root);
    }
}
