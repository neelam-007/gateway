package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.VariableServices;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceFinder;
import com.l7tech.policy.assertion.ext.commonui.CommonUIServices;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.*;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomConsoleContextTest {
    @Mock @SuppressWarnings("unused")
    private Registry registry;

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

    @Test
    public void addCustomExtensionInterfaceFinder() throws Exception {
        // mock custom extension interface registration, done in CustomAssertionsRegistrarImpl
        MyInterface testFace = new MyInterfaceImpl();
        Registry.setDefault(registry);
        when(registry.getExtensionInterface(MyInterface.class, null)).thenReturn(testFace);

        // get the registered extension interface
        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCustomExtensionInterfaceFinder(consoleContext);
        CustomExtensionInterfaceFinder customExtensionInterfaceFinder = (CustomExtensionInterfaceFinder) consoleContext.get(CustomExtensionInterfaceFinder.CONSOLE_CONTEXT_KEY);
        assertEquals(testFace, customExtensionInterfaceFinder.getExtensionInterface(MyInterface.class));

        // not registered
        assertNull(customExtensionInterfaceFinder.getExtensionInterface(MyInterfaceNotRegistered.class));
    }

    @Test
    public void addCommonUIServices() throws Exception {
        // Test only for existence of CommonUIServices in the console context.
        // The actual testing of CommonUIServices is implemented in
        // com.l7tech.console.api.CommonUIServicesTest.
        //
        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addCommonUIServices(consoleContext, new CustomAssertionHolder(), null);
        CommonUIServices commonUIServices = (CommonUIServices) consoleContext.get(CommonUIServices.CONSOLE_CONTEXT_KEY);
        assertNotNull(commonUIServices);
    }

    @Test
    public void addKeyValueStoreServices() throws Exception {
        // Test only for existence of keyValueStoreServices in the console context.
        // The actual testing of keyValueStoreServices is implemented in
        // com.l7tech.console.api.CustomKeyValueStoreTest.
        //
        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addKeyValueStoreServices(consoleContext);

        KeyValueStoreServices keyValueStoreServices = (KeyValueStoreServices) consoleContext.get(KeyValueStoreServices.CONSOLE_CONTEXT_KEY);
        assertNotNull(keyValueStoreServices);
    }

    @Test
    public void addVariableServices() throws Exception {
        // Test only for existence of VariableServices in the console context.
        // The actual testing of VariableServices is implemented in
        // com.l7tech.console.api.VariableServicesTest.
        //
        Map<String, Object> consoleContext = new HashMap<>(1);
        CustomConsoleContext.addVariableServices(consoleContext, new CustomAssertionHolder(), null);

        VariableServices variableServices = (VariableServices) consoleContext.get(VariableServices.CONSOLE_CONTEXT_KEY);
        assertNotNull(variableServices);
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
}