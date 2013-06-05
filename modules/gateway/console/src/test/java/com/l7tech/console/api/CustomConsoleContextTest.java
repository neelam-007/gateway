package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if (!CustomConsoleContext.isSupportedDataType(declaredMethod.getReturnType())) {
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
        public List<String> failReturnAndArgsWithUnsupportedGenericsAndTypes(List<String> listStringArg);
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
        assertNull(customExtensionInterfaceFinder.getExtensionInterface(this.getClass()));
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
}
