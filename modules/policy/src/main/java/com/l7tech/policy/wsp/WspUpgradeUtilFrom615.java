package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.HttpRoutingAssertion;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set of classes that upgrade from 6.1.5 to newer. HttpRoutingTimeoutPropertyVisitor handles the upgrade
 * for the case where the timeout properties converts from string to int
 * User: wlui<br/>
 */
public class WspUpgradeUtilFrom615 {
    private static final Logger logger = Logger.getLogger(WspUpgradeUtilFrom615.class.getName());
    static class HttpRoutingTimeoutPropertyVisitor extends WspVisitorWrapper {

        public HttpRoutingTimeoutPropertyVisitor(WspVisitor originalVisitor) {
            super(originalVisitor);
        }


        @Override
        public void propertyTypeMismatch(Element originalObject, Element problematicParameter, Object deserializedObject, String parameterName, TypedReference parameterValue, Throwable problemEncountered)throws InvalidPolicyStreamException {
            HttpRoutingAssertion ass;
            if (deserializedObject instanceof HttpRoutingAssertion) {
                ass = (HttpRoutingAssertion)deserializedObject;
            } else {
                logger.warning("unexpected deserialized object " + deserializedObject.getClass().getName());
                return;
            }

            if ("ConnectionTimeout".equals(parameterName)) {
                logger.log(Level.INFO, "Mismatch parameter type encountered: ConnectionTimeout");
                if (parameterValue.type != String.class)
                        return;

                String timeout = (String)parameterValue.target;
                try{
                    ass.setConnectionTimeout(Integer.parseInt(timeout));

                }catch (NumberFormatException e){
                    ass.setConnectionTimeoutMs(timeout);
                }
            }
            else if ("Timeout".equals(parameterName)) {
                logger.log(Level.INFO, "Mismatch parameter type encountered: Timeout");
                if (parameterValue.type != String.class)
                    return;

                String timeout = (String)parameterValue.target;
                try{
                    ass.setTimeout(Integer.parseInt(timeout));

                }catch (NumberFormatException e){
                    ass.setTimeoutMs(timeout);
                }
            }
        }
    }
}
