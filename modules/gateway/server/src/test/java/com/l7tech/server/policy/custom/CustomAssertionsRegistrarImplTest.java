package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomCredentialSource;
import com.l7tech.policy.assertion.ext.ServiceInvocation;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceBinding;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.admin.ExtensionInterfaceManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CustomAssertionsRegistrarImplTest {
    @Mock
    private ServerAssertionRegistry serverAssertionRegistryMock;

    @Mock
    private SecurePasswordManager securePasswordManagerMock;

    @Mock
    private CustomKeyValueStoreManager customKeyValueStoreManagerMock;

    @Mock
    private SsgKeyStoreManager ssgKeyStoreManagerMock;

    @Mock
    private DefaultKey defaultKeyMock;

    @Before
    public void setUp() throws Exception {
        // clear all registered assertions per unit test
        Field assertionsField = CustomAssertions.class.getDeclaredField("assertions");
        assertionsField.setAccessible(true);
        Map assertions = (Map) assertionsField.get(null);
        assertions.clear();
    }

    @Test
    public void registerCustomExtensionInterface() throws Exception {
        ExtensionInterfaceManager extensionInterfaceManager = new ExtensionInterfaceManager(null, null, null);
        CustomAssertionsRegistrarImpl customAssertionsRegistrarImpl = new CustomAssertionsRegistrarImpl(serverAssertionRegistryMock);
        customAssertionsRegistrarImpl.setExtensionInterfaceManager(extensionInterfaceManager);
        customAssertionsRegistrarImpl.setSecurePasswordManager(securePasswordManagerMock);
        customAssertionsRegistrarImpl.setCustomKeyValueStoreManager(customKeyValueStoreManagerMock);
        customAssertionsRegistrarImpl.setSsgKeyStoreManager(ssgKeyStoreManagerMock);
        customAssertionsRegistrarImpl.setDefaultKey(defaultKeyMock);

        // register MyCustomExtensionInterfaceBinding which holds an implementation of MyInterface
        customAssertionsRegistrarImpl.registerCustomExtensionInterface(MyCustomExtensionInterfaceBinding.class);

        // verify registration
        assertTrue(extensionInterfaceManager.isInterfaceRegistered("com.l7tech.server.policy.custom.CustomAssertionsRegistrarImplTest$MyInterface", null));
        assertFalse(extensionInterfaceManager.isInterfaceRegistered("com.l7tech.server.policy.custom.CustomAssertionsRegistrarImplTest$DoesNotExist", null));
    }

    private interface MyInterface {
        String echo(String in);
    }

    @SuppressWarnings("unused")
    private static class MyCustomExtensionInterfaceBinding extends CustomExtensionInterfaceBinding {
        public MyCustomExtensionInterfaceBinding() {
            super(MyInterface.class, new MyInterface() {
                @Override
                public String echo(String in) {
                    return "Echo: " + in;
                }
            });
        }
    }

    @Ignore("Ignoring deprecated function")
    @Test
    public void parseModuleFileName() throws Exception {
        ExtensionInterfaceManager extensionInterfaceManager = mock(ExtensionInterfaceManager.class);
        CustomAssertionsRegistrarImpl customAssertionsRegistrarImpl = new CustomAssertionsRegistrarImpl(serverAssertionRegistryMock);
        customAssertionsRegistrarImpl.setExtensionInterfaceManager(extensionInterfaceManager);
        customAssertionsRegistrarImpl.setSecurePasswordManager(securePasswordManagerMock);
        customAssertionsRegistrarImpl.setCustomKeyValueStoreManager(customKeyValueStoreManagerMock);
        customAssertionsRegistrarImpl.setSsgKeyStoreManager(ssgKeyStoreManagerMock);
        customAssertionsRegistrarImpl.setDefaultKey(defaultKeyMock);

        String moduleFileName = "salesforce_poc.jar";
        String configFileName = "custom_assertions.properties";
        String configFileUrlPath = "file:/C:/trunk/build/deploy/Gateway/node/default/../../runtime/modules/lib/" + moduleFileName + "!/" + configFileName;

        assertEquals(moduleFileName, customAssertionsRegistrarImpl.parseModuleFileName(configFileUrlPath, configFileName));
    }

    /**
     * A test non credential source assertion
     */
    @SuppressWarnings("serial")
    public static class TestNonCredentialSourceAssertion implements CustomAssertion {
        @Override
        public String getName() {
            return "Test NonCredentialSource CustomAssertion";
        }
    }

    /**
     * A test credential source assertion returning false
     */
    @SuppressWarnings("serial")
    public static class TestCredentialSourceAssertionReturnFalse implements CustomAssertion, CustomCredentialSource {
        @Override
        public String getName() {
            return "Test CredentialSource CustomAssertion returning false";
        }

        @Override
        public boolean isCredentialSource() {
            return false;
        }
    }

    /**
     * A test credential source assertion returning true
     */
    @SuppressWarnings("serial")
    public static class TestCredentialSourceAssertionReturnTrue implements CustomAssertion, CustomCredentialSource {
        @Override
        public String getName() {
            return "Test CredentialSource CustomAssertion returning true";
        }

        @Override
        public boolean isCredentialSource() {
            return true;
        }
    }

    /**
     * Our empty test ServiceInvocation class.
     */
    class TestServiceInvocation extends ServiceInvocation {
    }

    /**
     * Test hasCustomCredentialSource with registered assertion implementing
     * {@link com.l7tech.policy.assertion.ext.CustomCredentialSource#isCredentialSource()} method by returning <code>false</code>,
     * assertion implementing {@link com.l7tech.policy.assertion.ext.CustomCredentialSource#isCredentialSource()} method
     * by returning <code>true</code>, and assertion placed in {@link Category#MESSAGE MESSAGE} category.
     * <p>
     * There are two ways to set certain CustomAssertion as credential source:
     * <ol>
     *     <li>The legacy way, by placing the assertion into {@link com.l7tech.policy.assertion.ext.Category#ACCESS_CONTROL ACCESS_CONTROL}</li>
     *     <li>or by implementing {@link com.l7tech.policy.assertion.ext.CustomCredentialSource#isCredentialSource()} method and
     *     returning <code>true</code> </li>
     * </ol>
     */
    @Test
    public void hasCustomCredentialSource_OnlyCredentialSourceAssertion() throws Exception {
        CustomAssertionsRegistrarImpl customAssertionsRegistrarImpl =
            new CustomAssertionsRegistrarImpl(serverAssertionRegistryMock);
        customAssertionsRegistrarImpl.setExtensionInterfaceManager(mock(ExtensionInterfaceManager.class));
        customAssertionsRegistrarImpl.setSecurePasswordManager(securePasswordManagerMock);
        customAssertionsRegistrarImpl.setCustomKeyValueStoreManager(customKeyValueStoreManagerMock);
        customAssertionsRegistrarImpl.setSsgKeyStoreManager(ssgKeyStoreManagerMock);
        customAssertionsRegistrarImpl.setDefaultKey(defaultKeyMock);

        assertTrue("there are no assertions registered on startup", customAssertionsRegistrarImpl.getAssertions().isEmpty());

        //noinspection serial
        final CustomAssertionDescriptor descriptorNonCredentialSourceInterfaceAndNonAccessControlCategory = new CustomAssertionDescriptor (
                "Test.TestNonCredentialSourceAssertion",
                TestNonCredentialSourceAssertion.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.MESSAGE); // don't place it in ACCESS_CONTROL
                }}
        );
        CustomAssertions.register(descriptorNonCredentialSourceInterfaceAndNonAccessControlCategory);

        assertFalse("there are no credential source assertions", customAssertionsRegistrarImpl.hasCustomCredentialSource());

        //noinspection serial
        final CustomAssertionDescriptor descriptorCredentialSourceFalseInterface = new CustomAssertionDescriptor (
                "Test.TestCredentialSourceAssertionReturnFalse",
                TestCredentialSourceAssertionReturnFalse.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.CUSTOM_ASSERTIONS);
                    add(Category.MESSAGE); // don't place it in ACCESS_CONTROL
                }}
        );
        CustomAssertions.register(descriptorCredentialSourceFalseInterface);

        assertFalse("there are no credential source assertions", customAssertionsRegistrarImpl.hasCustomCredentialSource());

        //noinspection serial
        final CustomAssertionDescriptor descriptorCredentialSourceTrueInterface = new CustomAssertionDescriptor (
                "Test.TestCredentialSourceAssertionReturnTrue",
                TestCredentialSourceAssertionReturnTrue.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.CUSTOM_ASSERTIONS);
                    add(Category.MESSAGE); // don't place it in ACCESS_CONTROL
                }}
        );
        CustomAssertions.register(descriptorCredentialSourceTrueInterface);

        assertTrue("there is one credential source assertion", customAssertionsRegistrarImpl.hasCustomCredentialSource());
    }

    /**
     * Test hasCustomCredentialSource with two assertions <i>not</i> implementing {@link CustomCredentialSource},
     * one placed in {@link Category#MESSAGE MESSAGE} category and
     * the other in {@link Category#ACCESS_CONTROL ACCESS_CONTROL}.
     * <p>
     * There are two ways to set certain CustomAssertion as credential source:
     * <ol>
     *     <li>The legacy way, by placing the assertion into {@link com.l7tech.policy.assertion.ext.Category#ACCESS_CONTROL ACCESS_CONTROL}</li>
     *     <li>or by implementing {@link com.l7tech.policy.assertion.ext.CustomCredentialSource#isCredentialSource()} method and
     *     returning <code>true</code> </li>
     * </ol>
     */
    @Test
    public void hasCustomCredentialSource_OnlyAccessControlCategory() throws Exception {
        CustomAssertionsRegistrarImpl customAssertionsRegistrarImpl =
                new CustomAssertionsRegistrarImpl(serverAssertionRegistryMock);
        customAssertionsRegistrarImpl.setExtensionInterfaceManager(mock(ExtensionInterfaceManager.class));
        customAssertionsRegistrarImpl.setSecurePasswordManager(securePasswordManagerMock);
        customAssertionsRegistrarImpl.setCustomKeyValueStoreManager(customKeyValueStoreManagerMock);
        customAssertionsRegistrarImpl.setSsgKeyStoreManager(ssgKeyStoreManagerMock);
        customAssertionsRegistrarImpl.setDefaultKey(defaultKeyMock);

        assertTrue("there are no assertions registered on startup", customAssertionsRegistrarImpl.getAssertions().isEmpty());

        //noinspection serial
        final CustomAssertionDescriptor descriptorNonAccessControlCategory = new CustomAssertionDescriptor (
                "Test.TestNonCredentialSourceAssertion",
                TestNonCredentialSourceAssertion.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.MESSAGE); // don't place it in ACCESS_CONTROL
                    add(Category.CUSTOM_ASSERTIONS);
                }}
        );
        CustomAssertions.register(descriptorNonAccessControlCategory);

        assertFalse("there are no credential source assertions", customAssertionsRegistrarImpl.hasCustomCredentialSource());

        //noinspection serial
        final CustomAssertionDescriptor descriptorAccessControlCategory = new CustomAssertionDescriptor (
                "Test.TestNonCredentialSourceAssertion",
                TestNonCredentialSourceAssertion.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.CUSTOM_ASSERTIONS);
                    add(Category.ACCESS_CONTROL); // place it in ACCESS_CONTROL
                }}
        );
        CustomAssertions.register(descriptorAccessControlCategory);

        assertTrue("there is one credential source assertion", customAssertionsRegistrarImpl.hasCustomCredentialSource());
    }
}
