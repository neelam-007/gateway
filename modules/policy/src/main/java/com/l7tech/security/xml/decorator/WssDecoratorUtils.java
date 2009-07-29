package com.l7tech.security.xml.decorator;

import com.l7tech.message.SecurityKnob;

import java.util.*;

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

        // gather the relevant decoration results: added signatures, encrypted signatures
        Map<String, Boolean> signatures = new HashMap<String, Boolean>();
        Set<String> encrypted = new HashSet<String>();
        for (WssDecorator.DecorationResult decorationResult :  secKnob.getDecorationResults(actor)) {
            signatures.putAll(decorationResult.getSignatures());
            encrypted.addAll(decorationResult.getEncryptedSignatureValues());
        }

        // update the 'encrypted' flag for each signature, across all decoration results
        for(String signature : signatures.keySet()) {
            if (encrypted.contains(signature))
                signatures.put(signature, true);
        }

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
