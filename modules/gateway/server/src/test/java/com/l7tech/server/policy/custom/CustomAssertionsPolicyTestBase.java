package com.l7tech.server.policy.custom;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.TestCustomMessageTargetable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.ServiceInvocation;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerCustomAssertionHolder;
import com.l7tech.server.util.Injector;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * A base class which will setup all you need for calling checkRequest through the policy factory.
 */
public class CustomAssertionsPolicyTestBase {

    /**
     * Our ServiceInvocation implementation for testing
     */
    protected class TestServiceInvocation extends ServiceInvocation {
    }

    /**
     * Test Legacy CustomAssertion
     */
    public static class TestLegacyCustomAssertion implements CustomAssertion {
        private static final long serialVersionUID = 7349491450019520261L;
        @Override
        public String getName() {
            return "My Legacy CustomAssertion";
        }
    }

    @Mock(name = "applicationContext")
    protected ApplicationContext mockApplicationContext;

    @InjectMocks
    protected final ServerPolicyFactory serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new Injector() {
        @Override
        public void inject( final Object target ) {
            // Since we already have mocked ServiceInvocation object (serviceInvocation)
            // we need to inject that instance into newly created ServerCustomAssertionHolder.
            // for our test purposes
            //
            // !!!WARNING!!!
            // In the future ServerCustomAssertionHolder.serviceInvocation field should not be renamed
            // otherwise this unit test will fail
            if (target instanceof ServerCustomAssertionHolder) {
                try {
                    Field field = target.getClass().getDeclaredField("serviceInvocation");
                    field.setAccessible(true);
                    field.set(target, serviceInvocation);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    fail("Failed to inject ServerCustomAssertionHolder#serviceInvocation field.");
                }
            }
        }
    });

    @Spy
    protected ServiceInvocation serviceInvocation = new TestServiceInvocation();

    @Mock
    protected CustomAssertionsRegistrar mockRegistrar;

    protected final AssertionRegistry assertionRegistry = new AssertionRegistry();

    protected void doInit() throws Exception {
        // mock getBean to return appropriate mock classes for CustomAssertionRegistrar
        when(mockApplicationContext.getBean("customAssertionRegistrar")).thenReturn(mockRegistrar);
        
        // mock getBean to return appropriate mock classes for stashManagerFactory 
        final StashManagerFactory stashManagerFactory = new StashManagerFactory() {
            @Override
            public StashManager createStashManager() {
                return new ByteArrayStashManager();
            }
        };
        when(mockApplicationContext.getBean("stashManagerFactory")).thenReturn(stashManagerFactory);
        when(mockApplicationContext.getBean("stashManagerFactory", StashManagerFactory.class)).thenReturn(stashManagerFactory);

        // add sample Legacy descriptor
        //noinspection serial
        final CustomAssertionDescriptor descriptorLegacy = new CustomAssertionDescriptor(
                "Test.TestLegacyCustomAssertion",
                TestLegacyCustomAssertion.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.AUDIT_ALERT);
                }}
        );
        descriptorLegacy.setDescription("TestLegacyCustomAssertion Description");

        // add message targetable descriptor
        //noinspection serial
        final CustomAssertionDescriptor descriptorTargetable = new CustomAssertionDescriptor(
                "Test.TestCustomMessageTargetable",
                TestCustomMessageTargetable.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.AUDIT_ALERT);
                }}
        );
        descriptorLegacy.setDescription("TestCustomMessageTargetable Description");

        // add descriptors
        when(mockRegistrar.getDescriptor(TestLegacyCustomAssertion.class)).then(new Returns(descriptorLegacy));
        when(mockRegistrar.getDescriptor(TestCustomMessageTargetable.class)).then(new Returns(descriptorTargetable));

        // mock getBean to return appropriate mock classes for assertionRegistry
        assertionRegistry.afterPropertiesSet();
        when(mockApplicationContext.getBean("assertionRegistry", AssertionRegistry.class)).thenReturn(assertionRegistry);
        when(mockApplicationContext.getBean("assertionRegistry")).thenReturn(assertionRegistry);

        // Register the CustomAssertionHolder
        assertionRegistry.registerAssertion(CustomAssertionHolder.class);


        when(mockApplicationContext.getBean("policyFactory")).thenReturn(serverPolicyFactory);
        when(mockApplicationContext.getBean("policyFactory", ServerPolicyFactory.class)).thenReturn(serverPolicyFactory);
    }

    protected PolicyEnforcementContext makeContext(final Message request, final Message response) {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    protected Assertion makePolicy(final List<Assertion> assertions) {
        final AllAssertion allAssertion = new AllAssertion();
        for (Assertion assertion: assertions) {
            allAssertion.addChild(assertion);
        }
        return allAssertion;
    }
}
