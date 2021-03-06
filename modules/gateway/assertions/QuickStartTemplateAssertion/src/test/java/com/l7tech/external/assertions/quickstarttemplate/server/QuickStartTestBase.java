package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.external.assertions.quickstarttemplate.QuickStartDocumentationAssertion;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import org.junit.Ignore;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public abstract class QuickStartTestBase {

    protected static EncapsulatedAssertion mockEncapsulatedAssertion(final String name, final String description,
                                                           final String schema, final String sample) {
        final EncapsulatedAssertion ea = mock(EncapsulatedAssertion.class);
        final EncapsulatedAssertionConfig eac = mock(EncapsulatedAssertionConfig.class);
        when(ea.config()).thenReturn(eac);
        when(eac.getName()).thenReturn(name);
        final Map<String, String> properties = new HashMap<>();
        if (description != null) {
            properties.put(QuickStartDocumentationAssertion.QS_DESCRIPTION_PROPERTY, description);
        }
        if (schema != null) {
            properties.put(QuickStartDocumentationAssertion.QS_SCHEMA_PROPERTY, schema);
        }
        if (sample != null) {
            properties.put(QuickStartDocumentationAssertion.QS_SAMPLE_PROPERTY, sample);
        }
        when(eac.getProperties()).thenReturn(properties);
        when(eac.getProperty(QuickStartDocumentationAssertion.QS_DESCRIPTION_PROPERTY)).thenReturn(description);
        when(eac.getProperty(QuickStartDocumentationAssertion.QS_SCHEMA_PROPERTY)).thenReturn(schema);
        when(eac.getProperty(QuickStartDocumentationAssertion.QS_SAMPLE_PROPERTY)).thenReturn(sample);
        return ea;
    }

    protected static Assertion mockAssertion(final String name, final String externalName, final String description) {
        final Assertion assertion = Mockito.mock(Assertion.class);
        final AssertionMetadata metadata = Mockito.mock(AssertionMetadata.class);
        Mockito.doReturn(metadata).when(assertion).meta();
        Mockito.doReturn(name).when(metadata).get(AssertionMetadata.SHORT_NAME);
        Mockito.doReturn(externalName).when(metadata).get(AssertionMetadata.WSP_EXTERNAL_NAME);
        Mockito.doReturn(description).when(metadata).get(AssertionMetadata.DESCRIPTION);
        return assertion;
    }

}
