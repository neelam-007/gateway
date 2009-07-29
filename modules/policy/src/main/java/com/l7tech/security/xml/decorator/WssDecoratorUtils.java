package com.l7tech.security.xml.decorator;

import com.l7tech.message.SecurityKnob;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * @author jbufu
 */
public class WssDecoratorUtils {

    private WssDecoratorUtils() {}

    /**
     * Gets the signature decorations applied to a message for the specified actor.
     *
     * @return A map of signature values -> boolean map; the boolean is true if the signature was also encrypted
     * @see com.l7tech.security.xml.decorator.WssDecorator.DecorationResult#getSignatures()
     */
    public static Map<String, Boolean> getSignaturesDecorated(SecurityKnob secKnob, String actor) {
        if (secKnob == null || secKnob.getDecorationResults(actor) == null)
            return null;
        Map<String, Boolean> signatures = new HashMap<String, Boolean>();
        for (WssDecorator.DecorationResult decorationResult :  secKnob.getDecorationResults(actor)) {
            signatures.putAll(decorationResult.getSignatures());
        }
        // todo: check if signatures were encrypted by other decorations
        return signatures;
    }

    public static void promoteDecorationResults(SecurityKnob securityKnob, String oldActor, String newActor) {
        securityKnob.removeDecorationResults(newActor);
        List<WssDecorator.DecorationResult> decorationResults = securityKnob.getDecorationResults(oldActor);
        securityKnob.removeDecorationResults(oldActor);
        for (WssDecorator.DecorationResult dr : decorationResults) {
            dr.setSecurityHeaderActor(newActor);
            securityKnob.addDecorationResult(dr);
        }
        securityKnob.removeDecorationResults(oldActor);
    }
}
