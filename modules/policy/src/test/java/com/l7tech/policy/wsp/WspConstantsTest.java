/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.AllAssertions;

import java.util.logging.Logger;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;
import org.junit.Assert;

/**
 * Ensures that everything that can be serialized is also listed in {@link AllAssertions#SERIALIZABLE_EVERYTHING}.
 */
public class WspConstantsTest {
    private static Logger log = Logger.getLogger(WspConstantsTest.class.getName());

    private String getLocalName(Class c) {
        String fullname = c.getName();
        int lastdot = fullname.lastIndexOf('.');
        return fullname.substring(lastdot + 1);
    }

    @Test
    public void testDuplicateMappings() throws Exception {
        TypeMapping[] mappings = WspConstants.typeMappings;
        Set<String> typeMapSet = new HashSet<String>();

        // Accepted duplication for backwards compatibility
        Set<String> dupeWhitelist = new HashSet<String>( Arrays.asList(
            "com.l7tech.policy.wsp.ArrayTypeMapping/[Ljava.lang.String;/null/fieldNames"
        ) );

        // Check for duplicates
        for (TypeMapping typeMapping : mappings) {
            String mappingId = typeMapping.getClass().getName() + "/" + typeMapping.getMappedClass().getName() + "/" + typeMapping.getSinceVersion();
            String fullId = mappingId + "/" + typeMapping.getExternalName();
            if ( !typeMapSet.add(mappingId) && !dupeWhitelist.contains(fullId)) {
                Assert.fail("Duplicate type mapping: " + fullId);
            }
        }
    }

    @Test
    public void testAllAssertionsIsComplete() throws Exception {
        Set<Class<? extends Assertion>> assertionClasses = new HashSet<Class<? extends Assertion>>();
        Assertion[] all = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion ass : all)
            assertionClasses.add(ass.getClass());

        boolean failed = false;
        Collection<Class<? extends Assertion>> needed = new ArrayList<Class<? extends Assertion>>();

        TypeMapping[] mappings = WspConstants.typeMappings;
        for (TypeMapping typeMapping : mappings) {
            Class clazz = typeMapping.getMappedClass();
            if (Assertion.class.isAssignableFrom(clazz)) {
                if (!assertionClasses.contains(clazz)) {
                    //noinspection unchecked
                    needed.add(clazz);
                    log.warning("Class not listed in AllAssertions.SERIALIZABLE_EVERYTHING: " + clazz.getName());
                    failed = true;
                }
            }
        }

        if (failed) {
            System.err.println("Add the following lines to AllAssertions.SERIALIZABLE_EVERYTHING: \n");
            for (Class<? extends Assertion> assclass : needed) {
                String assname = getLocalName(assclass);
                System.out.println("        new " + assname + "(),");
            }

            Assert.fail("At least one assertion mapping was found for an assertion class not represented in AllAssertions.SERIALIZABLE_EVERYTHING");
        }
    }

    /**
     * Test for a generic getter with regular setter and vice versa
     */
    @Test
    public void testGenericsMismatch() {
        TypeMapping[] mappings = WspConstants.typeMappings;
        for (TypeMapping typeMapping : mappings) {
            List<String> genericgetters = new ArrayList<String>();
            List<String> getters = new ArrayList<String>();
            List<String> genericsetters = new ArrayList<String>();
            List<String> setters = new ArrayList<String>();

            if ( !(typeMapping instanceof BeanTypeMapping) ) continue;

            for ( Method method : typeMapping.getMappedClass().getMethods() ) {
                if (Modifier.isStatic(method.getModifiers()))
                    continue;

                String name = method.getName();
                Class[] parameterTypes = method.getParameterTypes();

                if ( name.startsWith("set") &&
                     parameterTypes.length != 1 ) {
                    continue;
                }

                if (name.startsWith("get") && name.length() > 3) {
                    genericgetters.add(name.substring(3) + ":" + method.getGenericReturnType());
                    getters.add(name.substring(3) + ":" + method.getReturnType());
                } else if (name.startsWith("set") && name.length() > 3) {
                    genericsetters.add(name.substring(3) + ":" + method.getGenericParameterTypes()[0]);
                    setters.add(name.substring(3) + ":" + method.getParameterTypes()[0]);
                }
            }

            for ( int i=0; i< genericgetters.size(); i++ ) {
                String genericGetter = genericgetters.get(i);
                String getter = getters.get(i);
                Assert.assertFalse("TypeMapping '"+typeMapping+"' for '"+typeMapping.getMappedClass()+"' has mismatched getter/setter : " + genericGetter, !genericsetters.contains(genericGetter) && genericsetters.contains(getter));
            }

            for ( int i=0; i< genericsetters.size(); i++ ) {
                String genericSetter = genericsetters.get(i);
                String setter = setters.get(i);
                Assert.assertFalse("TypeMapping '"+typeMapping+"' for '"+typeMapping.getMappedClass()+"' has mismatched getter/setter : " + genericSetter, !genericgetters.contains(genericSetter) && genericgetters.contains(setter));
            }
        }
    }

}
