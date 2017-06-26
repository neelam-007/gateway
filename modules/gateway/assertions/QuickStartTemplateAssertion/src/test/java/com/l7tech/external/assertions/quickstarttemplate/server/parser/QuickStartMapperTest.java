package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPolicyBuilderException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionType;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.cluster.ClusterPropertyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QuickStartMapperTest {

    @Mock
    private QuickStartEncapsulatedAssertionLocator locator;

    @Mock
    protected ClusterPropertyManager clusterPropertyManager;

    @InjectMocks
    private QuickStartMapper fixture;

    @Test
    public void callAssertionSetter() throws Exception {
        final AtomicBoolean wasAttributeSet = new AtomicBoolean(false);
        final Assertion assertion = new Assertion() {
            @SuppressWarnings("UnusedDeclaration")
            public void setTypeAttribute(String t) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setTypeAttribute(Date[] t) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setTypeAttribute(List<Date> t) {
                wasAttributeSet.set(true);
            }
        };

        // exact type match
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("TypeAttribute", "aString"));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // exact type array match
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("TypeAttribute", new Date[] {new Date(), new Date(), new Date()}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // can't convert, throw exception
        try {
            fixture.callAssertionSetter(null, assertion, ImmutableMap.of("NotThere", Currency.getInstance(Locale.CANADA)));
            fail("Expected callAssertionSetter(...) to fail, but has pass instead.");
        } catch (final QuickStartPolicyBuilderException e) {
            // expected
        }
    }

    @Test
    public void callAssertionSetterCodeInjectionProtectionAssertion() throws Exception {
        final AtomicBoolean wasAttributeSet = new AtomicBoolean(false);
        final Assertion assertion = new Assertion() {
            @SuppressWarnings("UnusedDeclaration")
            public void setProtections(CodeInjectionProtectionType[] c) {
                wasAttributeSet.set(true);
            }
        };

        List<String> codeInjectionProtectionTypes = Arrays.asList("htmlJavaScriptInjection", "phpEvalInjection");
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("Protections", codeInjectionProtectionTypes));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
    }

    @Test
    public void callAssertionSetterSslAssertionOption() throws Exception {
        final AtomicBoolean wasAttributeSet = new AtomicBoolean(false);
        final Assertion assertion = new Assertion() {
            @SuppressWarnings("UnusedDeclaration")
            public void setOption(SslAssertion.Option c) {
                wasAttributeSet.set(true);
            }
        };

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("Option", "Optional"));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
    }

    @Test
    public void callAssertionSetterWithPrimitive() throws Exception {
        final AtomicBoolean wasAttributeSet = new AtomicBoolean(false);
        final Assertion assertion = new Assertion() {
            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(int i) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Double d) {
                wasAttributeSet.set(true);
            }
        };

        // primitive
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", 1));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // autobox primitive wrapper to primitive
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", Integer.valueOf("1")));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // primitive wrapper
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", Double.valueOf("1.0")));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // autobox primitive to primitive wrapper - in case map autobox changes
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", 1.0));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
    }

    @Test
    public void callAssertionSetterWithPrimitiveArray() throws Exception {
        final AtomicBoolean wasAttributeSet = new AtomicBoolean(false);
        final Assertion assertion = new Assertion() {
            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(boolean[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(byte[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(char[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(short[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(int[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(long[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(double[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveAttribute(float[] a) {
                wasAttributeSet.set(true);
            }
        };

        // primitive array
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new boolean[] {true, false, true}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // autobox primitive wrapper array to primitive array
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Boolean[] {true, false, true}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Byte[] {1, 2, 3}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Character[] {'a', 'b', 'c'}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Short[] {1, 2, 3}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Integer[] {1, 2, 3}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Long[] {1L, 2L, 3L}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Double[] {1.0, 2.0, 3.0}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveAttribute", new Float[] {1.0F, 2.0F, 3.0F}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
    }

    @Test
    public void callAssertionSetterWithPrimitiveWrapperArray() throws Exception {
        final AtomicBoolean wasAttributeSet = new AtomicBoolean(false);
        final Assertion assertion = new Assertion() {
            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Boolean[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Byte[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Character[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Short[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Integer[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Long[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Double[] a) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setPrimitiveWrapperAttribute(Float[] a) {
                wasAttributeSet.set(true);
            }
        };

        // primitive wrapper array
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new Long[] {1L, 2L, 3L}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // autobox primitive array to primitive wrapper array
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new boolean[] {true, true, false}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new byte[] {1, 2, 3}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new char[] {'a', 'b', 'c'}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new short[] {1, 2, 3}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new int[] {1, 2, 3}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new long[] {1L, 2L, 3L}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new double[] {1.0, 2.0, 3.0}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("PrimitiveWrapperAttribute", new float[] {1.0F, 2.0F, 3.0F}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
    }

    @Test
    public void callAssertionSetterValueOf() throws Exception {
        final AtomicBoolean wasAttributeSet = new AtomicBoolean(false);
        final Assertion assertion = new Assertion() {
            @SuppressWarnings("UnusedDeclaration")
            public void setValueOfAttribute(HttpMethod h) {
                wasAttributeSet.set(true);
            }

            @SuppressWarnings("UnusedDeclaration")
            public void setValueOfAttribute(HttpMethod[] h) {
                wasAttributeSet.set(true);
            }
        };

        // value of
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("ValueOfAttribute", HttpMethod.GET));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);

        // value of array
        fixture.callAssertionSetter(null, assertion, ImmutableMap.of("ValueOfAttribute", new HttpMethod[] {HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}));
        assertTrue(wasAttributeSet.get());
        wasAttributeSet.set(false);
    }

    @Test
    public void getAssertionsShouldReturnEmptyList() throws Exception {
        final Service service = mockService();
        final List<Assertion> assertions = fixture.getAssertions(service);
        assertThat(assertions.size(), equalTo(0));
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetStringParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", "someValue"))
        );
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion(ImmutableMap.of("someKey", DataType.STRING));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<Assertion> assertions = fixture.getAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "someValue");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetBooleanParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", true))
        );
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion(ImmutableMap.of("someKey", DataType.BOOLEAN));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<Assertion> assertions = fixture.getAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "true");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetIntegerParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", 42))
        );
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion(ImmutableMap.of("someKey", DataType.INTEGER));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<Assertion> assertions = fixture.getAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "42");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetMultiTypeListParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", Lists.newArrayList("hi", 13, true)))
        );
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion(ImmutableMap.of("someKey", DataType.STRING));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<Assertion> assertions = fixture.getAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "hi;13;true");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetNoParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of())
        );
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion(ImmutableMap.of());
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<Assertion> assertions = fixture.getAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1, never()).putParameter(anyString(), anyString());
    }

    @Test(expected = QuickStartPolicyBuilderException.class)
    public void getEncapsulatedAssertionsShouldFailWhenParameterNotFound() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", Lists.newArrayList("somParameter")))
        );
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion(ImmutableMap.of());
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        fixture.getAssertions(service);
    }

    @Test
    public void getEncapsulatedAssertionsShouldIgnoreUnsetParameters() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", "someValue"))
        );
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion(ImmutableMap.of(
                "someKey", DataType.STRING,
                "someOtherKey", DataType.STRING));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<Assertion> assertions = fixture.getAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "someValue");
        verify(ea1, never()).putParameter(eq("someOtherKey"), anyString());
    }

    @Test(expected = QuickStartPolicyBuilderException.class)
    public void getEncapsulatedAssertionsShouldFailIfAssertionNotFound() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", "someValue"))
        );
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(null);
        fixture.getAssertions(service);
    }

    private static Service mockService(final Map<String, Map<String, ?>>... policies) {
        return new Service("SomeName", "/SomePath", Collections.emptyList(), Lists.newArrayList(policies));
    }

    private static Map<String, Map<String, ?>> mockPolicy(final String name, final Map<String, ?> attributes) {
        return ImmutableMap.of(name, attributes);
    }

    private static EncapsulatedAssertion mockEncapsulatedAssertion(final Map<String, DataType> descriptorMap) {
        final Set<EncapsulatedAssertionArgumentDescriptor> descriptors =
                descriptorMap.entrySet().stream().map(e -> mockDescriptor(e.getKey(), e.getValue())).collect(Collectors.toSet());
        final EncapsulatedAssertionConfig config = mock(EncapsulatedAssertionConfig.class);
        when(config.getArgumentDescriptors()).thenReturn(descriptors);
        final EncapsulatedAssertion assertion = mock(EncapsulatedAssertion.class);
        when(assertion.config()).thenReturn(config);
        return assertion;
    }

    private static EncapsulatedAssertionArgumentDescriptor mockDescriptor(final String name, final DataType type) {
        final EncapsulatedAssertionArgumentDescriptor descriptor = mock(EncapsulatedAssertionArgumentDescriptor.class);
        when(descriptor.getArgumentName()).thenReturn(name);
        when(descriptor.dataType()).thenReturn(type);
        return descriptor;
    }
}