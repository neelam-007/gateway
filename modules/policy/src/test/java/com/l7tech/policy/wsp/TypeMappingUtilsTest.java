/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 4, 2009
 * Time: 11:21:03 AM
 */
package com.l7tech.policy.wsp;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;

/**
 * Specifically tests the TypeMappingUtils.getClassForType() and findTypeMapping() methods
 */
public class TypeMappingUtilsTest {

    @Test
    public void testGetTypeForClass() throws NoSuchMethodException {

        Class clazz = TestType.class;
        Method method = clazz.getMethod("getSingleTypeParamMethod");
        Type t1 = method.getGenericReturnType();
        //redundant, just for illustration
        Assert.assertTrue(t1 instanceof ParameterizedType);
        
        Class c = TypeMappingUtils.getClassForType(t1);
        Assert.assertEquals("Retrieved class is incorrect",c, List.class);
    }

    private static class TestType {

        public List<String> getSingleTypeParamMethod(){
            return new ArrayList<String>();
        }

        public Map<String, Integer> getStringIntegerParamMethod(){
            return new HashMap<String, Integer>();
        }

        public List<Double> getDoubleTypeParamMethod(){
            return new ArrayList<Double>();
        }
    }

    /**
     * Tests that the TypeMappingUtils.findTypeMapping() method correctly finds TypeMappings when it is passed a
     * Collection with more than one CollectionTypeMapping with a mapped class of List.
     * Confirms that TypeMappings are found when they should be, and that it returns null when it can't find the
     * correct TypeMapping, even when the mappings Collection contains almost matching TypeMappings, but differ only
     * on their parameterized type argument
     * @throws NoSuchMethodException
     */
    @Test
    public void testFindTypeMapping() throws NoSuchMethodException {
        //Test with two different Collections
        CollectionTypeMapping listIntegerMaping =
                new CollectionTypeMapping(List.class, Integer.class, ArrayList.class, "NotUsedInTest");

        CollectionTypeMapping listStringMaping =
                new CollectionTypeMapping(List.class, String.class, ArrayList.class, "NotUsedInTest");
        
        //order is very important to ensure we find the correct TypeMapping
        Collection<TypeMapping> mappings = Arrays.asList(new TypeMapping[]{listIntegerMaping, listStringMaping});

        Class clazz = TestType.class;
        Method method = clazz.getMethod("getSingleTypeParamMethod");
        Type t1 = method.getGenericReturnType();

        TypeMapping tm = TypeMappingUtils.findTypeMapping(t1, mappings, null);

        Assert.assertNotNull("TypeMapping should have been found", tm);

        Assert.assertEquals("Incorrect TypeMapping found", listStringMaping, tm);

        //Make sure null is returned when mapping is not found

        method = clazz.getMethod("getDoubleTypeParamMethod");
        Type doubleType = method.getGenericReturnType();

        TypeMapping notFound = TypeMappingUtils.findTypeMapping(doubleType, mappings, null);
        Assert.assertNull("No mapping should be found", notFound);

        CollectionTypeMapping listDoubleMapping =
                new CollectionTypeMapping(List.class, Double.class, ArrayList.class, "NotUsedInTest");
        mappings = Arrays.asList(new TypeMapping[]{listIntegerMaping, listStringMaping, listDoubleMapping}); 

        TypeMapping doubleMapping = TypeMappingUtils.findTypeMapping(doubleType, mappings, null);
        Assert.assertNotNull("Double mpping should be found", doubleMapping);

        Assert.assertEquals("Incorrect TypeMapping found", listDoubleMapping, doubleMapping);        
    }

    /**
     * If a Map TypeMapping is introduced this tests it. The core test is that the ParameterizedMapping interface
     * class, whose method getMappedObjectsParameterizedClasses() returns a Class[] is correctly used to distinguish
     * between TypeMappings when there is more than one parameterized type in the mapped class.
     * @throws NoSuchMethodException
     */
    @Test
    public void testMapWithFindTypeMapping() throws NoSuchMethodException {

        TestMapTypeMapping mapStringString = new
                TestMapTypeMapping(Map.class, new Class[]{String.class, String.class}, "NotUsedInTest");

        TestMapTypeMapping mapStringInteger = new
                TestMapTypeMapping(Map.class, new Class[]{String.class, Integer.class}, "NotUsedInTest");

        //order is very important to ensure we find the correct TypeMapping
        Collection<TypeMapping> mappings = Arrays.asList(new TypeMapping[]{mapStringString, mapStringInteger});

        Class clazz = TestType.class;
        Method method = clazz.getMethod("getStringIntegerParamMethod");
        Type t1 = method.getGenericReturnType();

        TypeMapping tm = TypeMappingUtils.findTypeMapping(t1, mappings, null);

        Assert.assertNotNull("TypeMapping should have been found", tm);

        Assert.assertEquals("Incorrect TypeMapping found", mapStringInteger, tm);

    }

    private static class TestMapTypeMapping extends BasicTypeMapping implements ParameterizedMapping{
        private Class[] valueClasses;

        /**
         *
         * @param collectionType not used, for illustration only
         * @param valueClasses
         * @param externalName
         */
        public TestMapTypeMapping(Class collectionType, Class [] valueClasses, String externalName) {
            super(collectionType, externalName);
            this.valueClasses = valueClasses;
        }

        public Class[] getMappedObjectsParameterizedClasses() {
            return valueClasses;
        }
    }

}
