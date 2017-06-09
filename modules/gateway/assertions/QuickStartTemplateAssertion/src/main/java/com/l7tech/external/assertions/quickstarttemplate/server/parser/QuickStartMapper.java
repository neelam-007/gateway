package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import com.google.common.base.Joiner;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartPolicyBuilderException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionStringEncoding;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.ClassUtils.primitiveToWrapper;
import static org.apache.commons.lang.ClassUtils.wrapperToPrimitive;

public class QuickStartMapper {
    private static final Logger logger = Logger.getLogger(QuickStartMapper.class.getName());

    // allowed assertions (does not include encasses, all encasses allowed)
    private static final HashSet<String> supportedAssertionNames = new HashSet<>(8);
    static {
        supportedAssertionNames.add("CodeInjectionProtection");   // TODO handle setProtections(array) e.g. "Protections" : ["htmlJavaScriptInjection", "phpEvalInjection"]
        supportedAssertionNames.add("CORS");
        supportedAssertionNames.add("HardcodedResponse");   // TODO implement as encass to handle base64 encode/decode
        supportedAssertionNames.add("HttpBasic");
        supportedAssertionNames.add("HttpRouting");
        supportedAssertionNames.add("RateLimit");
        supportedAssertionNames.add("SetVariable");   // TODO implement as encass to handle base64 encode/decode
        supportedAssertionNames.add("Ssl");
        supportedAssertionNames.add("ThroughputQuota");
    }

    // TODO display to internal name mapping

    @NotNull
    private final QuickStartEncapsulatedAssertionLocator assertionLocator;

    public QuickStartMapper(@NotNull final QuickStartEncapsulatedAssertionLocator assertionLocator) {
        this.assertionLocator = assertionLocator;
    }

    // TODO is there a better time in the assertion lifecycle to set assertion registry?
    public void setAssertionRegistry(AssertionRegistry assertionRegistry) {
        assertionLocator.setAssertionRegistry(assertionRegistry);
    }
    /**
     * For each name
     *    - look up encass by name to get guid
     *    - if applicable set encass argument(s)
     */
    @NotNull
    public List<EncapsulatedAssertion> getEncapsulatedAssertions(@NotNull final Service service)
            throws QuickStartPolicyBuilderException, FindException {
        final List<EncapsulatedAssertion> encapsulatedAssertions = new ArrayList<>();
        for (final Map<String, Map<String, ?>> policyMap : service.policy) {
            // We know there is only one thing in this map, we've previously validated this.
            final String name = policyMap.keySet().iterator().next();
            final EncapsulatedAssertion encapsulatedAssertion = assertionLocator.findEncapsulatedAssertion(name);
            if (encapsulatedAssertion == null) {
                throw new QuickStartPolicyBuilderException("Unable to find encapsulated assertion template named : " + name);
            }
            setEncassArguments(encapsulatedAssertion, policyMap.get(name));
            encapsulatedAssertions.add(encapsulatedAssertion);
        }
        return encapsulatedAssertions;
    }

