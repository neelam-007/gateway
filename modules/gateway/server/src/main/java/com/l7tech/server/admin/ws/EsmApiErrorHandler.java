package com.l7tech.server.admin.ws;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

import com.l7tech.util.ExceptionUtils;

import javax.xml.namespace.QName;

/**
 * CXF Interceptor that handle runtime errors
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class EsmApiErrorHandler extends AbstractPhaseInterceptor<Message> {

    //- PUBLIC

    public EsmApiErrorHandler(){
        super(Phase.USER_LOGICAL);
    }

    @Override
    public void handleMessage( final Message message ) throws Fault {
        if ( message.get( FaultMode.class ) == FaultMode.UNCHECKED_APPLICATION_FAULT ) {

            // Replace the existing fault with a generic fault, log the original exception.
            final Fault originalFault = (Fault) message.getContent( Exception.class );
            final Fault replacementFault = new Fault(new org.apache.cxf.common.i18n.Message("server.error", new ListResourceBundle(){
                @Override
                protected Object[][] getContents() {
                    return new Object[][]{{"server.error", "Server Error"}};
                }
            }, Fault.FAULT_CODE_SERVER));
            message.setContent( Exception.class, replacementFault );

            final Message inMessage = message.getExchange().getInMessage();
            final QName interfaceName = (QName)inMessage.get("javax.xml.ws.wsdl.interface");
            final QName operationName = (QName)inMessage.get("javax.xml.ws.wsdl.operation");
            final String service = interfaceName==null ? "<Unknown>" : interfaceName.getLocalPart();
            final String operation = operationName==null ? "<Unknown>" : operationName.getLocalPart();
            logger.log( Level.WARNING, "Unexpected error while handling request for service '"+service+"', operation '"+operation+"', '"+ ExceptionUtils.getMessage(originalFault)+"'.", originalFault.getCause());
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EsmApiErrorHandler.class.getName());

}