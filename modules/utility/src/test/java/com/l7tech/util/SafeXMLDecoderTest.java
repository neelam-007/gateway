package com.l7tech.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.beans.ExceptionListener;
import java.beans.Introspector;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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

    @After
    public void cleanup() {
        SyspropUtil.clearProperties(
            SafeXMLDecoder.PROP_DISABLE_FILTER,
            ClassFilterBuilder.PROP_WHITELIST_CLASSES,
            ClassFilterBuilder.PROP_WHITELIST_CONSTRUCTORS,
            ClassFilterBuilder.PROP_WHITELIST_METHODS
        );
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

        ExceptionListener el = new ExceptionListener() {
            public void exceptionThrown(Exception e) {
            }
        };

        xmlDecoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml123bytes), this, el);
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
        final List<Exception> exceptionList = new ArrayList<>();

        final ExceptionListener exceptionListener = new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                exceptionList.add(e);
            }
        };

        SafeXMLDecoder xmlDecoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(
            xml123bytes));
        xmlDecoder.setExceptionListener(exceptionListener);
        assertEquals(1, xmlDecoder.readObject());
        assertEquals(2, xmlDecoder.readObject());
        assertEquals(3, xmlDecoder.readObject());
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

        Introspector.setBeanInfoSearchPath(new String[]{});

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
            } catch (ClassNotPermittedException e) {
                // Ok
            }
        }
    }

    @Test
    public void testDecodeReference_constructorNotWhitelisted() throws Exception {
        when(classFilter.permitMethod(Matchers.<Method>any())).thenReturn(true);
        when(classFilter.permitClass(anyString())).thenReturn(true);
        when(classFilter.permitConstructor(Matchers.<Constructor>any())).thenReturn(false);

        try (SafeXMLDecoder d = new SafeXMLDecoder(classFilter, new ByteArrayInputStream(beanXml(makeSampleBean()).getBytes()), null, failListener)) {
            try {
                d.readObject();
                fail("expected ConstructorNotPermittedException");
            } catch (SafeXMLDecoder.ConstructorNotPermittedException e) {
                // Ok
            }
        }
    }

    @Test
    public void testDecodeReference_methodNotWhitelisted() throws Exception {
        when(classFilter.permitMethod(Matchers.<Method>any())).thenReturn(false);
        when(classFilter.permitClass(anyString())).thenReturn(true);
        when(classFilter.permitConstructor(Matchers.<Constructor>any())).thenReturn(true);

        try (SafeXMLDecoder d = new SafeXMLDecoder(classFilter, new ByteArrayInputStream(beanXml(makeSampleBean()).getBytes()), null, failListener)) {
            try {
                d.readObject();
                fail("expected MethodNotPermittedException");
            } catch (SafeXMLDecoder.MethodNotPermittedException e) {
                // Ok
            }
        }
    }

    @Test
    public void testDecodeReference_strictFilter_filteringDisabled() throws Exception {
        when(classFilter.permitMethod(Matchers.<Method>any())).thenReturn(false);
        when(classFilter.permitClass(anyString())).thenReturn(false);
        when(classFilter.permitConstructor(Matchers.<Constructor>any())).thenReturn(false);

        SyspropUtil.setProperty("com.l7tech.util.SafeXMLDecoder.disableAllFiltering", "true");
        try (SafeXMLDecoder d = new SafeXMLDecoder(classFilter, new ByteArrayInputStream(beanXml(makeSampleBean()).getBytes()), null, failListener)) {
            Object got = d.readObject();
            assertNotNull(got);
        } finally {
            SyspropUtil.clearProperty("com.l7tech.util.SafeXMLDecoder.disableAllFiltering");
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

    @Test
    public void testDecodeArray() {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<java version=\"1.7.0_03\" class=\"java.beans.XMLDecoder\">\n" +
                " <array class=\"java.lang.Object\" length=\"3\">\n" +
                "  <void index=\"0\">\n" +
                "   <object class=\"java.util.HashMap\">\n" +
                "    <void method=\"put\">\n" +
                "     <string>file.format</string>\n" +
                "     <string>STANDARD</string>\n" +
                "    </void>\n" +
                "    <void method=\"put\">\n" +
                "     <string>file.maxSize</string>\n" +
                "     <string>1024</string>\n" +
                "    </void>\n" +
                "    <void method=\"put\">\n" +
                "     <string>file.logCount</string>\n" +
                "     <string>2</string>\n" +
                "    </void>\n" +
                "   </object>\n" +
                "  </void>\n" +
                "  <void index=\"1\">\n" +
                "   <object class=\"java.util.ArrayList\"/>\n" +
                "  </void>\n" +
                "  <void index=\"2\">\n" +
                "   <object class=\"java.util.HashMap\">\n" +
                "    <void method=\"put\">\n" +
                "     <string>policy-id</string>\n" +
                "     <object class=\"java.util.ArrayList\">\n" +
                "      <void method=\"add\">\n" +
                "       <string>241696774</string>\n" +
                "      </void>\n" +
                "     </object>\n" +
                "    </void>\n" +
                "   </object>\n" +
                "  </void>\n" +
                " </array>\n" +
                "</java>\n";

        ClassFilter testFilter = new ClassFilterBuilder().allowDefaults().build();

        SafeXMLDecoder decoder = new SafeXMLDecoder(testFilter, new ByteArrayInputStream(xml.getBytes(Charsets.UTF8)), null, failListener);
        Object obj = decoder.readObject();
        decoder.close();
        assertTrue("Returned object is not array", obj.getClass().isArray());
        assertSame("Array type expected", Object.class, obj.getClass().getComponentType());
        assertEquals("Size mismatch", 3, Array.getLength(obj));
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
