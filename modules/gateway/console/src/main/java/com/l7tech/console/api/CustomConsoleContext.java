package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.ServiceException;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceFinder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class CustomConsoleContext {

    public static void addCustomExtensionInterfaceFinder(Map<String, Object> consoleContext) {
        consoleContext.put("customExtensionInterfaceFinder", new CustomExtensionInterfaceFinder()  {
            @Override
            public <T> T getExtensionInterface(Class<T> interfaceClass) throws ServiceException {
                for (Method declaredMethod : interfaceClass.getDeclaredMethods()) {
                    if (!isSupportedDataType(declaredMethod.getGenericReturnType())) {
                        throw new ServiceException("Unsupported Custom Extension Interface return type '" +
                            declaredMethod.getReturnType() + "' for '" +
                            interfaceClass.getName() + "' '" + declaredMethod.getName() + "'.");
                    }

                    for (Class parameterType : declaredMethod.getParameterTypes()) {
                        if (!isSupportedDataType(parameterType)) {
                            throw new ServiceException("Unsupported Custom Extension Interface parameter type '" +
                                parameterType + "' for '" + interfaceClass.getName() + "' '" + declaredMethod.getName() + "'.");
                        }
                    }
                }

                return Registry.getDefault().getExtensionInterface(interfaceClass, null);
            }
        });
    }

    public static void addCommonUIServices(Map<String, Object> consoleContext, CustomAssertionHolder customAssertionHolder, Assertion previousAssertion) {
        consoleContext.put("commonUIServices", new CommonUIServicesImpl(customAssertionHolder, previousAssertion));
    }

    public static void addKeyValueStoreServices(Map<String, Object> consoleContext) {
        consoleContext.put("keyValueStoreServices", new KeyValueStoreServicesImpl());
    }

    public static boolean isSupportedDataType(Type dataType) {
        if (dataType instanceof Class) {
            return isSupportedDataType((Class) dataType);
        } else if (dataType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) dataType;
            if (!HashMap.class.getName().equals(((Class) parameterizedType.getRawType()).getName())) {
                return false;
            }
            for (Type actualTypeArgument: parameterizedType.getActualTypeArguments()) {
                if (!String.class.getName().equals(((Class) actualTypeArgument).getName())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Check data type of String, primitive, and array of String or primitive.
     *
     * @param inDataTypeClass: data type class to be checked.
     * @return true if the data type is one of String, primitive, and array of String or primitive.
     */
    protected static boolean isSupportedDataType(Class inDataTypeClass) {
        Class dataTypeClass;
        if (inDataTypeClass.isArray()) {
            dataTypeClass = inDataTypeClass.getComponentType();
        } else {
            dataTypeClass = inDataTypeClass;
        }
        return dataTypeClass.isPrimitive() || String.class.getName().equals(dataTypeClass.getName());
        // add support for more data types here (e.g. Collection types)
    }
}