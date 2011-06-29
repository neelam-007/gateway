package com.l7tech.external.assertions.api3scale;

import java.util.Map;

/**
 * User: wlui
 */
public interface Api3ScaleAdmin {
    public String testAuthorize(Api3ScaleAuthorizeAssertion ass, Map<String, Object> contextVars ) throws Api3ScaleTestException;

    static public class Api3ScaleTestException extends Exception {

        public Api3ScaleTestException() {
            super();
        }

        public Api3ScaleTestException(String message) {
            super(message);
        }

        public Api3ScaleTestException(String message, Exception e) {
            super(message, e);
        }

    }
}

