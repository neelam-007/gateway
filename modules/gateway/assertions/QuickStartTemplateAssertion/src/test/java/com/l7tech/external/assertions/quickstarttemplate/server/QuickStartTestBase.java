package com.l7tech.external.assertions.quickstarttemplate.server;

import com.google.common.collect.ImmutableMap;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartDocumentationAssertion;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.EncapsulatedAssertion;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuickStartTestBase {

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

}
