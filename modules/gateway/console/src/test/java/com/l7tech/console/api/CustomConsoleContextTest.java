package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceFinder;
import com.l7tech.policy.assertion.ext.commonui.CommonUIServices;
import com.l7tech.policy.assertion.ext.commonui.CustomSecurePasswordPanel;
import com.l7tech.policy.assertion.ext.commonui.CustomTargetVariablePanel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomConsoleContextTest {
    @Mock @SuppressWarnings("unused")
    private Registry registry;

    @Mock @SuppressWarnings("unused")
    private TrustedCertAdmin trustedCertAdmin;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void supportedCustomExtensionInterfaceDataTypes() {
        Class interfaceClass = CustomExtensionInterfaceTestMethodSignatures.class;
        for (Method declaredMethod : interfaceClass.getDeclaredMethods()) {
            String methodName = declaredMethod.getName();

            // test return type
            if (!CustomConsoleContext.isSupportedDataType(declaredMethod.getGenericReturnType())) {
                assertTrue(methodName.startsWith(CustomExtensionInterfaceTestMethodSignatures.FAIL_RETURN));
            }

            // test parameter types
            for (Class parameterType : declaredMethod.getParameterTypes()) {
                if (!CustomConsoleContext.isSupportedDataType(parameterType)) {
                    assertTrue(methodName.startsWith(CustomExtensionInterfaceTestMethodSignatures.FAIL_ARGS) || methodName.startsWith(CustomExtensionInterfaceTestMethodSignatures.FAIL_RETURN_AND_ARGS));
                }
            }
        }
    }

    private interface CustomExtensionInterfaceTestMethodSignatures {
        public final static String FAIL_ARGS = "failArgs";
        public final static String FAIL_RETURN = "failReturn";
        public final static String FAIL_RETURN_AND_ARGS = "failReturnAndArgs";

        // supported: primitives, String (arrays okay)
        @SuppressWarnings("unused")
        public long successTypes(boolean booleanArg, byte byteArg,  short shortArg, int intArg, long longArg);

        @SuppressWarnings("unused")
        public double[] successTypes(float[] floatArrayArg, double[] doubleArrayArg, char[] charArrayArg, String[] stringArrayArg);

        @SuppressWarnings("unused")
        public String[] successTypes(String stringArg, String[] stringArrayArg, boolean booleanArg);

        @SuppressWarnings("unused")
        public HashMap<String, String> successTypes(String string);

        @SuppressWarnings("unused")
        public void successTypes(String[] stringArrayArg);

        @SuppressWarnings("unused")
        public void successTypes();

        // unsupported: anything else (e.g. Assert), unit test will look for "failArgs", or "failReturn", or "failReturnAndArgs" to start the method name
        @SuppressWarnings("unused")
        public Assert failReturnAndArgsWithUnsupportedTypes(int intArg, Assert assertArg);

        @SuppressWarnings("unused")
        public Assert failReturnWithUnsupportedTypes(String stringArg, short shortArg);

        @SuppressWarnings("unused")
        public String failArgsWithUnsupportedTypes(float floatArg, Assert assertArg);

        @SuppressWarnings("unused")
        public List failReturnWithUnsupportedTypes();

        @SuppressWarnings("unused")
        public void failArgsWithUnsupportedTypes(String stringArg, Map<String, String> mapStringStringArg, short shortArg);

        @SuppressWarnings("unused")
        public void failArgsWithUnsupportedGenericsTypes(HashMap<String, String> hashMapStrings);

        @SuppressWarnings("unused")
        public List<String> failReturnAndArgsWithUnsupportedGenericsTypes(List<String> listStringArg);
    }

    @Test
    public void addCustomExtensionInterfaceFinder() throws Exception {
        // mock custom extension interface registration, done in CustomAssertionsRegistrarImpl
        MyInterface testFace = new MyInterfaceImpl();
        Registry.setDefault(registry);
        when(registry.getExtensionInterface(MyInterface.class, null)).thenReturn(testFace);

        // get the registered extension interface
        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCustomExtensionInterfaceFinder(consoleContext);
        CustomExtensionInterfaceFinder customExtensionInterfaceFinder = (CustomExtensionInterfaceFinder) consoleContext.get("customExtensionInterfaceFinder");
        assertEquals(testFace, customExtensionInterfaceFinder.getExtensionInterface(MyInterface.class));

        // not registered
        assertNull(customExtensionInterfaceFinder.getExtensionInterface(MyInterfaceNotRegistered.class));
    }

    @Test
    @Ignore("Developer Test")
    public void commonUIServicesCreateTargetVariablePanel() throws Exception {
        Registry.setDefault(registry);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get("commonUIServices");

        CustomTargetVariablePanel targetVariablePanel = commonUIServices.createTargetVariablePanel();
        assertNotNull(targetVariablePanel);
        assertNotNull(targetVariablePanel.getPanel());
    }

    @Test
    @Ignore("Developer Test")
    public void commonUIServicesCreatePasswordComboBoxPanel() throws Exception {
        Registry.setDefault(registry);
        when(registry.getTrustedCertManager()).thenReturn(trustedCertAdmin);
        List<SecurePassword> passwords = this.createSecurePasswordList();
        when(trustedCertAdmin.findAllSecurePasswords()).thenReturn(passwords);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get("commonUIServices");

        CustomSecurePasswordPanel securePasswordPanel = commonUIServices.createPasswordComboBoxPanel(new JDialog());
        assertNotNull(securePasswordPanel);
        assertNotNull(securePasswordPanel.getPanel());

        // check that only passwords are populated. Not PEM private keys.
        assertTrue(securePasswordPanel.containsItem(1000L));
        assertTrue(securePasswordPanel.containsItem(1001L));
        assertFalse(securePasswordPanel.containsItem(1002L));
        assertFalse(securePasswordPanel.containsItem(1003L));
    }

    @Test
    @Ignore("Developer Test")
    public void commonUIServicesCreatePEMPrivateKeyComboBoxPanel() throws Exception {
        Registry.setDefault(registry);
        when(registry.getTrustedCertManager()).thenReturn(trustedCertAdmin);
        List<SecurePassword> passwords = this.createSecurePasswordList();
        when(trustedCertAdmin.findAllSecurePasswords()).thenReturn(passwords);

        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get("commonUIServices");

        CustomSecurePasswordPanel securePasswordPanel = commonUIServices.createPEMPrivateKeyComboBoxPanel(new JDialog());
        assertNotNull(securePasswordPanel);
        assertNotNull(securePasswordPanel.getPanel());

        // check that only PEM private keys are populated. Not passwords.
        assertFalse(securePasswordPanel.containsItem(1000L));
        assertFalse(securePasswordPanel.containsItem(1001L));
        assertTrue(securePasswordPanel.containsItem(1002L));
        assertTrue(securePasswordPanel.containsItem(1003L));
    }

    private interface MyInterface {
        String echo(String in);
    }

    private class MyInterfaceImpl implements MyInterface {
        @Override
        public String echo(String in) {
            return "Echo: " + in;
        }
    }

    private interface MyInterfaceNotRegistered {
        String echo(String in);
    }

    private List<SecurePassword> createSecurePasswordList() {
        // Add 2 passwords and 2 PEM private keys.
        List<SecurePassword> passwords = new ArrayList<>(4);

        SecurePassword password = new SecurePassword();
        password.setOid(1000L);
        password.setName("pass1");
        password.setType(SecurePassword.SecurePasswordType.PASSWORD);
        password.setEncodedPassword("");
        passwords.add(password);

        password = new SecurePassword();
        password.setOid(1001L);
        password.setName("pass2");
        password.setType(SecurePassword.SecurePasswordType.PASSWORD);
        password.setEncodedPassword("");
        passwords.add(password);

        password = new SecurePassword();
        password.setOid(1002L);
        password.setName("pem1");
        password.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
        password.setEncodedPassword("");
        passwords.add(password);

        password = new SecurePassword();
        password.setOid(1003L);
        password.setName("pem2");
        password.setType(SecurePassword.SecurePasswordType.PEM_PRIVATE_KEY);
        password.setEncodedPassword("");
        passwords.add(password);

        return passwords;
    }
}
