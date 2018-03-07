package com.l7tech.server.transport.jms2;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NonCachedSessionHolder extends SessionHolderBase{
    private static final Logger logger = Logger.getLogger(NonCachedSessionHolder.class.getName());


    protected NonCachedSessionHolder(final JmsEndpointConfig cfg,
                                     final JmsBag bag) {
        super(cfg, bag);
    }

    @Override
    public JmsBag borrowJmsBag() throws JmsRuntimeException, NamingException {
        touch();
        try {
            logger.log(Level.FINE, "Session pool is 0. Creating new JMS Session.");
            return makeJmsBag();
        } catch (JMSException e) {
            throw new JmsRuntimeException(e);
        } catch (NamingException e) {
            throw e;
        } catch (JmsConfigException e) {
            throw new JmsRuntimeException(e);
        } catch (Exception e) {
            throw new JmsRuntimeException(e);
        }
    }

    @Override
    public void returnJmsBag(JmsBag jmsBag) {
        logger.log(Level.FINE, "Session pool is 0. Closing JMS Session.");
        jmsBag.closeSession();
    }

    @Override
    public void close() {
        logger.log(
                Level.FINE,
                "Closing JMS connection ({0}), version {1}:{2}",
                new Object[]{
                        name, connectionVersion, endpointVersion
                });
        bag.close();
        //reset referenceCount
        referenceCount.set(0);
    }
}
