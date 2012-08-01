package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.Base64Value;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.junit.Test;

import static junit.framework.Assert.*;

@SuppressWarnings({"JavaDoc"})
public class AssertionUtilsTest {

    @Test
    public void testGetAllBase64DecodedProperties() throws Exception {
        SetVariableAssertion setVariableAssertion = new SetVariableAssertion();
        final String content = "This is the contents which should be found";
        setVariableAssertion.setExpression(content);

        final String decodedProperties = AssertionUtils.getBase64EncodedPropsDecoded(setVariableAssertion);
        assertEquals(content, decodedProperties);
    }

    /**
     * Null property values cause no NPEs.
     */
    @Test
    public void testPropertyValueIsNull() throws Exception {
        SetVariableAssertion setVariableAssertion = new SetVariableAssertion();
        final String decodedProperties = AssertionUtils.getBase64EncodedPropsDecoded(setVariableAssertion);
        assertTrue(decodedProperties.isEmpty());
    }

    @Test
    public void testNoDecodedMethodDecode() throws Exception {
        AssertionWithAnnotation assertion = new AssertionWithAnnotation();
        final String content = "This is the contents which should be found";
        assertion.setBase64Prop(HexUtils.encodeBase64(content.getBytes(Charsets.UTF8)));

        final String decodedProperties = AssertionUtils.getBase64EncodedPropsDecoded(assertion);
        assertEquals(content, decodedProperties);
    }

    @Test
    public void testTwoEncodedProperties() throws Exception {
        AssertionWithTwoProperties assertion = new AssertionWithTwoProperties();
        final String content1 = "This is the contents which should be found 1";
        assertion.setBase64Prop1(HexUtils.encodeBase64(content1.getBytes(Charsets.UTF8)));

        final String content2 = "This is the contents which should be found 2";
        assertion.setBase64Prop2(HexUtils.encodeBase64(content2.getBytes(Charsets.UTF8)));

        final String decodedProperties = AssertionUtils.getBase64EncodedPropsDecoded(assertion);
        assertTrue(decodedProperties.contains(content1));
        assertTrue(decodedProperties.contains(content2));
    }

    /**
     * This class has it's property annotated with @Base64 but does not specify a method to decode the property.
     */
    private static class AssertionWithAnnotation extends Assertion{

        @Base64Value
        public String getBase64Prop() {
            return base64Prop;
        }

        public void setBase64Prop(String base64Prop) {
            this.base64Prop = base64Prop;
        }

        private String base64Prop;
    }

    /**
     * This class has it's property annotated with @Base64 but does not specify a method to decode the property.
     */
    private static class AssertionWithTwoProperties extends Assertion{

        @Base64Value
        public String getBase64Prop1() {
            return base64Prop1;
        }

        public void setBase64Prop1(String base64Prop1) {
            this.base64Prop1 = base64Prop1;
        }

        @Base64Value(decodeMethodName = "base64Prop2")
        public String getBase64Prop2() {
            return base64Prop2;
        }

        public void setBase64Prop2(String base64Prop2) {
            this.base64Prop2 = base64Prop2;
        }

        public String base64Prop2() {
            if (base64Prop2 != null) {
                return new String(HexUtils.decodeBase64(base64Prop2));
            }

            return null;
        }

        private String base64Prop1;
        private String base64Prop2;
    }

}
