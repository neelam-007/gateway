package com.l7tech.policy;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class AllAssertionsTest {

    @Test
    public void testMetadataConstruction() throws Exception {
        final Assertion[] assertions = AllAssertions.SERIALIZABLE_EVERYTHING;
        for ( final Assertion assertion : assertions ) {
            assertion.meta();
        }
    }

    @Test
    public void testAssertionMetadataConsistency() throws Throwable {
        Assertion[] assertions = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion assertion : assertions) {
            try {
                testAssertionMetadataConsistency(assertion);
            } catch (AssertionError e) {
                throw new AssertionError("Failure for assertion " + assertion.getClass().getName() + ": " + ExceptionUtils.getMessage(e)).initCause(e);
            }
        }
    }

    public static void testAssertionMetadataConsistency(Assertion assertion) {        
        boolean messageTargetable = assertion instanceof MessageTargetable;
        boolean processesRequest = assertion.getClass().isAnnotationPresent(ProcessesRequest.class);
        boolean processesResponse = assertion.getClass().isAnnotationPresent(ProcessesResponse.class);
        boolean usesVariables = assertion instanceof UsesVariables;

        assertTrue("MessageTargetable conflicts with @ProcessesRequest", !messageTargetable || messageTargetable != processesRequest);
        assertTrue("MessageTargetable conflicts with @ProcessesResponse", !messageTargetable || messageTargetable != processesResponse);
        assertTrue("@ProcessesRequest conflicts with @ProcessesResponse", !processesRequest || processesRequest != processesResponse);
        try {
            // all getVariablesUsed implemenations must be annotated as follows;
            // @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
            assertTrue("Uses variables not annotated for migration", !usesVariables || assertion.getClass().getMethod( "getVariablesUsed" ).isAnnotationPresent( Migration.class ));
        } catch ( NoSuchMethodException e ) {
            // meh
        }
    }

    /**
     * Test for a generic getter with regular setter and vice versa
     */
    @Test
    public void testGenericsMismatch() {
        for ( Assertion assertion : AllAssertions.GATEWAY_EVERYTHING ) {
            List<String> genericgetters = new ArrayList<String>();
            List<String> getters = new ArrayList<String>();
            List<String> genericsetters = new ArrayList<String>();
            List<String> setters = new ArrayList<String>();

            for ( Method method : assertion.getClass().getMethods() ) {
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
                Assert.assertFalse("Assertion '"+assertion.getClass()+"' has mismatched getter/setter : " + genericGetter, !genericsetters.contains(genericGetter) && genericsetters.contains(getter));
            }

            for ( int i=0; i< genericsetters.size(); i++ ) {
                String genericSetter = genericsetters.get(i);
                String setter = setters.get(i);
                Assert.assertFalse("Assertion '"+assertion.getClass()+"' has mismatched getter/setter : " + genericSetter, !genericgetters.contains(genericSetter) && genericgetters.contains(setter));
            }
        }
    }

    @Test
    public void testCloneIsDeepCopy() {
        for ( Assertion assertion : AllAssertions.GATEWAY_EVERYTHING ) {
            checkCloneIsDeepCopy(assertion);
        }
    }
    /**
     * Test for deep clone
     *
     * Note: This does not test that values of Arrays or Collections are deep clones.
     */
    public static void checkCloneIsDeepCopy(final Assertion assertion) {
        List<Method> getters = new ArrayList<Method>();

        for ( Method method : assertion.getClass().getMethods() ) {
            if (Modifier.isStatic(method.getModifiers()))
                continue;

            String name = method.getName();
            Class[] parameterTypes = method.getParameterTypes();

            if (name.startsWith("get") && name.length() > 3 && parameterTypes.length==0 && !TypeMappingUtils.isIgnorableProperty(name.substring(3))) {
                getters.add(method);
            }
        }

        Assertion copy = (Assertion) assertion.clone();
        // Note Arrays are cloneable but must explicitly be cloned otherwise a cloned instance will share the same array instance as the original
        for ( Method method : getters ) {
            try {
                Object o1 = method.invoke(assertion);
                if ( o1==null ||
                        o1 instanceof String ||                       // Add your own "immutable" classes here
                        o1 instanceof XmlSecurityRecipientContext ||
                        o1 instanceof SslAssertion.Option ||
                        o1 instanceof XpathExpression ||
                        o1 instanceof TimeUnit ||
                        o1 instanceof SecurityTokenType ||
                        o1 instanceof WsTrustRequestType ||
                        o1 instanceof HtmlFormDataLocation ||
                        o1 instanceof HtmlFormDataType ||
                        o1 instanceof Goid ||
                        o1 instanceof Integer ||
                        o1.getClass().isEnum() ||
                        o1.getClass().isPrimitive() ) {
                    continue;
                }
                Object o2 = method.invoke(copy);
                Assert.assertTrue("Assertion property not cloned : " + assertion.getClass().getName() + "." + method.getName(), o1!=o2);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
    }
}
