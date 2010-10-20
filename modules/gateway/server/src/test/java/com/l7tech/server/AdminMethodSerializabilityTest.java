package com.l7tech.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class AdminMethodSerializabilityTest {
    public static final String SPRING_NS = "http://www.springframework.org/schema/beans";

    private Method lastMethod;

    @Test
    public void testAllAdminMethodArgumentsAreSerializable() throws Exception {
        Document adminContext = XmlUtil.parse(getClass().getClassLoader().getResourceAsStream("com/l7tech/server/resources/adminContext.xml"));
        List<Element> beans = DomUtils.findChildElementsByName(adminContext.getDocumentElement(), SPRING_NS, "bean");
        for (Element bean : beans) {
            String id = bean.getAttribute("id");
            if (!id.endsWith("Admin"))
                continue;

            final String classname = bean.getAttribute("class");
            if (classname == null || classname.length() < 1) // We won't try to check beans that inherit impls, for now
                continue;

            Class adminClass = getClass().getClassLoader().loadClass(classname);

            try {
                for ( Class interfaceClass : adminClass.getInterfaces() ) {
                    if ( interfaceClass.getName().endsWith("Admin") ) {
                        testAllAdminMethodsAreSerializable(interfaceClass);
                    }
                }
            } catch (AssertionError e) {
                fail("failed for method " + lastMethod + ": " + ExceptionUtils.getMessage(e));
            }
        }
    }

    private void testAllAdminMethodsAreSerializable(Class clazz) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            testMethodReturnValueIsSerializable(method);
            testMethodArgumentsAreSerializable(method);
        }
    }

    private void testMethodReturnValueIsSerializable(Method method) {
        lastMethod = method;
        final Class<?> type = method.getReturnType();
        if (type != void.class)
            testClassIsSerializableOrIsInterface(type);
    }

    private void testMethodArgumentsAreSerializable(Method method) {
        Class<?>[] paramsTypes = method.getParameterTypes();
        for (Class<?> paramsType : paramsTypes) {
            testClassIsSerializableOrIsInterface(paramsType);
        }
    }

    private void testClassIsSerializableOrIsInterface(Class c) {
        assertTrue("Class used as admin method argument or return value is not either serializable or an interface: " + c, c.isInterface() || c.isPrimitive() || Serializable.class.isAssignableFrom(c));
    }
}
