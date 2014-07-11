package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 08/03/12
 * Time: 2:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPCodecConfiguration implements XMLStreamCodecConfiguration {
    private static String RESET_ELEMENT = "stream";
    private static String RESET_NAMESPACE = "http://etherx.jabber.org/streams";
    private static String START_TLS_NAMESPACE = "urn:ietf:params:xml:ns:xmpp-tls";
    private static String START_TLS_ELEMENT = "starttls";
    private static String START_TLS_RESPONSE = "proceed";
    
    private List<StanzaToProcessRule> rules;

    public XMPPCodecConfiguration() {
        rules = new ArrayList<StanzaToProcessRule>();
        rules.add(new StanzaToProcessDepthRule(
                DepthComparisonOperator.EQUAL,
                1,
                StanzaRuleApplicability.START
        ));
        rules.add(new StanzaToProcessDepthRule(
                DepthComparisonOperator.LESS_THAN,
                2,
                StanzaRuleApplicability.END
        ));
    }

    public String getResetElementNamespace() {
        return RESET_NAMESPACE;
    }

    public String getResetElement() {
        return RESET_ELEMENT;
    }

    public List<StanzaToProcessRule> getStanzaProcessingRules() {
        return rules;
    }

    public boolean isStartTLSSupport() {
        return true;
    }

    public String getStartTLSElementNamespace() {
        return START_TLS_NAMESPACE;
    }

    public boolean isStartTLSReplyUnencrypted() {
        return true;
    }
}
