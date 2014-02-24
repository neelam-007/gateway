package com.l7tech.server.transport.ftp;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import org.apache.ftpserver.ConnectionConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.*;

/**
  * @author Jamie Williams - jamie.williams2@ca.com
 */
public class FtpRequestProcessorFactory {

    @Autowired
    private MessageProcessor messageProcessor;

    @Autowired
    private SoapFaultManager soapFaultManager;

    @Autowired
    private StashManagerFactory stashManagerFactory;

    @Autowired
    private EventChannel messageProcessingEventChannel;

    /**
     * Creates a new FtpRequestProcessor for the specified SsgConnector.
     *
     * @return the new FtpRequestProcessor
     * @throws com.l7tech.server.transport.ListenerException on invalid overridden content type specified in SsgConnector
     */
    public FtpRequestProcessor create(SsgConnector connector, ConnectionConfig connectionConfig) throws ListenerException {
        Goid hardwiredServiceGoid = connector.getGoidProperty(EntityType.SERVICE,
                SsgConnector.PROP_HARDWIRED_SERVICE_ID, PersistentEntity.DEFAULT_GOID);

        String overrideContentTypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        ContentTypeHeader overrideContentType = null;

        try {
            if (overrideContentTypeStr != null)
                overrideContentType = ContentTypeHeader.parseValue(overrideContentTypeStr);
        } catch (IOException e) {
            throw new ListenerException("Unable to start FTP listener: Invalid overridden content type: " +
                    overrideContentTypeStr);
        }

        // this executor handles upload tasks, with a maximum pool size equal to the maximum number of connections
        ExecutorService transferTaskExecutor = new ThreadPoolExecutor(0, connectionConfig.getMaxThreads(),
                5L * 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(connectionConfig.getMaxThreads()),
                new ThreadPoolExecutor.AbortPolicy());

        return new FtpRequestProcessor(
                messageProcessor,
                soapFaultManager,
                stashManagerFactory,
                messageProcessingEventChannel,
                transferTaskExecutor,
                overrideContentType,
                hardwiredServiceGoid,
                connector.getGoid(),
                connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, -1L));
    }
}
