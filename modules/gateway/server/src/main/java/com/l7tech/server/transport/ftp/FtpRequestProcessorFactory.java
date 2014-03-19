package com.l7tech.server.transport.ftp;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
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
        boolean supportExtendedCommands =
                connector.getBooleanProperty(SsgConnector.PROP_SUPPORT_EXTENDED_FTP_COMMANDS);

        Goid hardwiredServiceGoid =
                connector.getGoidProperty(EntityType.SERVICE, SsgConnector.PROP_HARDWIRED_SERVICE_ID, null);


        // to support proxying FTP requests for the extended command set the listener must be hardwired to a service
        if (null == hardwiredServiceGoid && supportExtendedCommands) {
            throw new ListenerException("Unable to start FTP listener: no service specified for listener.");
        }

        String overrideContentTypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        ContentTypeHeader overrideContentType = null;

        try {
            if (overrideContentTypeStr != null)
                overrideContentType = ContentTypeHeader.parseValue(overrideContentTypeStr);
        } catch (IOException e) {
            throw new ListenerException("Unable to start FTP listener: invalid overridden content type '" +
                    overrideContentTypeStr + "'.");
        }

        // this executor handles upload tasks, with a maximum pool size equal to the maximum number of connections
        ExecutorService transferTaskExecutor = new ThreadPoolExecutor(1, connectionConfig.getMaxThreads(),
                5L * 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(connectionConfig.getMaxThreads()),
                new ThreadPoolExecutor.AbortPolicy());

        long maxRequestSize = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, -1L);

        if (supportExtendedCommands) {
            return new ExtendedCommandsFtpRequestProcessor(
                    messageProcessor,
                    soapFaultManager,
                    stashManagerFactory,
                    messageProcessingEventChannel,
                    transferTaskExecutor,
                    overrideContentType,
                    hardwiredServiceGoid,
                    connector.getGoid(),
                    maxRequestSize);
        } else {
            return new UploadOnlyFtpRequestProcessor(
                    messageProcessor,
                    soapFaultManager,
                    stashManagerFactory,
                    messageProcessingEventChannel,
                    transferTaskExecutor,
                    overrideContentType,
                    hardwiredServiceGoid,
                    connector.getGoid(),
                    maxRequestSize);
        }
    }
}
