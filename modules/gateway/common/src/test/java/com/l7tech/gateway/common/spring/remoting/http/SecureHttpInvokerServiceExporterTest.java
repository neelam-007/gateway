package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.test.BugId;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.SyspropUtil;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.*;
import java.util.Arrays;
import java.util.Set;

public class SecureHttpInvokerServiceExporterTest {

    @Before
    public void setUp() throws Exception {
        // enable security
        SyspropUtil.setProperty("com.l7tech.gateway.common.spring.remoting.http.restrictClasses", "true");
    }

    @After
    public void tearDown() throws Exception {
    }

    @SuppressWarnings("UnusedDeclaration")
    public static interface TestService {
        public String method1(String arg);
        public byte[] method2(byte[] arg);
        public int method3(int arg);
    }

    @Test
    public void testPermittedClassNames() throws Exception {
        // sample data
        final String TEST_STRING = "test string";
        final byte[] TEST_BYTES = "test bytes".getBytes(Charsets.UTF8);
        // create our SecureHttpInvokerServiceExporter
        final SecureHttpInvokerServiceExporter exporter = createSecureHttpInvokerServiceExporter(
                new TestService() {
                    @Override
                    public String method1(String arg) {
                        Assert.assertThat(arg, Matchers.allOf(Matchers.not(Matchers.isEmptyOrNullString()), Matchers.equalTo(TEST_STRING)));
                        return arg;
                    }
                    @Override
                    public byte[] method2(byte[] arg) {
                        Assert.assertNotNull(arg);
                        Assert.assertTrue(Arrays.equals(arg, TEST_BYTES));
                        return arg;
                    }
                    @Override
                    public int method3(int arg) {
                        //Assert.fail("method3 shouldn't be call");
                        return 0;
                    }
                },
                TestService.class,
                // wir-upe AdminLogin permitted classes as per admin-servlet.xml
                CollectionUtils.set(
                        "[B",
                        "[Ljava.lang.Class;",
                        "[Ljava.lang.Object;",
                        "java.lang.Class",
                        "java.lang.String",
                        "org.springframework.remoting.support.RemoteInvocation"
                )
        );

        // test for sample string
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(createSampleContent(TEST_STRING, "method1"));
        exporter.handleRequest(request, response);
        RemoteInvocationResult result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNotNull(result);
        Assert.assertNull(result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String.class), Matchers.equalTo((Object) TEST_STRING)));

        // test for bytes
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(TEST_BYTES, "method2"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull(result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(byte[].class)));
        Assert.assertTrue(Arrays.equals((byte[]) result.getValue(), TEST_BYTES));

        // test for not allowed arg e.g. int
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(10, "method3"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("non-whitelisted integer shouldn't have been deserialized", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.anyOf(
                        Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class 'I'"),
                        Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class 'int'"),
                        Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class 'java.lang.Integer'")
                )
        );
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // bunch of test classes
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class UnsafeClass implements Serializable {
    }

    public static class SafeClass implements Serializable {
    }

    public static class SafeClassWithSafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClass ignore = new SafeClass();
    }

    public static class SafeClassWithUnsafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final UnsafeClass ignore = new UnsafeClass();
    }

    public static class SafeClassInheritUnsafeClass extends UnsafeClass {
    }

    public static class SafeClassWithSafeFieldInheritUnsafeClass implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClassInheritUnsafeClass ignore = new SafeClassInheritUnsafeClass();
    }

    private static final class UnsafeClassWithSafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClass ignore = new SafeClass();
    }

    @SuppressWarnings("UnusedDeclaration")
    public static interface TestServiceInheritance {
        public int method1(UnsafeClass arg);
        public int method2(SafeClass arg);
        public int method3(SafeClassWithSafeField arg);
        public int method4(SafeClassWithUnsafeField arg);
        public int method5(SafeClassInheritUnsafeClass arg);
        public int method6(SafeClassWithSafeFieldInheritUnsafeClass arg);
        public int method7(UnsafeClassWithSafeField arg);
    }

    static private TestServiceInheritance newTestServiceInheritanceInstance() {
        return new TestServiceInheritance() {
            @Override
            public int method1(final UnsafeClass arg) {
                Assert.assertNotNull(arg);
                //Assert.fail("method1 with UnsafeClass shouldn't be call");
                return 1;
            }
            @Override
            public int method2(final SafeClass arg) {
                Assert.assertNotNull(arg);
                return 2;
            }
            @Override
            public int method3(final SafeClassWithSafeField arg) {
                Assert.assertNotNull(arg);
                return 3;
            }
            @Override
            public int method4(final SafeClassWithUnsafeField arg) {
                Assert.assertNotNull(arg);
                //Assert.fail("method4 with SafeClassWithUnsafeField shouldn't be call");
                return 4;
            }
            @Override
            public int method5(final SafeClassInheritUnsafeClass arg) {
                Assert.assertNotNull(arg);
                //Assert.fail("method5 with SafeClassInheritUnsafeClass shouldn't be call");
                return 5;
            }
            @Override
            public int method6(final SafeClassWithSafeFieldInheritUnsafeClass arg) {
                Assert.assertNotNull(arg);
                //Assert.fail("method6 with SafeClassWithSafeFieldInheritUnsafeClass shouldn't be call");
                return 6;
            }
            @Override
            public int method7(final UnsafeClassWithSafeField arg) {
                Assert.assertNotNull(arg);
                //Assert.fail("method7 with UnsafeClassWithSafeField shouldn't be call");
                return 7;
            }
        };
    }

    @Test
    public void testWhitelistClassesWithInheritance() throws Exception {
        // create our SecureHttpInvokerServiceExporter
        final SecureHttpInvokerServiceExporter exporter = createSecureHttpInvokerServiceExporter(
                newTestServiceInheritanceInstance(),
                TestServiceInheritance.class,
                // wir-upe AdminLogin permitted classes as per admin-servlet.xml
                CollectionUtils.set(
                        "[Ljava.lang.Class;",
                        "[Ljava.lang.Object;",
                        "java.lang.Class",
                        "java.lang.String",
                        "org.springframework.remoting.support.RemoteInvocation",
                        SafeClass.class.getName(),
                        SafeClassWithSafeField.class.getName(),
                        SafeClassWithUnsafeField.class.getName(),
                        SafeClassInheritUnsafeClass.class.getName(),
                        SafeClassWithSafeFieldInheritUnsafeClass.class.getName()
                )
        );

        // test for not allowed UnsafeClass
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new UnsafeClass(), "method1"));
        exporter.handleRequest(request, response);
        RemoteInvocationResult result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method1' called: 'UnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for allowed SafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new SafeClass(), "method2"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method2' wasn't called: 'SafeClass' should have been successfully deserialized.", result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Integer.class), Matchers.equalTo((Object)2)));

        // test for allowed SafeClassWithSafeField
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new SafeClassWithSafeField(), "method3"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method3' wasn't called: 'SafeClassWithSafeField' should have been successfully deserialized.", result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Integer.class), Matchers.equalTo((Object)3)));

        // test for not allowed SafeClassWithUnsafeField
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new SafeClassWithUnsafeField(), "method4"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method4' called: 'SafeClassWithUnsafeField' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed SafeClassInheritUnsafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new SafeClassInheritUnsafeClass(), "method5"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method5' called: 'SafeClassInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed SafeClassWithSafeFieldInheritUnsafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new SafeClassWithSafeFieldInheritUnsafeClass(), "method6"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method6' called: 'SafeClassWithSafeFieldInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed UnsafeClassWithSafeField
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new UnsafeClassWithSafeField(), "method7"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method7' called: 'UnsafeClassWithSafeField' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClassWithSafeField.class.getName() + "'")
        );
    }

    @BugId("SSG-12998")
    @Test
    public void testInheritFromNonWhitelistClasses() throws Exception {
        // create our SecureHttpInvokerServiceExporter
        final SecureHttpInvokerServiceExporter exporter = createSecureHttpInvokerServiceExporter(
                newTestServiceInheritanceInstance(),
                TestServiceInheritance.class,
                // wir-upe AdminLogin permitted classes as per admin-servlet.xml
                CollectionUtils.set(
                        "[Ljava.lang.Class;",
                        "[Ljava.lang.Object;",
                        "java.lang.Class",
                        "java.lang.String",
                        "org.springframework.remoting.support.RemoteInvocation",
                        SafeClass.class.getName(),
                        SafeClassWithSafeField.class.getName(),
                        SafeClassWithUnsafeField.class.getName(),
                        SafeClassInheritUnsafeClass.class.getName(),
                        SafeClassWithSafeFieldInheritUnsafeClass.class.getName()
                )
        );

        // test for not allowed SafeClassInheritUnsafeClass
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new SafeClassInheritUnsafeClass(), "method5"));
        exporter.handleRequest(request, response);
        RemoteInvocationResult result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method5' called: 'SafeClassInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed SafeClassWithSafeFieldInheritUnsafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent(new SafeClassWithSafeFieldInheritUnsafeClass(), "method6"));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method6' called: 'SafeClassWithSafeFieldInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SecureHttpInvokerServiceExporter.ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static private <T> SecureHttpInvokerServiceExporter createSecureHttpInvokerServiceExporter(
            final T service,
            final Class<? extends T> serviceClass,
            final Set<String> permittedClassNames
    ) throws Exception {
        return createSecureHttpInvokerServiceExporter(null, service, serviceClass, permittedClassNames);
    }

    static private <T> SecureHttpInvokerServiceExporter createSecureHttpInvokerServiceExporter(
            final ClassLoader classLoader,
            final T service,
            final Class<? extends T> serviceClass,
            final Set<String> permittedClassNames
    ) throws Exception {
        Assert.assertNotNull(service);
        Assert.assertNotNull(serviceClass);
        Assert.assertThat(service, Matchers.instanceOf(serviceClass));

        final SecureHttpInvokerServiceExporter exporter = new SecureHttpInvokerServiceExporter();
        exporter.setPermittedClassNames(permittedClassNames);
        exporter.setModuleClassLoader(classLoader != null ? classLoader : SecureHttpInvokerServiceExporterTest.class.getClassLoader());
        exporter.setService(service);
        exporter.setServiceInterface(serviceClass);
        exporter.prepare();
        return exporter;
    }

    /**
     * Creates a sample serialized byte array from the specified {@code object} and wraps it inside a {@link RemoteInvocation}<br/>
     * This simulates remote invocation content.
     */
    static private byte[] createSampleContent(final Object object, final String methodName) throws Exception {
        Assert.assertNotNull(object);

        // create simple remote invocation object
        final RemoteInvocation invocation = new RemoteInvocation(
                methodName,
                new Class[] {object.getClass()},
                new Object[] {object}
        );

        try (
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos)
        ) {
            oos.writeObject(invocation);
            bos.flush();
            return bos.toByteArray();
        }
    }

    /**
     * Extract the expected object (specified with {@code tClass}) from the serialized {@code content} byte array.
     */
    static private <T> T extractInvocationResult(final byte[] content, Class<? extends T> tClass) throws Exception {
        Assert.assertNotNull(content);
        Assert.assertThat(content.length, Matchers.greaterThan(0));
        Assert.assertNotNull(tClass);

        try (
                final ByteArrayInputStream bis = new ByteArrayInputStream(content);
                final ObjectInputStream ois = new ObjectInputStream(bis)
        ) {
            final Object obj = ois.readObject();
            Assert.assertThat(obj, Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(tClass)));
            return tClass.cast(obj);
        }
    }
}