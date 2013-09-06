package com.l7tech.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.util.ClassUtils;
import com.l7tech.util.DomUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Ensures that all admin methods have a @Secured annotation.
 * <p/>
 * If a method should not be checked by the interceptor, use a @Secured annotation
 * with an explicit method stereotype of UNCHECKED_WIDE_OPEN.
 */
public class AdminMethodSecuredAnnotationTest {

    public static final String SPRING_NS = "http://www.springframework.org/schema/beans";

    static final Set<String> ignoredBeans = new HashSet<>(Arrays.asList(
        // Cluster log access is only for node-to-node log downloads and uses its own security check
        // based on TLS mutual auth (both client and server must prove possession of the cluster default SSL key)
        "clusterLogAccessAdmin"
    ));

    @Test
    public void testAllAdminMethodsAreSecured() throws Exception {
        Document adminContext = XmlUtil.parse(getClass().getClassLoader().getResourceAsStream("com/l7tech/server/resources/adminContext.xml"));
        List<Element> beans = DomUtils.findChildElementsByName(adminContext.getDocumentElement(), SPRING_NS, "bean");
        for (Element bean : beans) {
            String id = bean.getAttribute("id");
            if (!id.endsWith("Admin"))
                continue;

            if (ignoredBeans.contains(id))
                continue;

            final String classname = bean.getAttribute("class");
            if (classname == null || classname.length() < 1) // We won't try to check beans that inherit impls, for now
                continue;

            Class<?> adminClass = getClass().getClassLoader().loadClass(classname);

            testClassHasSecuredAnnotation(adminClass);

            for ( Class interfaceClass : adminClass.getInterfaces() ) {
                if ( interfaceClass.getName().endsWith("Admin") ) {
                    testAllAdminMethodsHaveSecuredAnnotation(interfaceClass);
                }
            }
        }
    }

    private void testClassHasSecuredAnnotation(Class<?> adminClass) {
        List<Secured> annotations = new ArrayList<>();
        collectClassAnnotations(adminClass, annotations);

        assertTrue("Admin interface must declare @Secured as class annotation: " + adminClass, !annotations.isEmpty());
    }

    private static void collectClassAnnotations(Class clazz, List<Secured> annotations) {
        //noinspection unchecked
        Secured secured = (Secured) clazz.getAnnotation(Secured.class);
        if (secured != null) annotations.add(secured);
        for (Class intf : clazz.getInterfaces()) {
            collectClassAnnotations(intf, annotations);
        }
    }

    private void testAllAdminMethodsHaveSecuredAnnotation(Class clazz) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            Secured secured = method.getAnnotation(Secured.class);
            assertNotNull("Admin method lacks @Secured annotation: " + ClassUtils.getMethodName(method), secured);

            int numSecured = 0;
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Secured) {
                    numSecured++;
                }
            }
            assertTrue("Admin method lacks @Secured annotation (??): " + ClassUtils.getMethodName(method), numSecured > 0); // can't fail here
            assertTrue("Admin method has multiple @Secured annotations: " + ClassUtils.getMethodName(method), numSecured == 1);
        }
    }
}
