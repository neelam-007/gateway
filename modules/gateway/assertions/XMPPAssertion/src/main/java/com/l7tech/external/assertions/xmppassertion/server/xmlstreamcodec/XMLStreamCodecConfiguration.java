package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 08/03/12
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
 */
public interface XMLStreamCodecConfiguration {
    public String getResetElementNamespace();
    
    public String getResetElement();
    
    public List<StanzaToProcessRule> getStanzaProcessingRules();
}
