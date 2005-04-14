/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A TypeMapping that supports a bean-like object with a default constructor and some getters and setters.
 */
class BeanTypeMapping extends ComplexTypeMapping {
    private static final Logger log = Logger.getLogger(BeanTypeMapping.class.getName());

    BeanTypeMapping(Class clazz, String externalName) {
        super(clazz, externalName);
    }

    public BeanTypeMapping(Class clazz, String externalName, String nsUri, String nsPrefix) {
        super(clazz, externalName, nsUri, nsPrefix);
    }

    protected void populateElement(Element element, TypedReference object) {
        try {
            emitBeanProperties(object.target, element);
        } catch (InvocationTargetException e) {
            throw new InvalidPolicyTreeException(e);
        } catch (IllegalAccessException e) {
            throw new InvalidPolicyTreeException(e);
        }
    }

    protected void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        Object target = object.target;

        // gather properties
        List properties = TypeMappingUtils.getChildElements(source);
        for (Iterator i = properties.iterator(); i.hasNext();) {
            Element kid = (Element)i.next();
            String parm = kid.getLocalName();

            TypedReference thawedReference = WspConstants.typeMappingObject.thaw(kid, visitor);
            callSetMethod(source, kid, target, parm, thawedReference, visitor);
        }
    }

    private void callSetMethod(Element targetSource,
                               Element propertySource,
                               Object target,
                               String parm,
                               TypedReference value,
                               WspVisitor visitor)
            throws InvalidPolicyStreamException
    {
        Object[] parameter = new Object[] { value.target };
        Class tryType = value.type;
        String methodName = "set" + parm;
        log.finest("Trying to set property: " + target.getClass() + ".set" + parm + "(" +
                      (value.target == null ? "null" : value.target.getClass().getName()) + ")");
        try {
            Method setter = null;
            do {
                try {
                    setter = target.getClass().getMethod(methodName, new Class[]{tryType});
                } catch (NoSuchMethodException e) {
                    tryType = tryType.getSuperclass();
                    if (tryType == null) {
                        // out of superclasses; buck stops here
                        visitor.unknownProperty(targetSource, propertySource, target, parm, value, null);
                        return;
                    }
                }
            } while (setter == null);
            TypeMappingUtils.invokeMethod(setter, target, parameter);
        } catch (SecurityException e) {
            visitor.unknownProperty(targetSource, propertySource, target, parm, value, e);
        } catch (IllegalAccessException e) {
            visitor.unknownProperty(targetSource, propertySource, target, parm, value, e);
        } catch (InvocationTargetException e) {
            visitor.unknownProperty(targetSource, propertySource, target, parm, value, e);
        }
    }

    /**
     * Add the properties of a Bean style object to its already-created node in a document.
     *
     * @param bean    The bean to serialize
     * @param element The assertion's already-created node, to which we will appendChild() each property we find.
     * @throws java.lang.reflect.InvocationTargetException
     * @throws IllegalAccessException
     */
    private void emitBeanProperties(Object bean, Element element) throws InvocationTargetException, IllegalAccessException
    {
        // Create a template object if we can, so we can avoid saving properties that are just defaulted anyway
        Object defaultTemplate = null;
        if (constructor != null) {
            try {
                defaultTemplate = constructor.newInstance(new Object[0]);
            } catch (InstantiationException e) {
                defaultTemplate = null;
            } catch (IllegalAccessException e) {
                defaultTemplate = null;
            } catch (InvocationTargetException e) {
                defaultTemplate = null;
            }
        }

        Class ac = bean.getClass();
        Map setters = new HashMap();
        Map getters = new HashMap();
        Method[] methods = ac.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String name = method.getName();
            if (name.startsWith("is") && name.length() > 2 && method.getReturnType().equals(boolean.class))
                getters.put(name.substring(2), method);
            else if (name.startsWith("get") && name.length() > 3)
                getters.put(name.substring(3), method);
            else if (name.startsWith("set") && name.length() > 3)
                setters.put(name.substring(3) + ":" + method.getParameterTypes()[0], method);
        }
        for (Iterator i = getters.keySet().iterator(); i.hasNext();) {
            String parm = (String)i.next();
            if (TypeMappingUtils.isIgnorableProperty(parm))
                continue;
            Method getter = (Method)getters.get(parm);
            if (getter == null)
                throw new InvalidPolicyTreeException("Internal error"); // can't happen

            if (Modifier.isStatic(getter.getModifiers())) { // ignore statics
                continue;
            }

            Method setter = (Method)setters.get(parm + ":" + getter.getReturnType());
            if (setter == null)
                throw new InvalidPolicyTreeException("WspWriter: Warning: class " + bean.getClass() + ": no setter found for parameter " + parm);
            Class returnType = getter.getReturnType();
            if (!setter.getParameterTypes()[0].equals(returnType))
                throw new InvalidPolicyTreeException("class has getter and setter for " + parm + " which disagree about its type");
            TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(returnType);
            if (tm == null)
                throw new InvalidPolicyTreeException("class " + bean.getClass() + " has property \"" + parm + "\" with unsupported type " + returnType);
            final Object[] noArgs = new Object[0];
            Object value = TypeMappingUtils.invokeMethod(getter, bean, noArgs);

            if (defaultTemplate != null) {
                // See if we can skip saving this property.  We'll skip it if the default object has the same value.
                Object defaultValue = TypeMappingUtils.invokeMethod(getter, defaultTemplate, noArgs);
                if (value == defaultValue)
                    continue;
                if (value != null && defaultValue != null && value.equals(defaultValue))
                    continue;
            }

            TypedReference tr = new TypedReference(returnType, value, parm);
            tm.freeze(tr, element);
        }
    }
}
