package com.l7tech.console.api;

import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class CustomConsoleContextTest {

    @Test
    public void supportedCustomExtensionInterfaceDataTypes() {
        Class interfaceClass = CustomExtensionInterfaceTestMethodSignatures.class;
        for (Method declaredMethod : interfaceClass.getDeclaredMethods()) {
            String methodName = declaredMethod.getName();

            // test return type
            if (!CustomConsoleContext.isSupportedDataType(declaredMethod.getReturnType())) {
                Assert.assertTrue(methodName.startsWith(CustomExtensionInterfaceTestMethodSignatures.FAIL_RETURN));
            }

            // test parameter types
            for (Class parameterType : declaredMethod.getParameterTypes()) {
                if (!CustomConsoleContext.isSupportedDataType(parameterType)) {
                    Assert.assertTrue(methodName.startsWith(CustomExtensionInterfaceTestMethodSignatures.FAIL_ARGS) || methodName.startsWith(CustomExtensionInterfaceTestMethodSignatures.FAIL_RETURN_AND_ARGS));
                }
            }
        }
    }
}
