package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.HttpRoutingAssertion;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set of classes that upgrade from 3.6.5 to newer. HttpRoutingPropertyVisitor handles the upgrade
 * for the case where the now gone copyCookie was set to false, in that case, we initialize the http
 * rules so that cookies dont get forwarded.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 9, 2007<br/>
 */
public class WspUpgradeUtilFrom365 {
    private static final Logger logger = Logger.getLogger(WspUpgradeUtilFrom365.class.getName());
    static class HttpRoutingPropertyVisitor extends WspVisitorWrapper {

        public HttpRoutingPropertyVisitor(WspVisitor originalVisitor) {
            super(originalVisitor);
        }

        @Override
        public void unknownProperty(Element originalObject, Element problematicParameter, Object deserializedObject, String parameterName, TypedReference parameterValue, Throwable problemEncountered) throws InvalidPolicyStreamException {
            HttpRoutingAssertion ass;
            if (deserializedObject instanceof HttpRoutingAssertion) {
                ass = (HttpRoutingAssertion)deserializedObject;
            } else {
                logger.warning("unexpected deserialized object " + deserializedObject.getClass().getName());
                return;
            }
            if ("CopyCookies".equals(parameterName)) {
                logger.log(Level.INFO, "Deprecated parameter encountered: CopyCookies");
                Boolean cookieson = (Boolean)parameterValue.target;
                if (!cookieson.booleanValue()) { // my compiler doesn't like the alternative for some reason
                    ass.getRequestHeaderRules().remove("cookie");
                    ass.getResponseHeaderRules().remove("set-cookie");
                }
            }
        }
    }
}
