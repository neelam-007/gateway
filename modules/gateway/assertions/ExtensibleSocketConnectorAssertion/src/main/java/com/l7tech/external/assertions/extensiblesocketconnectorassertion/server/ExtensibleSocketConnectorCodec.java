package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import java.lang.reflect.InvocationTargetException;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 10/10/13
 * Time: 10:48 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExtensibleSocketConnectorCodec {
    public void configureCodec(Object codecConfig) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;
}
