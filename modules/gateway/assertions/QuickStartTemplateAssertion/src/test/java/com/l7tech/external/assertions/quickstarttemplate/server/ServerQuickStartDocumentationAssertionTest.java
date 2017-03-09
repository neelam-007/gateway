package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.collect.Sets;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartDocumentationAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerQuickStartDocumentationAssertionTest {

    @Mock
    private QuickStartEncapsulatedAssertionLocator assertionLocator;
    @Mock
    private PolicyEnforcementContext context;

    private ServerQuickStartDocumentationAssertion fixture;

    @Before
    public void setUp() throws PolicyAssertionException {
        fixture = new ServerQuickStartDocumentationAssertion(new QuickStartDocumentationAssertion(), mock(ApplicationContext.class));
        fixture.setAssertionLocator(assertionLocator);
    }

    @Test
    public void checkRequestGeneratesDocumentation() throws Exception {
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion("MyAssertion", "MyDescription", "MySchema", "MySample");
        when(assertionLocator.findEncapsulatedAssertions()).thenReturn(Sets.newHashSet(
                ea1
        ));
        fixture.checkRequest(context);
        verify(context).setVariable(QuickStartDocumentationAssertion.QS_DOC, "<p>MyAssertion MyDescription</p>");
    }

    @Test(expected = PolicyAssertionException.class)
    public void checkRequestFindExceptionResultsInPolicyAssertionException() throws FindException, IOException, PolicyAssertionException {
        when(assertionLocator.findEncapsulatedAssertions()).thenThrow(new FindException());
        fixture.checkRequest(context);
    }

    @Test
    public void findEncapsulatedAssertionsOrderedByNameShouldReturnListOrderedByName() throws FindException {
        final EncapsulatedAssertion ea1 = mockEncapsulatedAssertion("MyAssertionA", "MyDescription", "MySchema", "MySample");
        final EncapsulatedAssertion ea2 = mockEncapsulatedAssertion("MyAssertionB", "MyDescription", "MySchema", "MySample");
        final EncapsulatedAssertion ea3 = mockEncapsulatedAssertion("MyAssertionC", "MyDescription", "MySchema", "MySample");
        when(assertionLocator.findEncapsulatedAssertions()).thenReturn(Sets.newHashSet(
                ea2,
                ea3,
                ea1
        ));
        assertThat(fixture.findEncapsulatedAssertionsOrderedByName().stream()
                .map(e -> e.config().getName())
                .collect(Collectors.toList()), contains("MyAssertionA", "MyAssertionB", "MyAssertionC"));
    }

    private static EncapsulatedAssertion mockEncapsulatedAssertion(final String name, final String description,
                                                                   final String schema, final String sample) {
        final EncapsulatedAssertion ea = mock(EncapsulatedAssertion.class);
        final EncapsulatedAssertionConfig eac = mock(EncapsulatedAssertionConfig.class);
        when(ea.config()).thenReturn(eac);
        when(eac.getName()).thenReturn(name);
        when(eac.getProperty(QuickStartDocumentationAssertion.QS_DESCRIPTION_PROPERTY)).thenReturn(description);
        when(eac.getProperty(QuickStartDocumentationAssertion.QS_SCHEMA_PROPERTY)).thenReturn(schema);
        when(eac.getProperty(QuickStartDocumentationAssertion.QS_SAMPLE_PROPERTY)).thenReturn(sample);
        return ea;
    }

}