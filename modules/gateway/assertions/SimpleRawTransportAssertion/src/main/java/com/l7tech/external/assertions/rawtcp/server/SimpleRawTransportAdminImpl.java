package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAdmin;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.message.Message;
import com.l7tech.util.ConfigFactory;

/**
 * User: wlui
 */
public class SimpleRawTransportAdminImpl implements SimpleRawTransportAdmin{
    @Override
     public long getDefaultResponseSizeLimit(){
        return ConfigFactory.getLongProperty("com.l7tech.external.assertions.rawtcp.defaultResponseSizeLimit", Message.getMaxBytes());
    }
}

