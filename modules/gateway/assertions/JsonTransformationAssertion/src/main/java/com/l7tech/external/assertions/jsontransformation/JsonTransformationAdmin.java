package com.l7tech.external.assertions.jsontransformation;

/**
 * User: wlui
 */
public interface JsonTransformationAdmin {
    public String testTransform(String input, JsonTransformationAssertion.Transformation transformation,
                                JsonTransformationAssertion.TransformationConvention convention,
                                String rootTag, boolean prettyPrint, boolean asArray) throws JsonTransformationTestException;

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
