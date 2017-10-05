package com.l7tech.server.spring.remoting.http;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gateway.common.spring.remoting.http.SecureHttpInvokerServiceExporter;
import com.l7tech.gateway.common.spring.remoting.http.SecureHttpInvokerServiceExporterStub;
import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.solutionkit.SolutionKitBuilder;
import com.l7tech.test.BugId;
import com.l7tech.util.*;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.Primitives;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("serial")
@RunWith(MockitoJUnitRunner.class)
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

    // sample data
    private final String TEST_STRING = "test string";
    private final byte[] TEST_BYTES = "test bytes".getBytes(Charsets.UTF8);

    @Test
    public void testPermittedClassNames() throws Exception {
        // create our test service instance
        final TestService testInstance = new TestService() {
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
        };

        final String[] whitelistedClasses = new String[] {
                "[B",
                "[Ljava.lang.Class;",
                "[Ljava.lang.Object;",
                "java.lang.Class",
                "java.lang.String",
                "org.springframework.remoting.support.RemoteInvocation"
        };

        // test SecureHttpInvokerServiceExporter using permittedClasses i.e. RestrictedCodebaseAwareObjectInputStream
        doTestPermittedClassNames(
                createSecureHttpInvokerServiceExporter(
                        testInstance,
                        TestService.class,
                        CollectionUtils.set(whitelistedClasses)
                )
        );

        // test SecureHttpInvokerServiceExporter using ClassFilter i.e. ClassFilterCodebaseAwareObjectInputStream
        doTestPermittedClassNames(
                createClassFilterSecureHttpInvokerServiceExporter(
                        testInstance,
                        TestService.class,
                        new ClassFilterBuilder().addClasses(
                                false,
                                whitelistedClasses
                        ).build()
                )
        );
    }

    private void doTestPermittedClassNames(final SecureHttpInvokerServiceExporter exporter) throws Exception {
        Assert.assertNotNull(exporter);

        // test for sample string
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method1", TEST_STRING));
        exporter.handleRequest(request, response);
        RemoteInvocationResult result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNotNull(result);
        Assert.assertNull(result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String.class), Matchers.equalTo((Object) TEST_STRING)));

        // test for bytes
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method2", (Object)TEST_BYTES));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull(result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(byte[].class)));
        Assert.assertTrue(Arrays.equals((byte[]) result.getValue(), TEST_BYTES));

        // test for not allowed arg e.g. int
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method3", 10));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("non-whitelisted integer shouldn't have been deserialized", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
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

    private static final class UnsafeClassWithSafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClass ignore = new SafeClass();
    }

    @DeserializeSafe
    public static class SafeClass implements Serializable {
    }

    @DeserializeSafe
    public static class SafeClassWithSafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClass ignore = new SafeClass();
    }

    @DeserializeSafe
    public static class SafeClassWithUnsafeField implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final UnsafeClass ignore = new UnsafeClass();
    }

    @DeserializeSafe
    public static class SafeClassInheritUnsafeClass extends UnsafeClass {
    }

    @DeserializeSafe
    public static class SafeClassWithSafeFieldInheritUnsafeClass implements Serializable {
        @SuppressWarnings("UnusedDeclaration")
        private final SafeClassInheritUnsafeClass ignore = new SafeClassInheritUnsafeClass();
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
        final String[] whitelistedClasses = new String[] {
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
        };

        // test SecureHttpInvokerServiceExporter using permittedClasses i.e. RestrictedCodebaseAwareObjectInputStream
        doTestWhitelistClassesWithInheritance(
                createSecureHttpInvokerServiceExporter(
                        newTestServiceInheritanceInstance(),
                        TestServiceInheritance.class,
                        CollectionUtils.set(whitelistedClasses)
                )
        );

        // test SecureHttpInvokerServiceExporter using ClassFilter i.e. ClassFilterCodebaseAwareObjectInputStream
        doTestWhitelistClassesWithInheritance(
                createClassFilterSecureHttpInvokerServiceExporter(
                        newTestServiceInheritanceInstance(),
                        TestServiceInheritance.class,
                        new ClassFilterBuilder().addClasses(
                                false,
                                whitelistedClasses
                        ).build()
                )
        );
    }

    private void doTestWhitelistClassesWithInheritance(final SecureHttpInvokerServiceExporter exporter) throws Exception {
        Assert.assertNotNull(exporter);

        // test for not allowed UnsafeClass
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method1", new UnsafeClass()));
        exporter.handleRequest(request, response);
        RemoteInvocationResult result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method1' called: 'UnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for allowed SafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method2", new SafeClass()));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method2' wasn't called: 'SafeClass' should have been successfully deserialized.", result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Integer.class), Matchers.equalTo((Object)2)));

        // test for allowed SafeClassWithSafeField
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method3", new SafeClassWithSafeField()));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method3' wasn't called: 'SafeClassWithSafeField' should have been successfully deserialized.", result.getException());
        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Integer.class), Matchers.equalTo((Object)3)));

        // test for not allowed SafeClassWithUnsafeField
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method4", new SafeClassWithUnsafeField()));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method4' called: 'SafeClassWithUnsafeField' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed SafeClassInheritUnsafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method5", new SafeClassInheritUnsafeClass()));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method5' called: 'SafeClassInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed SafeClassWithSafeFieldInheritUnsafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method6", new SafeClassWithSafeFieldInheritUnsafeClass()));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method6' called: 'SafeClassWithSafeFieldInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed UnsafeClassWithSafeField
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method7", new UnsafeClassWithSafeField()));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method7' called: 'UnsafeClassWithSafeField' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClassWithSafeField.class.getName() + "'")
        );
    }

    @BugId("SSG-12998")
    @Test
    public void testInheritFromNonWhitelistClasses() throws Exception {
        // wir-upe AdminLogin permitted classes as per admin-servlet.xml
        final String[] whitelistedClasses = new String[] {
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
        };

        // test SecureHttpInvokerServiceExporter using permittedClasses i.e. RestrictedCodebaseAwareObjectInputStream
        doTestInheritFromNonWhitelistClasses(
                createSecureHttpInvokerServiceExporter(
                        newTestServiceInheritanceInstance(),
                        TestServiceInheritance.class,
                        CollectionUtils.set(whitelistedClasses)
                )
        );

        // test SecureHttpInvokerServiceExporter using ClassFilter i.e. ClassFilterCodebaseAwareObjectInputStream
        doTestInheritFromNonWhitelistClasses(
                createClassFilterSecureHttpInvokerServiceExporter(
                        newTestServiceInheritanceInstance(),
                        TestServiceInheritance.class,
                        new ClassFilterBuilder().addClasses(
                                false,
                                whitelistedClasses
                        ).build()
                )
        );
    }

    private void doTestInheritFromNonWhitelistClasses(final SecureHttpInvokerServiceExporter exporter) throws Exception {
        Assert.assertNotNull(exporter);

        // test for not allowed SafeClassInheritUnsafeClass
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method5", new SafeClassInheritUnsafeClass()));
        exporter.handleRequest(request, response);
        RemoteInvocationResult result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method5' called: 'SafeClassInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
        //noinspection ThrowableResultOfMethodCallIgnored
        Assert.assertThat(
                result.getException().getMessage(),
                Matchers.equalToIgnoringCase("Attempt to deserialize non-whitelisted class '" + UnsafeClass.class.getName() + "'")
        );

        // test for not allowed SafeClassWithSafeFieldInheritUnsafeClass
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setContent(createSampleContent("method6", new SafeClassWithSafeFieldInheritUnsafeClass()));
        exporter.handleRequest(request, response);
        result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
        Assert.assertNull("'method6' called: 'SafeClassWithSafeFieldInheritUnsafeClass' shouldn't have been deserialized.", result.getValue());
        Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
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
        exporter.setBypassDeserializationClassFilter(true);
        exporter.prepare();
        return exporter;
    }

    /**
     * Creates a sample serialized byte array from the specified {@code object} and wraps it inside a {@link RemoteInvocation}<br/>
     * This simulates remote invocation content.
     */
    static private byte[] createSampleContent(final String methodName, final Object ... objects) throws Exception {
        // create simple remote invocation object
        final RemoteInvocation invocation = new RemoteInvocation(
                methodName,
                getClasses(objects),
                getObjects(objects)
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
     * extracts specified {@code objects} classes.
     */
    static private Class[] getClasses(final Object[] objects) throws Exception {
        if (objects != null) {
            final Class[] classes = new Class[objects.length];
            for (int i = 0; i < objects.length; ++i) {
                final Object obj = objects[i];
                Assert.assertNotNull(obj);
                classes[i] = obj instanceof Primitive ? ((Primitive) obj).getPrimClass() : obj.getClass();
            }
            return classes;
        }
        return new Class[0];
    }

    static private Object[] getObjects(final Object[] objects) throws Exception {
        if (objects != null) {
            final Object[] retObjects = new Object[objects.length];
            for (int i = 0; i < objects.length; ++i) {
                final Object obj = objects[i];
                Assert.assertNotNull(obj);
                retObjects[i] = obj instanceof Primitive ? ((Primitive) obj).getValue() : obj;
            }
            return retObjects;
        }
        return new Object[0];
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

    public static interface BlankServiceInterface {
    }

    @Test
    public void testIsDeserializationClassFilterDisabled() throws Exception {
        final BlankServiceInterface dummyService = new BlankServiceInterface() { };
        SecureHttpInvokerServiceExporter exporter = createClassFilterSecureHttpInvokerServiceExporter(
                null,
                dummyService,
                BlankServiceInterface.class,
                null,
                null
        );
        Assert.assertFalse(exporter.isDeserializationClassFilterDisabled());

        exporter = createClassFilterSecureHttpInvokerServiceExporter(
                null,
                dummyService,
                BlankServiceInterface.class,
                false,
                null
        );
        Assert.assertFalse(exporter.isDeserializationClassFilterDisabled());

        exporter = createClassFilterSecureHttpInvokerServiceExporter(
                null,
                dummyService,
                BlankServiceInterface.class,
                true,
                null
        );
        Assert.assertTrue(exporter.isDeserializationClassFilterDisabled());
    }

    @Test
    public void testTrustedSignerCertsAdminWhitelist() throws Exception {
        // create our SecureHttpInvokerServiceExporter
        final AtomicBoolean methodCalled = new AtomicBoolean(false);
        final SecureHttpInvokerServiceExporter exporter = createClassFilterSecureHttpInvokerServiceExporter(
                new TrustedSignerCertsAdmin() {
                    @NotNull
                    @Override
                    public Collection<X509Certificate> lookUpTrustedSigningCertsForServerModuleFiles() throws FindException {
                        methodCalled.set(true);
                        return Collections.emptyList();
                    }

                    @NotNull
                    @Override
                    public Collection<X509Certificate> lookUpTrustedSigningCertsForSolutionKits() throws FindException {
                        methodCalled.set(true);
                        return Collections.emptyList();
                    }
                },
                TrustedSignerCertsAdmin.class
        );

        final Functions.UnaryVoid<RemoteInvocationResult> defaultNoErrorTester = new Functions.UnaryVoid<RemoteInvocationResult>() {
            @Override
            public void call(final RemoteInvocationResult result) {
                Assert.assertNotNull(result);
                Assert.assertTrue("method is executed", methodCalled.get());
                Assert.assertNull(result.getException());
                Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Collection.class)));
            }
        };

        final Functions.UnaryVoid<RemoteInvocationResult> defaultMethodNotExecutedTester = new Functions.UnaryVoid<RemoteInvocationResult>() {
            @Override
            public void call(final RemoteInvocationResult result) {
                Assert.assertNotNull(result);
                Assert.assertFalse("method is not executed", methodCalled.get());
                Assert.assertNull(result.getValue());
                Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(NoSuchMethodException.class)));
            }
        };

        final Functions.UnaryVoid<RemoteInvocationResult> defaultClassNotPermitedTester = new Functions.UnaryVoid<RemoteInvocationResult>() {
            @Override
            public void call(final RemoteInvocationResult result) {
                Assert.assertNotNull(result);
                Assert.assertFalse("method is not executed", methodCalled.get());
                Assert.assertNull(result.getValue());
                Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
            }
        };

        // test lookUpTrustedSigningCertsForServerModuleFiles
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(defaultNoErrorTester, "lookUpTrustedSigningCertsForServerModuleFiles"),
                        testMethod(defaultMethodNotExecutedTester, "lookUpTrustedSigningCertsForServerModuleFiles", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "lookUpTrustedSigningCertsForServerModuleFiles", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "lookUpTrustedSigningCertsForServerModuleFiles", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "lookUpTrustedSigningCertsForServerModuleFiles", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test lookUpTrustedSigningCertsForSolutionKits
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(defaultNoErrorTester, "lookUpTrustedSigningCertsForSolutionKits"),
                        testMethod(defaultMethodNotExecutedTester, "lookUpTrustedSigningCertsForSolutionKits", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "lookUpTrustedSigningCertsForSolutionKits", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "lookUpTrustedSigningCertsForSolutionKits", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "lookUpTrustedSigningCertsForSolutionKits", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );
    }

    static private final AsyncAdminMethods.JobId<String> TEST_STRING_JOB_ID = new AsyncAdminMethods.JobId<>("string".getBytes(Charsets.UTF8), String.class);
    static private final AsyncAdminMethods.JobId<Goid> TEST_GOID_JOB_ID = new AsyncAdminMethods.JobId<>("goid".getBytes(Charsets.UTF8), Goid.class);

    private static class Primitive {
        private final Object value;
        public Primitive(final Object value) {
            Assert.assertNotNull(value);
            Assert.assertTrue(Primitives.isPrimitiveOrWrapper(value.getClass()));
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public Class<?> getPrimClass() {
            return Primitives.primitiveTypeOf(value.getClass());
        }
    }

    @Test
    public void testSolutionKitAdminWhitelist() throws Exception {
        // create a sample SolutionKit using/setting all fields
        final SolutionKit sampleSolutionKit = new SolutionKitBuilder()
                .goid(new Goid(1, 1))
                .name("sk 1")
                .version(1)
                .skGuid(UUID.randomUUID().toString())
                .skVersion("1.0")
                .mappings("mappings")
                .uninstallBundle("uninstallBundle")
                .lastUpdateTime(1L)
                .parent(new Goid(1, 0))
                .addInstallProperty("install_prop1", "install_prop1_value")
                .addInstallProperty("install_prop2", "install_prop2_value")
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "sk1 desc")
                .addOwnershipDescriptor("sk1_entity1", EntityType.JDBC_CONNECTION, true)
                .addOwnershipDescriptor("sk1_entity2", EntityType.SECURE_PASSWORD, true)
                .addOwnershipDescriptor("sk1_entity3", EntityType.EMAIL_LISTENER, false)
                .build();

        // create our SecureHttpInvokerServiceExporter
        final AtomicBoolean methodCalled = new AtomicBoolean(false);
        final SecureHttpInvokerServiceExporter exporter = createClassFilterSecureHttpInvokerServiceExporter(
                new SolutionKitAdmin() {
                    @NotNull
                    @Override
                    public List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) throws FindException {
                        methodCalled.set(true);
                        return Collections.emptyList();
                    }

                    @NotNull
                    @Override
                    public String testInstall(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) throws Exception {
                        methodCalled.set(true);
                        return TEST_STRING;
                    }

                    @NotNull
                    @Override
                    public JobId<String> testInstallAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) {
                        methodCalled.set(true);
                        return TEST_STRING_JOB_ID;
                    }

                    @NotNull
                    @Override
                    public Goid install(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) throws Exception {
                        methodCalled.set(true);
                        return Goid.DEFAULT_GOID;
                    }

                    @NotNull
                    @Override
                    public JobId<Goid> installAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) {
                        methodCalled.set(true);
                        return TEST_GOID_JOB_ID;
                    }

                    @NotNull
                    @Override
                    public String uninstall(@NotNull Goid goid) throws Exception {
                        methodCalled.set(true);
                        return TEST_STRING;
                    }

                    @NotNull
                    @Override
                    public JobId<String> uninstallAsync(@NotNull Goid goid) {
                        methodCalled.set(true);
                        return TEST_STRING_JOB_ID;
                    }

                    @NotNull
                    @Override
                    public Collection<SolutionKitHeader> findHeaders() throws FindException {
                        methodCalled.set(true);
                        return Collections.emptyList();
                    }

                    @NotNull
                    @Override
                    public Collection<SolutionKitHeader> findHeaders(@NotNull Goid parentGoid) throws FindException {
                        methodCalled.set(true);
                        return Collections.emptyList();
                    }

                    @NotNull
                    @Override
                    public Collection<SolutionKit> find(@NotNull String solutionKitGuid) throws FindException {
                        methodCalled.set(true);
                        return Collections.emptyList();
                    }

                    @NotNull
                    @Override
                    public Collection<SolutionKit> find(@NotNull Goid parentGoid) throws FindException {
                        methodCalled.set(true);
                        return Collections.emptyList();
                    }

                    @Override
                    public SolutionKit get(@NotNull Goid goid) throws FindException {
                        methodCalled.set(true);
                        return sampleSolutionKit;
                    }

                    @Override
                    public SolutionKit get(@NotNull String guid, @Nullable String instanceModifier) throws FindException {
                        methodCalled.set(true);
                        return sampleSolutionKit;
                    }

                    @NotNull
                    @Override
                    public Goid save(@NotNull SolutionKit solutionKit) throws SaveException {
                        methodCalled.set(true);
                        return Goid.DEFAULT_GOID;
                    }

                    @Override
                    public void update(@NotNull SolutionKit solutionKit) throws UpdateException {
                        methodCalled.set(true);
                    }

                    @Override
                    public void delete(@NotNull Goid goid) throws FindException, DeleteException {
                        methodCalled.set(true);
                    }

                    @Override
                    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
                        methodCalled.set(true);
                        return TEST_STRING;
                    }

                    @Override
                    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
                        methodCalled.set(true);
                        return new JobResult<>(
                                "status",
                                null,
                                "throwableClassname",
                                "throwableMessage"
                        );
                    }

                    @Override
                    public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning) {
                        methodCalled.set(true);
                    }
                },
                SolutionKitAdmin.class
        );

        final Functions.UnaryVoid<RemoteInvocationResult> defaultMethodNotExecutedTester = new Functions.UnaryVoid<RemoteInvocationResult>() {
            @Override
            public void call(final RemoteInvocationResult result) {
                Assert.assertNotNull(result);
                Assert.assertFalse("method is not executed", methodCalled.get());
                Assert.assertNull(result.getValue());
                Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(NoSuchMethodException.class)));
            }
        };

        final Functions.UnaryVoid<RemoteInvocationResult> defaultClassNotPermitedTester = new Functions.UnaryVoid<RemoteInvocationResult>() {
            @Override
            public void call(final RemoteInvocationResult result) {
                Assert.assertNotNull(result);
                Assert.assertFalse("method is not executed", methodCalled.get());
                Assert.assertNull(result.getValue());
                Assert.assertThat(result.getException(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(ClassNotPermittedException.class)));
            }
        };

        // test public List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) throws FindException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Collection.class)));
                                    }
                                },
                                "getSolutionKitsToUpgrade",
                                sampleSolutionKit
                        ),
                        testMethod(defaultMethodNotExecutedTester, "getSolutionKitsToUpgrade", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getSolutionKitsToUpgrade", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getSolutionKitsToUpgrade", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getSolutionKitsToUpgrade", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public String testInstall(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) throws Exception
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String.class), Matchers.equalTo((Object)TEST_STRING)));
                                    }
                                },
                                "testInstall",
                                sampleSolutionKit, "bundle", new Primitive(true)
                        ),
                        testMethod(defaultMethodNotExecutedTester, "testInstall", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "testInstall", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "testInstall", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "testInstall", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public JobId<String> testInstallAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade)
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(AsyncAdminMethods.JobId.class), Matchers.equalTo((Object) TEST_STRING_JOB_ID)));
                                    }
                                },
                                "testInstallAsync",
                                sampleSolutionKit, "bundle", new Primitive(true)
                        ),
                        testMethod(defaultMethodNotExecutedTester, "testInstallAsync", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "testInstallAsync", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "testInstallAsync", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "testInstallAsync", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public Goid install(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade) throws Exception
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Goid.class), Matchers.equalTo((Object) Goid.DEFAULT_GOID)));
                                    }
                                },
                                "install",
                                sampleSolutionKit, "bundle", new Primitive(true)
                        ),
                        testMethod(defaultMethodNotExecutedTester, "install", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "install", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "install", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "install", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public JobId<Goid> installAsync(@NotNull SolutionKit solutionKit, @NotNull String bundle, boolean isUpgrade)
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(AsyncAdminMethods.JobId.class), Matchers.equalTo((Object) TEST_GOID_JOB_ID)));
                                    }
                                },
                                "installAsync",
                                sampleSolutionKit, "bundle", new Primitive(true)
                        ),
                        testMethod(defaultMethodNotExecutedTester, "installAsync", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "installAsync", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "installAsync", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "installAsync", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test String uninstall(@NotNull Goid goid) throws Exception
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String.class), Matchers.equalTo((Object) TEST_STRING)));
                                    }
                                },
                                "uninstall",
                                Goid.DEFAULT_GOID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "uninstall", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "uninstall", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "uninstall", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "uninstall", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public JobId<String> uninstallAsync(@NotNull Goid goid)
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(AsyncAdminMethods.JobId.class), Matchers.equalTo((Object) TEST_STRING_JOB_ID)));
                                    }
                                },
                                "uninstallAsync",
                                Goid.DEFAULT_GOID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "uninstallAsync", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "uninstallAsync", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "uninstallAsync", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "uninstallAsync", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public Collection<SolutionKitHeader> findHeaders() throws FindException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Collection.class)));
                                    }
                                },
                                "findHeaders"
                        ),
                        testMethod(defaultMethodNotExecutedTester, "findHeaders", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "findHeaders", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "findHeaders", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "findHeaders", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public Collection<SolutionKitHeader> findHeaders(@NotNull Goid parentGoid) throws FindException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Collection.class)));
                                    }
                                },
                                "findHeaders",
                                Goid.DEFAULT_GOID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "findHeaders", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "findHeaders", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "findHeaders", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "findHeaders", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public Collection<SolutionKit> find(@NotNull String solutionKitGuid) throws FindException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Collection.class)));
                                    }
                                },
                                "find",
                                "some guid"
                        ),
                        testMethod(defaultMethodNotExecutedTester, "find", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "find", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "find", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "find", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public Collection<SolutionKit> find(@NotNull Goid parentGoid) throws FindException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Collection.class)));
                                    }
                                },
                                "find",
                                Goid.DEFAULT_GOID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "find", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "find", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "find", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "find", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public SolutionKit get(@NotNull Goid goid) throws FindException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(SolutionKit.class), Matchers.equalTo((Object) sampleSolutionKit)));
                                    }
                                },
                                "get",
                                Goid.DEFAULT_GOID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "get", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "get", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "get", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "get", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public Goid save(@NotNull SolutionKit solutionKit) throws SaveException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(Goid.class), Matchers.equalTo((Object) Goid.DEFAULT_GOID)));
                                    }
                                },
                                "save",
                                sampleSolutionKit
                        ),
                        testMethod(defaultMethodNotExecutedTester, "save", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "save", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "save", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "save", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public void update(@NotNull SolutionKit solutionKit) throws UpdateException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertNull("void return expected", result.getValue());
                                    }
                                },
                                "update",
                                sampleSolutionKit
                        ),
                        testMethod(defaultMethodNotExecutedTester, "update", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "update", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "update", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "update", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public void delete(@NotNull Goid goid) throws FindException, DeleteException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertNull("void return expected", result.getValue());
                                    }
                                },
                                "delete",
                                Goid.DEFAULT_GOID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "delete", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "delete", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "delete", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "delete", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId)
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String.class), Matchers.equalTo((Object) TEST_STRING)));
                                    }
                                },
                                "getJobStatus",
                                TEST_STRING_JOB_ID
                        ),
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(String.class), Matchers.equalTo((Object) TEST_STRING)));
                                    }
                                },
                                "getJobStatus",
                                TEST_GOID_JOB_ID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "getJobStatus", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getJobStatus", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getJobStatus", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getJobStatus", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(AsyncAdminMethods.JobResult.class)));
                                    }
                                },
                                "getJobResult",
                                TEST_STRING_JOB_ID
                        ),
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertThat(result.getValue(), Matchers.allOf(Matchers.notNullValue(), Matchers.instanceOf(AsyncAdminMethods.JobResult.class)));
                                    }
                                },
                                "getJobResult",
                                TEST_GOID_JOB_ID
                        ),
                        testMethod(defaultMethodNotExecutedTester, "getJobResult", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getJobResult", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getJobResult", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "getJobResult", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );

        // test public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning)
        testMethods(
                methodCalled,
                exporter,
                Arrays.asList(
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertNull("void return expected", result.getValue());
                                    }
                                },
                                "cancelJob",
                                TEST_STRING_JOB_ID, new Primitive(true)
                        ),
                        testMethod(
                                new Functions.UnaryVoid<RemoteInvocationResult>() {
                                    @Override
                                    public void call(RemoteInvocationResult result) {
                                        Assert.assertNotNull(result);
                                        Assert.assertTrue("method is executed", methodCalled.get());
                                        Assert.assertNull(result.getException());
                                        Assert.assertNull("void return expected", result.getValue());
                                    }
                                },
                                "cancelJob",
                                TEST_GOID_JOB_ID, new Primitive(false)
                        ),
                        testMethod(defaultMethodNotExecutedTester, "cancelJob", new SafeClass()),
                        testMethod(defaultClassNotPermitedTester, "cancelJob", new UnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "cancelJob", new SafeClassInheritUnsafeClass()),
                        testMethod(defaultClassNotPermitedTester, "cancelJob", new SafeClassWithSafeFieldInheritUnsafeClass())
                )
        );
    }

    private Triple<String, Object[], Functions.UnaryVoid<RemoteInvocationResult>> testMethod(
            final Functions.UnaryVoid<RemoteInvocationResult> resultTester,
            String methodName,
            Object ... methodArgs
    ) throws Exception {
        Assert.assertNotNull(resultTester);
        Assert.assertThat(methodName, Matchers.not(Matchers.isEmptyOrNullString()));
        return Triple.triple(
                methodName,
                methodArgs == null ? new Object[0] : methodArgs,
                resultTester
        );
    }

    private void testMethods(
            final AtomicBoolean methodCalledFlag,
            final SecureHttpInvokerServiceExporter exporter,
            final Collection<Triple<String, Object[], Functions.UnaryVoid<RemoteInvocationResult>>> methodArgs
    ) throws Exception {
        Assert.assertNotNull(methodCalledFlag);
        Assert.assertNotNull(exporter);

        for (final Triple<String, Object[], Functions.UnaryVoid<RemoteInvocationResult>> methodArg : methodArgs) {
            final String methodName = methodArg.left;
            final Object[] methodParams = methodArg.middle;
            final Functions.UnaryVoid<RemoteInvocationResult> resultTester = methodArg.right;
            Assert.assertThat(methodName, Matchers.not(Matchers.isEmptyOrNullString()));
            Assert.assertNotNull(methodParams);
            Assert.assertNotNull(resultTester);

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockHttpServletRequest request = new MockHttpServletRequest();
            methodCalledFlag.set(false);
            request.setContent(createSampleContent(methodName, methodParams));
            exporter.handleRequest(request, response);
            RemoteInvocationResult result = extractInvocationResult(response.getContentAsByteArray(), RemoteInvocationResult.class);
            Assert.assertNotNull(result);
            resultTester.call(result);
        }
    }

    /**
     * Create default {@link ClassFilter} enabled {@link SecureHttpInvokerServiceExporter}:<br/>
     * BypassDeserializationClassFilter: {@code false}<br/>
     * Deserialization {@code ClassFilter}: default i.e. {@code DeserializeClassFilter.getInstance()}<br/>
     */
    static private <T> SecureHttpInvokerServiceExporter createClassFilterSecureHttpInvokerServiceExporter(
            final T service,
            final Class<? extends T> serviceClass
    ) throws Exception {
        return createClassFilterSecureHttpInvokerServiceExporter(null, service, serviceClass, false, null);
    }

    static private <T> SecureHttpInvokerServiceExporter createClassFilterSecureHttpInvokerServiceExporter(
            final T service,
            final Class<? extends T> serviceClass,
            final ClassFilter classFilter
    ) throws Exception {
        return createClassFilterSecureHttpInvokerServiceExporter(null, service, serviceClass, false, classFilter);
    }

    static private <T> SecureHttpInvokerServiceExporter createClassFilterSecureHttpInvokerServiceExporter(
            final ClassLoader classLoader,
            final T service,
            final Class<? extends T> serviceClass,
            final Boolean bypassDeserializationClassFilter,
            final ClassFilter classFilter
    ) throws Exception {
        Assert.assertNotNull(service);
        Assert.assertNotNull(serviceClass);
        Assert.assertThat(service, Matchers.instanceOf(serviceClass));

        final SecureHttpInvokerServiceExporter exporter =
                classFilter != null
                        ? SecureHttpInvokerServiceExporterStub.mockWithClassFilterOverride(classFilter)
                        : new SecureHttpInvokerServiceExporter();
        exporter.setModuleClassLoader(classLoader != null ? classLoader : SecureHttpInvokerServiceExporterTest.class.getClassLoader());
        exporter.setService(service);
        exporter.setServiceInterface(serviceClass);
        exporter.setBypassDeserializationClassFilter(bypassDeserializationClassFilter);
        exporter.setSecurityCallback(Mockito.mock(SecureHttpInvokerServiceExporter.SecurityCallback.class));
        exporter.prepare();
        return exporter;
    }
}