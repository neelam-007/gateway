package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.collect.Sets;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartDocumentationAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartAssertionLocator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerQuickStartDocumentationAssertionTest extends QuickStartTestBase {

    @Mock
    private QuickStartAssertionLocator assertionLocator;
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
        final ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).setVariable(keyCaptor.capture(), valueCaptor.capture());
        assertThat(keyCaptor.getValue(), equalTo(QuickStartDocumentationAssertion.QS_DOC));
        final String doc = valueCaptor.getValue();
        // Just a basic sanity check to make sure it's got something in it. More detailed testing is in the
        // doc generator class.
        assertThat(doc, containsString("MyAssertion"));
        assertThat(doc, containsString("MyDescription"));
        assertThat(doc, containsString("MySchema"));
        assertThat(doc, containsString("MySample"));
    }

    @Test(expected = PolicyAssertionException.class)
    public void checkRequestFindExceptionResultsInPolicyAssertionException() throws FindException, IOException, PolicyAssertionException {
        when(assertionLocator.findEncapsulatedAssertions()).thenThrow(new FindException());
        fixture.checkRequest(context);
    }
}