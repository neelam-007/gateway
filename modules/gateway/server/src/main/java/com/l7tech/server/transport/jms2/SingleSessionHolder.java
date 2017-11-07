package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleSessionHolder extends SessionHolderBase {

    private static final Logger logger = Logger.getLogger(SingleSessionHolder.class.getName());

    private final JmsBag singleSession;

    public SingleSessionHolder(final JmsEndpointConfig cfg,
                               final JmsBag bag) throws  Exception{
        super(cfg, bag);
        singleSession = makeJmsBag();
    }

    @Override
    public JmsBag borrowJmsBag() throws JmsRuntimeException, NamingException {
        logger.log(Level.FINEST, "Borrowed session " + singleSession);
        return singleSession;
    }

    @Override
    public void returnJmsBag(JmsBag jmsBag) {
        //nothing to do
    }

    @Override
    public void close() {
        logger.log(
                Level.FINE,
                "Closing JMS connection ({0}), version {1}:{2}",
                new Object[]{
                        name, connectionVersion, endpointVersion
                });
        singleSession.close();
    }
}
