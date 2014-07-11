package com.l7tech.external.assertions.xmppassertion.server;

/**
* Created with IntelliJ IDEA.
* User: njordan
* Date: 20/06/12
* Time: 11:11 AM
* To change this template use File | Settings | File Templates.
*/
public class XMPPClassHelperFactory {
    private static XMPPClassHelperFactory INSTANCE = new XMPPClassHelperFactory();

    public XMPPClassHelper createClassHelper() {
        return new XMPPClassHelperImpl();
    }

    public static XMPPClassHelperFactory getIntance() {
        return INSTANCE;
    }

    protected static void setInstance(XMPPClassHelperFactory factory) {
        INSTANCE = factory;
    }
}
