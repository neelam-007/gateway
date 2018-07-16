package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 10/01/14
 * Time: 10:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class OutboundIoHandlerAdapter extends IoHandlerAdapter {
    private static final Logger logger = Logger.getLogger(OutboundIoHandlerAdapter.class.getName());

    private byte[] response;

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.log(Level.WARNING, "Error communicating with server: " + cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        response = (byte[]) message;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        //remove previous response, as we are initiating a new transaction
        response = null;
    }

    public byte[] getResponse() {
        return response;
    }
}
