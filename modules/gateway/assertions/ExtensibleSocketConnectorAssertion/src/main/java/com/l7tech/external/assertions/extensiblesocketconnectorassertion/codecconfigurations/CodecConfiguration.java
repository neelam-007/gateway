package com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations;

import com.l7tech.util.XmlSafe;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/12/11
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public interface CodecConfiguration {

    boolean requiresListenerRestart(CodecConfiguration newConfig);

    boolean isInbound();

    void setInbound(boolean value);
}
