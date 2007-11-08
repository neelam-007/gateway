/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * TypeMapping that knows how to serialize collections.  This superclass will always use the prototype class
 * when deserializing.
 */
public class CollectionTypeMapping extends ComplexTypeMapping {
    private final Constructor implConstructor;
    private final Class valueType;
    private final Class implClass;

    /**
     * Create a class mapping that will serialize the specified concrete Collection type.
     * Deserialized Collection member objects will be rejected unless they are instances of valueType.
     * Deserialized Collection members may not be null.
     *
     * @param collectionType  the collection type, ie List.class
     * @param valueType       the type of object that this collection will permit to be deserialized, ie String.class or Object.class
     * @param implClass       concrete implementation to use when
     * @param externalName    the name to use to represent this mapped type inside the XML
     */
    public CollectionTypeMapping(Class collectionType, Class valueType, Class implClass, String externalName) {
        super(collectionType, externalName);
        if (!Collection.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("Unable to create CollectionTypeMapping: class is not assignable to Collection");
        if (valueType == null)
            throw new IllegalArgumentException("Unable to create CollectionTypeMapping: value type must not be null");
        if (implClass == null)
            throw new IllegalArgumentException("Unable to create CollectionTypeMapping: implementation class must not be null");
        this.implClass = implClass;
        this.valueType = valueType;
        try {
            implConstructor = implClass.getConstructor(new Class[0]);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Unable to create CollectionTypeMapping for class " + implClass.getName() +
                    ": class has no default constructor");
        }

        // Make sure the default constructor works
        final Object o;
        try {
            o = implConstructor.newInstance(new Object[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create CollectionTypeMapping for class " + implClass.getName() +
                    ": unable to invoke default constructor: ", e);
        }

        if (!(o instanceof Collection))
            throw new IllegalArgumentException("Class " + clazz + ": c'tor didn't make Collection"); // can't happen
    }

    protected void populateElement(WspWriter wspWriter, Element newElement, TypedReference object) throws InvalidPolicyTreeException {
        Collection collection = (Collection)object.target;
        Object[] array = collection.toArray();
        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            WspConstants.typeMappingObject.freeze(wspWriter, new TypedReference(Object.class, o, "item"), newElement);
        }
    }

    protected TypedReference createObject(Element element, String value, WspVisitor visitor) throws InvalidPolicyStreamException {
        Collection objects = null;
        try {
            objects = (Collection)implConstructor.newInstance(new Object[0]);
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to create " + implClass, e);
        }
        List arrayElements = TypeMappingUtils.getChildElements(element, "item");
        for (Iterator i = arrayElements.iterator(); i.hasNext();) {
            Element kidElement = (Element)i.next();
            TypedReference ktr = WspConstants.typeMappingObject.thaw(kidElement, visitor);

            if (ktr.target == null || !valueType.isAssignableFrom(ktr.target.getClass())) {
                String type = ktr.target == null ? "null" : ktr.target.getClass().getName();
                throw new InvalidPolicyStreamException(getClass().getName() + " unable to accept member of type " + type);
            }

            objects.add(ktr.target);
        }
        try {
            return new TypedReference(clazz, objects, element.getLocalName());
        } catch (ArrayStoreException e) {
            throw new InvalidPolicyStreamException("Array item with incompatible type", e);
        }
    }
}
