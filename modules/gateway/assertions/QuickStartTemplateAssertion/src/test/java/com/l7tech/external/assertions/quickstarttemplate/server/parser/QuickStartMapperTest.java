package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPolicyBuilderException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QuickStartMapperTest {

    @Mock
    private QuickStartEncapsulatedAssertionLocator locator;

    @InjectMocks
    private QuickStartMapper fixture;

    @Test
    public void getEncapsulatedAssertionsShouldReturnEmptyList() throws Exception {
        final Service service = mockService();
        final List<EncapsulatedAssertion> assertions = fixture.getEncapsulatedAssertions(service);
        assertThat(assertions.size(), equalTo(0));
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetStringParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", "someValue"))
        );
        final EncapsulatedAssertion ea1 = mockAssertion(ImmutableMap.of("someKey", DataType.STRING));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<EncapsulatedAssertion> assertions = fixture.getEncapsulatedAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "someValue");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetBooleanParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", true))
        );
        final EncapsulatedAssertion ea1 = mockAssertion(ImmutableMap.of("someKey", DataType.BOOLEAN));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<EncapsulatedAssertion> assertions = fixture.getEncapsulatedAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "true");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetIntegerParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", 42))
        );
        final EncapsulatedAssertion ea1 = mockAssertion(ImmutableMap.of("someKey", DataType.INTEGER));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<EncapsulatedAssertion> assertions = fixture.getEncapsulatedAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "42");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetMultiTypeListParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", Lists.newArrayList("hi", 13, true)))
        );
        final EncapsulatedAssertion ea1 = mockAssertion(ImmutableMap.of("someKey", DataType.STRING));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<EncapsulatedAssertion> assertions = fixture.getEncapsulatedAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1).putParameter("someKey", "hi;13;true");
    }

    @Test
    public void getEncapsulatedAssertionsShouldSetNoParameter() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of())
        );
        final EncapsulatedAssertion ea1 = mockAssertion(ImmutableMap.of());
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<EncapsulatedAssertion> assertions = fixture.getEncapsulatedAssertions(service);
        assertThat(assertions, contains(ea1));
        verify(ea1, never()).putParameter(anyString(), anyString());
    }

    @Test(expected = QuickStartPolicyBuilderException.class)
    public void getEncapsulatedAssertionsShouldFailWhenParameterNotFound() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", Lists.newArrayList("somParameter")))
        );
        final EncapsulatedAssertion ea1 = mockAssertion(ImmutableMap.of());
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        fixture.getEncapsulatedAssertions(service);
    }

    @Test
    public void getEncapsulatedAssertionsShouldIgnoreUnsetParameters() throws Exception {
        final Service service = mockService(
                mockPolicy("SomeAssertion", ImmutableMap.of("someKey", "someValue"))
        );
        final EncapsulatedAssertion ea1 = mockAssertion(ImmutableMap.of(
                "someKey", DataType.STRING,
                "someOtherKey", DataType.STRING));
        when(locator.findEncapsulatedAssertion("SomeAssertion")).thenReturn(ea1);
        final List<EncapsulatedAssertion> assertions = fixture.getEncapsulatedAssertions(service);
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
        fixture.getEncapsulatedAssertions(service);
    }

    // TODO add unit tests

    private static Service mockService(final Map<String, Map<String, ?>>... policies) {
        return new Service("SomeName", "/SomePath", Collections.emptyList(), Lists.newArrayList(policies));
    }

    private static Map<String, Map<String, ?>> mockPolicy(final String name, final Map<String, ?> attributes) {
        return ImmutableMap.of(name, attributes);
    }

    private static EncapsulatedAssertion mockAssertion(final Map<String, DataType> descriptorMap) {
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