package com.l7tech.external.assertions.jsontransformation;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;

/**
 * User: wlui
 */
@Secured
public interface JsonTransformationAdmin {
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    public String testTransform(String input, JsonTransformationAssertion.Transformation transformation,
                                JsonTransformationAssertion.TransformationConvention convention,
                                String rootTag, boolean prettyPrint, boolean asArray, boolean useNumbers) throws JsonTransformationTestException;

    static public class JsonTransformationTestException extends Exception {

        public JsonTransformationTestException() {
            super();
        }

        public JsonTransformationTestException(String message) {
            super(message);
        }

        public JsonTransformationTestException(String message, Exception e) {
            super(message, e);
        }

    }
}
