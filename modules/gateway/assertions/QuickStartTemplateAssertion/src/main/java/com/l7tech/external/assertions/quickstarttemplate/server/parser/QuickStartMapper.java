package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPolicyBuilderException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionStringEncoding;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionType;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.util.EnumTranslator;
import com.l7tech.util.HexUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuickStartMapper {
    private static final Logger logger = Logger.getLogger(QuickStartMapper.class.getName());

    @NotNull
    private final QuickStartAssertionLocator assertionLocator;

    @NotNull
    private final AssertionMapper assertionMapper;

    @NotNull
    private final ClusterPropertyManager clusterPropertyManager;

    public QuickStartMapper(
            @NotNull final QuickStartAssertionLocator assertionLocator,
            @NotNull final AssertionMapper assertionMapper,
            @NotNull final ClusterPropertyManager clusterPropertyManager
    ) {
        this.assertionLocator = assertionLocator;
        this.assertionMapper = assertionMapper;
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public void setAssertionRegistry(AssertionRegistry assertionRegistry) {
        assertionLocator.setAssertionRegistry(assertionRegistry);
    }

    /**
     * For each name
     *    - look up encapsulated assertion by name to get guid
     *      - if exists and if applicable, set argument(s)
     *    - else try as assertion, if applicable, use mapped name
     *      - throw exception if assertion is unsupported and the allow-all-assertion flag is false
     */
    @NotNull
    public List<Assertion> getAssertions(@NotNull final Service service) throws QuickStartPolicyBuilderException, FindException {
        final List<Assertion> assertions = new ArrayList<>();
        for (final Map<String, Map<String, ?>> policyMap : service.policy) {
            // We know there is only one thing in this map, we've previously validated this.
            final String templateName = policyMap.keySet().iterator().next();

            final EncapsulatedAssertion encapsulatedAssertion = assertionLocator.findEncapsulatedAssertion(templateName);
            if (encapsulatedAssertion != null) {
                // Process as encass
                setEncassArguments(encapsulatedAssertion, policyMap.get(templateName));
                assertions.add(encapsulatedAssertion);
            } else {
                // Process as assertion
                final AssertionSupport assertionSupport = assertionMapper.getSupportedAssertions().get(templateName);
                if (assertionSupport == null && !Boolean.valueOf(clusterPropertyManager.getProperty(QuickStartTemplateAssertion.ENABLE_ALL_ASSERTIONS_FLAG_KEY))) {
                    // this assertion is unsupported and the allow-all-assertion flag is false
                    throw new QuickStartPolicyBuilderException("Template item named " + templateName + " is not registered on the Gateway.");
                }

                String assertionName = assertionSupport == null ? templateName : assertionSupport.getExternalName();
                final Assertion assertion = assertionLocator.findAssertion(assertionName);
                if (assertion == null) {
                    // the template name matches a supported assertion but the assertion is not found on the gateway registry this is a misconfiguration on our part!
                    throw new QuickStartPolicyBuilderException("Assertion " + assertionName + " for policy template item named " + templateName + " is not registered on the Gateway.");
                }
                callAssertionSetter(assertionSupport, assertion, policyMap.get(assertionName));
                assertions.add(assertion);
            }
        }
        return assertions;
    }

    private void setEncassArguments(@NotNull final EncapsulatedAssertion encapsulatedAssertion, @NotNull final Map<String, ?> properties) throws QuickStartPolicyBuilderException {
        if (encapsulatedAssertion.config() == null) {
            throw new IllegalStateException("Unable to obtain the encapsulated assertion config object.");
        }

        for (final Map.Entry<String, ?> entry : properties.entrySet()) {
            final EncapsulatedAssertionArgumentDescriptor descriptor = findArgumentDescriptor(entry.getKey(), encapsulatedAssertion);
            if (descriptor == null) {
                throw new QuickStartPolicyBuilderException("Incorrect encapsulated assertion property: " + entry.getKey() + ", for encapsulated assertion: " + Optional.ofNullable(encapsulatedAssertion.config()).map(EncapsulatedAssertionConfig::getName).orElse("<null>"));
            }
            // Don't know the type... Can't, so we have to check a number of different types.
            final Object propertyValue = entry.getValue();
            String resultingValue;
            if (propertyValue instanceof Iterable) {
                // If it's an iterable, we cannot pass arrays to encapsulated assertions, so we merge them together
                // like this into a semicolon delimited string.
                resultingValue = Joiner.on(";").join((Iterable) propertyValue);
            } else if (propertyValue instanceof Map) {
                // If it's a map, assume it's json, retain double quotes when parsing
                try {
                    resultingValue = new ObjectMapper().writeValueAsString(propertyValue);
                } catch (IOException e) {
                    throw new QuickStartPolicyBuilderException("Unable to convert Map to JSON string: ", e);
                }
            } else {
                // Convert the value using the encapsulated assertion encoding type.
                resultingValue = EncapsulatedAssertionStringEncoding.encodeToString(descriptor.dataType(), propertyValue);
                // If we couldn't convert it, try a string as a last resort.
                if (resultingValue == null) {
                    resultingValue = propertyValue.toString();
                }
            }
            encapsulatedAssertion.putParameter(entry.getKey(), resultingValue);
        }
    }

    @VisibleForTesting
    void callAssertionSetter(@Nullable final AssertionSupport assertionSupport,
                             @NotNull final Assertion assertion,
                             @NotNull final Map<String, ?> properties) throws QuickStartPolicyBuilderException {
        try {
            Method method = null;
            String setMethodName;
            propertiesLoop:
            for (final Map.Entry<String, ?> entry : properties.entrySet()) {
                final String propertyName = entry.getKey();
                Object propertyValue = entry.getValue();

                // map template property with internal setter
                final String fieldName = Optional.ofNullable(assertionSupport)
                        .map(AssertionSupport::getProperties)
                        .map(pn -> pn.get(propertyName))
                        .orElse(propertyName);
                setMethodName = "set" + fieldName;

                // if applicable base 64 encode the string
                final Boolean isFieldBase64Encoded = Optional.ofNullable(assertionSupport)
                        .map(AssertionSupport::getPropertiesIsBase64Encoded)
                        .map(pb64s -> pb64s.get(propertyName))
                        .orElse(Boolean.FALSE);
                if (isFieldBase64Encoded && propertyValue instanceof String) {
                    propertyValue = HexUtils.encodeBase64((propertyValue.toString().getBytes()));
                }

                try {
                    method = assertion.getClass().getMethod(setMethodName, propertyValue.getClass());
                    method.invoke(assertion, propertyValue);
                    continue;
                } catch (NoSuchMethodException e) {
                    logger.log(Level.FINE, "Reflection failed to invoke : " + (method == null ? setMethodName : method.toString()));
                    // continue trying to cast input type to other expected types
                }

                // these util methods were tried without success:
                //      org.apache.commons.lang.reflect.MethodUtils.invokeMethod, org.apache.commons.beanutils.MethodUtils.invokeMethod, org.springframework.util.ReflectionUtils.findMethod, java.beans.Statement.execute

                // try primitive and primitive wrapper types for the method argument
                if (propertyValue.getClass().isArray()) {
                    if (propertyValue.getClass().getComponentType().isPrimitive()) {
                        try {
                            Class wrapperArrayClass = com.l7tech.util.ClassUtils.primitiveArrayToWrapperArray(propertyValue.getClass());
                            method = assertion.getClass().getMethod(setMethodName, wrapperArrayClass);
                            invokePrimitiveArrayMethod(method, wrapperArrayClass, assertion, propertyValue);
                            continue;
                        } catch (NoSuchMethodException e) {
                            logger.log(Level.FINE, "Reflection failed to invoke : " + (method == null ? setMethodName : method.toString()));
                        }
                    } else if (com.l7tech.util.ClassUtils.isPrimitiveWrapper(propertyValue.getClass().getComponentType())) {
                        try {
                            Class primitiveArrayClass = com.l7tech.util.ClassUtils.wrapperArrayToPrimitiveArray(propertyValue.getClass());
                            method = assertion.getClass().getMethod(setMethodName, primitiveArrayClass);
                            invokeWrapperArrayMethod(method, primitiveArrayClass, assertion, propertyValue);
                            continue;
                        } catch (NoSuchMethodException e) {
                            logger.log(Level.FINE, "Reflection failed to invoke : " + (method == null ? setMethodName : method.toString()));
                        }
                    }
                } else {   // not an array
                    if (propertyValue.getClass().isPrimitive()) {
                        try {
                            method = assertion.getClass().getMethod(setMethodName, ClassUtils.primitiveToWrapper(propertyValue.getClass()));
                            method.invoke(assertion, propertyValue);
                            continue;
                        } catch (NoSuchMethodException e) {
                            logger.log(Level.FINE, "Reflection failed to invoke : " + (method == null ? setMethodName : method.toString()));
                        }
                    } else if (com.l7tech.util.ClassUtils.isPrimitiveWrapper(propertyValue.getClass())) {
                        try {
                            method = assertion.getClass().getMethod(setMethodName, ClassUtils.wrapperToPrimitive(propertyValue.getClass()));
                            method.invoke(assertion, propertyValue);
                            continue;
                        } catch (NoSuchMethodException e) {
                            logger.log(Level.FINE, "Reflection failed to invoke : " + (method == null ? setMethodName : method.toString()));
                        }
                    }
                }

                // hard-coded dependencies to core Gateway as near last resort - hopefully this list remains small
                if ("setProtections".equals(setMethodName)) {
                    try {
                        handleSetProtections(setMethodName, assertion, propertyValue);
                        continue;
                    } catch (NoSuchMethodException e2) {
                        logger.log(Level.FINE, "Reflection failed to invoke : " + setMethodName);
                    }
                }


                // looping through all methods as last resort - performance hit
                for (Method declaredMethod : assertion.getClass().getMethods()) {
                    // find matching method name with one argument signature
                    if (setMethodName.equals(declaredMethod.getName()) && declaredMethod.getParameterCount() == 1) {
                        Class<?>[] declaredMethodParameterTypes = declaredMethod.getParameterTypes();
                        Class<?> declaredMethodParameterType = declaredMethodParameterTypes[0];

                        // try to convert string to method type using EnumTranslator (e.g. com.l7tech.policy.assertion.SslAssertion.Option)
                        try {
                            Method getEnumTranslatorMethod = declaredMethodParameterType.getMethod("getEnumTranslator");
                            EnumTranslator enumTranslator = (EnumTranslator) getEnumTranslatorMethod.invoke(null);
                            declaredMethod.invoke(assertion, enumTranslator.stringToObject(propertyValue.toString()));
                            continue propertiesLoop;
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            logger.log(Level.FINE, "Reflection failed to invoke : " + declaredMethod.toString());
                        }

                        // try to find and instantiate parameter constructor that takes a single String (e.g. com.l7tech.objectmodel.Goid)
                        try {
                            Constructor declaredMethodParameterConstructor = declaredMethodParameterType.getDeclaredConstructor(String.class);
                            if (declaredMethodParameterConstructor != null) {
                                declaredMethod.invoke(assertion, declaredMethodParameterConstructor.newInstance(propertyValue));
                            }
                            continue propertiesLoop;
                        } catch (NoSuchMethodException | InstantiationException e) {
                            logger.log(Level.FINE, "Reflection failed to invoke : " + declaredMethod.toString());
                        }

                        // try to convert string to method type using enum valueOf (e.g. com.l7tech.common.http.HttpMethod)
                        try {
                            Method valueOfMethod = declaredMethodParameterType.getMethod("valueOf", String.class);
                            declaredMethod.invoke(assertion, valueOfMethod.invoke(null, propertyValue));
                            continue propertiesLoop;
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            logger.log(Level.FINE, "Reflection failed to invoke : " + declaredMethod.toString());
                        }
                    }

                }

                // can't convert, fail and throw exception
                logger.log(Level.WARNING, "Failed to invoke method: " + setMethodName + "(...) with argument type: " + propertyValue.getClass());
                throw new QuickStartPolicyBuilderException("Unable to set " + assertion.getClass().getSimpleName()+ " - " + fieldName + " = " + entry.getValue());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new QuickStartPolicyBuilderException("Encountered an unexpected error", e);
        }
    }

    private void handleSetProtections(@NotNull final String setMethodName, @NotNull final Assertion assertion, @NotNull final Object propertyValue) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Method method = assertion.getClass().getMethod(setMethodName, CodeInjectionProtectionType[].class);

        // convert string representation to array of CodeInjectionProtectionType
        List codeInjectionProtectionTypeStrings = (List) propertyValue;
        CodeInjectionProtectionType[] codeInjectionProtectionTypes = new CodeInjectionProtectionType[codeInjectionProtectionTypeStrings.size()];
        int index = 0;
        for (Object codeInjectionProtectionTypeString : codeInjectionProtectionTypeStrings) {
            codeInjectionProtectionTypes[index++] = CodeInjectionProtectionType.fromWspName((String) codeInjectionProtectionTypeString);
        }

        method.invoke(assertion, (Object) codeInjectionProtectionTypes);
    }

    private static void invokePrimitiveArrayMethod(@NotNull final Method method, @NotNull final Class wrapperArrayClass,
                                                   @NotNull final Assertion assertion, @NotNull final Object propertyValue) throws IllegalAccessException, InvocationTargetException {
        // this ugly if-else-if workaround is to deal with not being able to dynamically cast e.g. method.invoke(assertion, ArrayUtils.toObject((wrapperArrayClass) propertyValue));
        if (wrapperArrayClass.equals(Boolean[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((boolean[]) propertyValue));
        } else if (wrapperArrayClass.equals(Byte[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((byte[]) propertyValue));
        } else if (wrapperArrayClass.equals(Character[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((char[]) propertyValue));
        } else if (wrapperArrayClass.equals(Short[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((short[]) propertyValue));
        } else if (wrapperArrayClass.equals(Integer[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((int[]) propertyValue));
        } else if (wrapperArrayClass.equals(Long[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((long[]) propertyValue));
        } else if (wrapperArrayClass.equals(Double[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((double[]) propertyValue));
        } else if (wrapperArrayClass.equals(Float[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toObject((float[]) propertyValue));
        }
    }

    private static void invokeWrapperArrayMethod(@NotNull final Method method, @NotNull final Class primitiveArrayClass,
                                                 @NotNull final Assertion assertion, @NotNull final Object propertyValue) throws IllegalAccessException, InvocationTargetException {
        // this ugly if-else-if workaround is to deal with not being able to dynamically cast e.g. method.invoke(assertion, ArrayUtils.toPrimitive((primitiveArrayClass) propertyValue));
        if (primitiveArrayClass.equals(boolean[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Boolean[]) propertyValue));
        } else if (primitiveArrayClass.equals(byte[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Byte[]) propertyValue));
        } else if (primitiveArrayClass.equals(char[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Character[]) propertyValue));
        } else if (primitiveArrayClass.equals(short[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Short[]) propertyValue));
        } else if (primitiveArrayClass.equals(int[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Integer[]) propertyValue));
        } else if (primitiveArrayClass.equals(long[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Long[]) propertyValue));
        } else if (primitiveArrayClass.equals(double[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Double[]) propertyValue));
        } else if (primitiveArrayClass.equals(float[].class)) {
            method.invoke(assertion, (Object) ArrayUtils.toPrimitive((Float[]) propertyValue));
        }
    }

    @Nullable
    private static EncapsulatedAssertionArgumentDescriptor findArgumentDescriptor(@NotNull final String name, @NotNull final EncapsulatedAssertion ea) {
        assert ea.config() != null;
        assert ea.config().getArgumentDescriptors() != null;
        return ea.config().getArgumentDescriptors().stream()
                .filter(ad -> name.equals(ad.getArgumentName()))
                .findFirst()
                .orElse(null);
    }
}