    /**
     * For each name
     *    - look up assertion by name to get guid
     *    - if applicable set argument(s)
     */
    @NotNull
    public List<Assertion> getAssertions(@NotNull final Service service) throws QuickStartPolicyBuilderException, FindException { // throws QuickStartPolicyBuilderException, , NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final List<Assertion> assertions = new ArrayList<>();
        for (final Map<String, Map<String, ?>> policyMap : service.policy) {
            // We know there is only one thing in this map, we've previously validated this.
            final String name = policyMap.keySet().iterator().next();   // TODO map display to internal name

            // check if assertion name is allowed
            Assertion assertion = null;
            if (supportedAssertionNames.contains(name)) {  // TODO cluster property override to allow ALL assertion names
                assertion = assertionLocator.findAssertion(name);
            }

            if (assertion == null) {
                // allow all encasses, no check needed
                final EncapsulatedAssertion encapsulatedAssertion = assertionLocator.findEncapsulatedAssertion(name);
                if (encapsulatedAssertion == null) {
                    throw new QuickStartPolicyBuilderException("Unable to find assertion for policy template item named : " + name);
                }
                // process as encass
                setEncassArguments(encapsulatedAssertion, policyMap.get(name));
                assertions.add(encapsulatedAssertion);
            } else {
                // process as assertion
                callAssertionSetter(assertion, policyMap.get(name));
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
                throw new QuickStartPolicyBuilderException("Incorrect encapsulated assertion property: " + entry.getKey() + ", for encapsulated assertion: " + encapsulatedAssertion.config().getName());
            }
            // Don't know the type... Can't, so we have to check a number of different types.
            final Object propertyValue = entry.getValue();
            String resultingValue;
            if (propertyValue instanceof Iterable) {
                // If it's an iterable, we cannot pass arrays to encapsulated assertions, so we merge them together
                // like this into a semicolon delimited string.
                resultingValue = Joiner.on(";").join((Iterable) propertyValue);
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

    private void callAssertionSetter(@NotNull final Assertion assertion, @NotNull final Map<String, ?> properties) throws QuickStartPolicyBuilderException { // throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        try {
            Method method;
            String setMethodName;
            for (final Map.Entry<String, ?> entry : properties.entrySet()) {
                final Object propertyValue = entry.getValue();
                setMethodName = "set" + entry.getKey();   // TODO map display to internal name
                try {
                    method = assertion.getClass().getMethod(setMethodName, propertyValue.getClass());
                    method.invoke(assertion, propertyValue);
                } catch (NoSuchMethodException e) {
                    logger.log(Level.FINE, "Reflection failed to get/invoke method: " + setMethodName + " with argument type: " + propertyValue.getClass() + ".  Trying again to cast from input type to other expected types.");

                    // try primitive and primitive wrapper types for the method argument
                    if (propertyValue.getClass().isPrimitive()) {
                        try {
                            method = assertion.getClass().getMethod(setMethodName, primitiveToWrapper(propertyValue.getClass()));
                            method.invoke(assertion, propertyValue);
                            continue;
                        } catch (NoSuchMethodException e2) {
                            // do nothing, try next
                            // TODO log fine
                        }
                    } else if (isPrimitiveWrapper(propertyValue.getClass())) {
                        try {
                            method = assertion.getClass().getMethod(setMethodName, wrapperToPrimitive(propertyValue.getClass()));
                            method.invoke(assertion, propertyValue);
                            continue;
                        } catch (NoSuchMethodException e2) {
                            // do nothing, try next
                            // TODO log fine
                        }
                    }
                    // TODO array and collection conversion of primitives and it's wrappers

                    // looping through all methods as last resort - performance hit
                    for (Method declaredMethod : assertion.getClass().getDeclaredMethods()) {
                        if (setMethodName.equals(declaredMethod.getName()) && declaredMethod.getParameterCount() == 1) {
                            Class<?>[] declaredMethodParameterTypes = declaredMethod.getParameterTypes();
                            Class<?> declaredMethodParameterType = declaredMethodParameterTypes[0];

                            // try to convert string to method type using valueOf
                            Method valueOfMethod = declaredMethodParameterType.getMethod("valueOf", String.class);
                            declaredMethod.invoke(assertion, valueOfMethod.invoke(null, propertyValue));

                            // TODO array and collection conversion - e.g. java.util.ArrayList to/from com.l7tech.policy.assertion.CodeInjectionProtectionAssertion.setProtections(CodeInjectionProtectionType[])

                            break;
                        }
                    }

                    // TODO can't convert, throw exception
                    // TODO set HTTP error code here?
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new QuickStartPolicyBuilderException("Encountered an unexpected error", e);
        }
    }

    private static boolean isPrimitiveWrapper(@NotNull final Class<?> c) {
        return primitiveToWrapper(c) != null;
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
