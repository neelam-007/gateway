package com.l7tech.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HeaderBasedEntityFinder;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UsesEntitiesAtDesignTimeAssertionTranslatorTest {
    private static final String GUID = "abc123";
    private UsesEntitiesAtDesignTimeAssertionTranslator translator;
    private EncapsulatedAssertionConfig config;
    private EncapsulatedAssertion assertion;
    @Mock
    private HeaderBasedEntityFinder entityFinder;

    @Before
    public void setup() throws Exception {
        translator = new UsesEntitiesAtDesignTimeAssertionTranslator(entityFinder);
        assertion = new EncapsulatedAssertion();
        assertion.setEncapsulatedAssertionConfigGuid(GUID);
        config = new EncapsulatedAssertionConfig();
    }

    @Test
    public void translate() throws Exception {
        assertNull(assertion.config());
        when(entityFinder.find(any(EntityHeader.class))).thenReturn(config);

        final EncapsulatedAssertion translated = (EncapsulatedAssertion) translator.translate(assertion);
        assertEquals(config, translated.config());
        assertSame(assertion, translated);
        verify(entityFinder).find(any(EntityHeader.class));
    }

    @Test
    public void translateNotUsesEntitiesAtDesignTime() throws Exception {
        final Include notUsesEntitiesAtDesignTime = new Include();
        final Assertion translated = translator.translate(notUsesEntitiesAtDesignTime);
        assertSame(notUsesEntitiesAtDesignTime, translated);
        verify(entityFinder, never()).find(any(EntityHeader.class));
    }

    @BugId("SSM-4278")
    @Test
    public void translateFindException() throws Exception {
        when(entityFinder.find(any(EntityHeader.class))).thenThrow(new FindException("mocking exception"));
        final EncapsulatedAssertion translated = (EncapsulatedAssertion) translator.translate(assertion);
        assertNull(translated.config());
    }
}
