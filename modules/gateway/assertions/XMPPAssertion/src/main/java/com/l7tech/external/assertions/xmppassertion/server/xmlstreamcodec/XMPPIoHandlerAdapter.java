package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 18/06/12
 * Time: 1:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPIoHandlerAdapter extends IoHandlerAdapter {
    private Object targetObject;

    private Method messageReceivedMethod;
    private Method sessionOpenedMethod;
    private Method sessionClosedMethod;

    public XMPPIoHandlerAdapter(Object targetObject) {
        this.targetObject = targetObject;

        try {
            messageReceivedMethod = targetObject.getClass().getMethod("messageReceived", Object.class, Object.class);
            sessionOpenedMethod = targetObject.getClass().getMethod("sessionOpened", Object.class);
            sessionClosedMethod = targetObject.getClass().getMethod("sessionClosed", Object.class);
        } catch(NoSuchMethodException e) {
            // Ignore
        }
    }

    @Override
    public void messageReceived(final IoSession session, Object message) throws Exception {
        try {
            messageReceivedMethod.invoke(targetObject, session, message);
        } catch(IllegalAccessException e) {
            // Ignore
        } catch(InvocationTargetException e) {
            // Ignore
        }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        try {
            sessionOpenedMethod.invoke(targetObject, session);
        } catch(IllegalAccessException e) {
            // Ignore
        } catch(InvocationTargetException e) {
            // Ignore
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        try {
            sessionClosedMethod.invoke(targetObject, session);
        } catch(IllegalAccessException e) {
            // Ignore
        } catch(InvocationTargetException e) {
            // Ignore
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        String a = "5";
    }
}
