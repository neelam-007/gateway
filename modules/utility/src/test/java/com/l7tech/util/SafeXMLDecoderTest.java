package com.l7tech.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.beans.ExceptionListener;
import java.beans.Introspector;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Vector;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SafeXMLDecoderTest {

    @Mock
    private ExceptionListener ignoreListener;
    private ExceptionListener failListener = new ExceptionListener() {
        @Override
        public void exceptionThrown(Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        }
    };

    @Mock
    private ClassFilter openFilter;

    @Mock
    private ClassFilter classFilter;

    @Before
    public void initMocks() {
        when(openFilter.permitClass(anyString())).thenReturn(true);
        when(openFilter.permitMethod(Matchers.<Method>anyObject())).thenReturn(true);
        when(openFilter.permitConstructor(Matchers.<Constructor>anyObject())).thenReturn(true);
    }

    private InputStream getCodedXML(Class clazz, String xmlFile)
        throws Exception {
        InputStream refIn;

        String version = System.getProperty("java.version");

        refIn = SafeXMLDecoderTest.class.getResourceAsStream(xmlFile);
        if (refIn == null) {
            throw new Error("resource " + xmlFile + " not exist in "
                + SafeXMLDecoderTest.class.getPackage());
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(refIn,
            "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        refIn.close();
        String refString = sb.toString();
        refString = refString.replace("${version}", version);
        if (clazz != null) {
            refString = refString.replace("${classname}", clazz.getName());
        }
        return new ByteArrayInputStream(refString.getBytes("UTF-8"));
    }

    static byte xml123bytes[] = null;

    static {
        ByteArrayOutputStream byteout = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(byteout);
        enc.writeObject(Integer.valueOf("1"));
        enc.writeObject(Integer.valueOf("2"));
        enc.writeObject(Integer.valueOf("3"));
        enc.close();
        xml123bytes = byteout.toByteArray();
    }

    static class MockClassLoader extends ClassLoader {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

    }

    /*
     * test SafeXMLDecoder constructor with null inputStream argument
     */
    @Test
    public void test_Constructor_NullInputStream_scenario1() {
        SafeXMLDecoder xmlDecoder = new SafeXMLDecoder(openFilter, null);
        assertNull(xmlDecoder.readObject());
        assertNull(xmlDecoder.getOwner());
        assertNotNull(xmlDecoder.getExceptionListener());
        xmlDecoder.close();
    }

    /*
     * test SafeXMLDecoder constructor with null inputStream argument
     */
    @Test
    public void test_Constructor_NullInputStream_scenario2() {
        SafeXMLDecoder xmlDecoder = new SafeXMLDecoder(openFilter, null, null);
        assertNull(xmlDecoder.readObject());
        assertNull(xmlDecoder.getOwner());
        assertNotNull(xmlDecoder.getExceptionListener());
        xmlDecoder.close();
    }

    /*
     * test SafeXMLDecoder constructor with null inputStream argument
     */
    @Test
    public void test_Constructor_NullInputStream_scenario3() {
        SafeXMLDecoder xmlDecoder = new SafeXMLDecoder(openFilter, null, null, null);
        assertNull(xmlDecoder.readObject());
        assertNull(xmlDecoder.getOwner());
        assertNotNull(xmlDecoder.getExceptionListener());
        xmlDecoder.close();
    }

    /*
     * test SafeXMLDecoder constructor with null inputStream argument
     */
    @Test
    public void test_Constructor_NullInputStream_scenario4() {
        SafeXMLDecoder xmlDecoder = new SafeXMLDecoder(openFilter, null, null, null, null);
        assertNull(xmlDecoder.readObject());
        assertNull(xmlDecoder.getOwner());
        assertNotNull(xmlDecoder.getExceptionListener());
        xmlDecoder.close();
    }

    /*
     * test SafeXMLDecoder constructor
     */
    @Test
    public void test_Constructor_Normal() throws Exception {
        SafeXMLDecoder xmlDecoder;
        xmlDecoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        assertEquals(null, xmlDecoder.getOwner());

        final Vector<Exception> exceptions = new Vector<Exception>();
        ExceptionListener el = new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                exceptions.addElement(e);
            }
        };

        xmlDecoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes),
            this, el);
        assertEquals(el, xmlDecoder.getExceptionListener());
        assertEquals(this, xmlDecoder.getOwner());
    }

    @Test
    public void testClose() {
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        assertEquals(Integer.valueOf("1"), dec.readObject());

        dec.close();

        assertEquals(Integer.valueOf("2"), dec.readObject());
        assertEquals(Integer.valueOf("3"), dec.readObject());
    }

    @Test
    public void testGetExceptionListener() {
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        assertNotNull(dec.getExceptionListener());
    }

    @Test
    public void testGetOwner() {
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        assertNull(dec.getOwner());
    }

    @Test
    public void testReadObject_ArrayOutOfBounds() {
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        assertEquals(Integer.valueOf("1"), dec.readObject());
        assertEquals(Integer.valueOf("2"), dec.readObject());
        assertEquals(Integer.valueOf("3"), dec.readObject());

        try {
            dec.readObject();
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testReadObject_Repeated() throws Exception {
        final Vector<Exception> exceptionList = new Vector<Exception>();

        final ExceptionListener exceptionListener = new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                exceptionList.addElement(e);
            }
        };

        SafeXMLDecoder xmlDecoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(
            xml123bytes));
        xmlDecoder.setExceptionListener(exceptionListener);
        assertEquals(new Integer(1), xmlDecoder.readObject());
        assertEquals(new Integer(2), xmlDecoder.readObject());
        assertEquals(new Integer(3), xmlDecoder.readObject());
        xmlDecoder.close();
        assertEquals(0, exceptionList.size());
    }

    @Test
    public void testSetExceptionListener_Called() throws Exception {
        class MockExceptionListener implements ExceptionListener {

            private boolean isCalled = false;

            public void exceptionThrown(Exception e) {
                isCalled = true;
            }

            public boolean isCalled() {
                return isCalled;
            }
        }

        SafeXMLDecoder xmlDecoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(
            "<java><string/>".getBytes("UTF-8")));
        MockExceptionListener mockListener = new MockExceptionListener();
        xmlDecoder.setExceptionListener(mockListener);

        assertFalse(mockListener.isCalled());
        // Real Parsing should occur in method of ReadObject rather constructor.
        assertNotNull(xmlDecoder.readObject());
        assertTrue(mockListener.isCalled());
    }

    @Test
    public void testSetExceptionListener() {
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        Object defaultL = dec.getExceptionListener();

        dec.setExceptionListener(null);
        assertSame(defaultL, dec.getExceptionListener());

        ExceptionListener newL = ignoreListener;
        dec.setExceptionListener(newL);
        assertSame(newL, dec.getExceptionListener());
    }

    @Test
    public void testSetOwner() {
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        assertNull(dec.getOwner());

        String owner = "owner";
        dec.setOwner(owner);
        assertSame(owner, dec.getOwner());

        dec.setOwner(null);
        assertNull(dec.getOwner());
    }

    /*
     * Class under test for void SafeXMLDecoder(java.io.InputStream)
     */
    @Test
    public void testXMLDecoderInputStream() {
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes));
        assertNull(dec.getOwner());
        assertNotNull(dec.getExceptionListener());
    }

    @Test
    public void testDecodeReference_success() throws Exception {

        when(classFilter.permitMethod(Matchers.<Method>any())).thenReturn(true);
        when(classFilter.permitClass("com.l7tech.util.SafeXMLDecoderTest$SampleBean")).thenReturn(true);
        when(classFilter.permitConstructor(Matchers.<Constructor>any())).thenReturn(true);

        Introspector.setBeanInfoSearchPath(new String[] {});

        try (SafeXMLDecoder d = new SafeXMLDecoder(classFilter, new ByteArrayInputStream(beanXml(makeSampleBean()).getBytes()), null, failListener)) {
            SampleBean bean;
            bean = (SampleBean) d.readObject();
            assertNotNull(bean);
            assertEquals(SampleBean.class, bean.getClass());
            assertEquals("new name", bean.getMyid());
            assertEquals(3, bean.getI());
            assertEquals("referenced bean", bean.getRef().getMyid());
            assertEquals(44, bean.getRef().getI());
            assertNull(bean.getRef().getRef());

            try {
                d.readObject();
                fail("expected index out of bounds");
            } catch (ArrayIndexOutOfBoundsException e) {
                // Ok
            }
        }
    }

    @Test
    public void testDecodeReference_classNotWhitelisted() throws Exception {
        when(classFilter.permitMethod(Matchers.<Method>any())).thenReturn(true);
        when(classFilter.permitClass(anyString())).thenReturn(false);
        when(classFilter.permitConstructor(Matchers.<Constructor>any())).thenReturn(true);

        try (SafeXMLDecoder d = new SafeXMLDecoder(classFilter, new ByteArrayInputStream(beanXml(makeSampleBean()).getBytes()), null, failListener)) {
            try {
                d.readObject();
                fail("expected ClassNotPermittedException");
            } catch (SafeXMLDecoder.ClassNotPermittedException e) {
                // Ok
            }
        }
    }

    /*
     * Class under test for void SafeXMLDecoder(java.io.InputStream,
     * java.lang.Object)
     */
    @Test
    public void testXMLDecoderInputStreamObject() {
        String owner = "owner";
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes),
            owner);
        assertSame(owner, dec.getOwner());
        assertNotNull(dec.getExceptionListener());
    }

    /*
     * Class under test for void SafeXMLDecoder(java.io.InputStream,
     * java.lang.Object, java.beans.ExceptionListener)
     */
    @Test
    public void testXMLDecoderInputStreamObjectExceptionListener() {
        String owner = "owner";
        ExceptionListener l = ignoreListener;
        SafeXMLDecoder dec = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes),
            owner, l);
        assertSame(owner, dec.getOwner());
        assertSame(l, dec.getExceptionListener());
    }

    /**
     * Regression test for HARMONY-1890
     */
    @Test
    public void testDecodeEmptyStringArray1890() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(out);
        SafeXMLDecoder decoder;
        Object obj;

        encoder.writeObject(new String[10]);
        encoder.close();

        decoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(out.toByteArray()));
        obj = decoder.readObject();
        decoder.close();
        assertTrue("Returned object is not array", obj.getClass().isArray());
        assertSame("String type expected", String.class, obj.getClass()
            .getComponentType());
        assertEquals("Size mismatch", 10, Array.getLength(obj));
    }

    private void decode(String resourceName) throws Exception {
        SafeXMLDecoder d = null;
        try {
            Introspector.setBeanInfoSearchPath(new String[] {});
            d = new SafeXMLDecoder(openFilter, new BufferedInputStream(ClassLoader
                .getSystemClassLoader().getResourceAsStream(resourceName)));
            while (true) {
                d.readObject();
            }
        } catch (ArrayIndexOutOfBoundsException aibe) {
            assertTrue(true);
        } finally {
            if (d != null) {
                d.close();
            }
        }
    }

    public static class SampleBean {
        String myid = "default ID";

        int i = 1;

        SampleBean ref;

        public SampleBean() {
            System.out.println();
        }

        public String getMyid() {
            return myid;
        }

        public void setMyid(String myid) {
            this.myid = myid;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public SampleBean getRef() {
            return ref;
        }

        public void setRef(SampleBean ref) {
            this.ref = ref;
        }

        @Override
        public String toString() {
            String superResult = super.toString();
            superResult.substring(superResult.indexOf("@"));
            return "myid=" + myid;
        }
    }

    private static String beanXml(SampleBean b) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(baos);
        enc.writeObject(b);
        enc.close();
        return baos.toString();
    }

    private static SampleBean makeSampleBean() {
        SampleBean ref = new SampleBean();
        ref.setI(44);
        ref.setMyid("referenced bean");

        SampleBean b = new SampleBean();
        b.setI(3);
        b.setMyid("new name");
        b.setRef(ref);
        return b;
    }
}
