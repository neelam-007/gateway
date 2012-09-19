package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.jscape.inet.sftp.SftpFile;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.ssh.server.MessageProcessingSshUtil;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.ActiveTransportModule;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Charsets;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ThreadPool.ThreadPoolShutDownException;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.ssh.SftpPollingListenerConstants.*;
import static com.l7tech.external.assertions.ssh.server.sftppollinglistener.SftpPollingListener.*;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_SSH_MESSAGE_INPUT;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.TextUtils.trim;

/**
 * SFTP polling listener module (aka boot process).
 */
public class SftpPollingListenerModule extends ActiveTransportModule implements ApplicationListener {
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(SftpPollingListenerModule.class.getName());

    private static final Set<String> SUPPORTED_TYPES = caseInsensitiveSet( ACTIVE_CONNECTOR_TYPE_SFTP );

    private final Map<Long, SftpPollingListener> activeListeners = new ConcurrentHashMap<Long, SftpPollingListener>();
    private ThreadPoolBean threadPoolBean;

    @Inject
    private GatewayState gatewayState;
    @Inject
    private SecurePasswordManager securePasswordManager;
    @Inject
    private MessageProcessor messageProcessor;
    @Inject
    private StashManagerFactory stashManagerFactory;
    @Inject
    private ApplicationEventPublisher messageProcessingEventChannel;
    @Inject
    private ServerConfig serverConfig;

    /**
     * Single constructor for module.
     */
    public SftpPollingListenerModule( @NotNull final ThreadPoolBean threadPoolBean ) {
        super("SFTP Polling Listener module", Component.GW_SFTP_POLL_RECV, logger, SERVICE_SSH_MESSAGE_INPUT);
        this.threadPoolBean = threadPoolBean;
    }

    @Override
    protected boolean isInitialized() {
        return !threadPoolBean.isShutdown();
    }

    /**
     * Starts {@link com.l7tech.external.assertions.ssh.server.sftppollinglistener.SftpPollingListener}s using
     * configuration in {@link com.l7tech.gateway.common.transport.SsgActiveConnector}.
     */
    @Override
    protected void doStart() throws LifecycleException {
        super.doStart();
        if (gatewayState.isReadyForMessages()) {
            try {
                threadPoolBean.start();
                startInitialListeners();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "Unable to access initial SFTP listener(s): " + getMessage( e ), getDebugException( e ));
            }
        }
    }

    /**
     * Starts all configured listeners.
     *
     * @throws com.l7tech.objectmodel.FindException when problems occur during subsystem startup
     */
    private void startInitialListeners() throws FindException {
        final boolean wasSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);
            final Collection<SsgActiveConnector> connectors = ssgActiveConnectorManager.findSsgActiveConnectorsByType( ACTIVE_CONNECTOR_TYPE_SFTP );
            for ( final SsgActiveConnector connector : connectors ) {
                if ( connector.isEnabled() && connectorIsOwnedByThisModule( connector ) ) {
                    try {
                        addConnector( connector.getReadOnlyCopy() );
                    } catch ( Exception e ) {
                        logger.log(Level.WARNING, "Unable to start polling SFTP connector " + connector.getName() +
                                        ": " + getMessage( e ), e);
                    }
                }
            }
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }
    
    /**
     * Attempts to stop all running SFTP polling listeners.
     */
    @Override
    protected void doStop() {
        for ( final SftpPollingListener listener : activeListeners.values() ) {
            logger.info("Stopping SFTP polling receiver '" + listener.getDisplayName() + "'");
            listener.stop();
        }
        for ( final SftpPollingListener listener : activeListeners.values() ) {
            logger.info("Waiting for SFTP polling receiver to stop '" + listener.getDisplayName() + "'");
            listener.ensureStopped();
        }

        activeListeners.clear();
        threadPoolBean.shutdown();
    }

    @Override
    protected void addConnector( @NotNull final SsgActiveConnector ssgActiveConnector ) throws ListenerException {
        SftpPollingListener newListener = null;
        try {
            newListener = new SftpPollingListener( ssgActiveConnector, getApplicationContext(), securePasswordManager ) {
                @Override
                void handleFile( final SftpFile file ) throws SftpPollingListenerException {
                    try {
                        final Future<SftpPollingListenerException> result = threadPoolBean.submitTask( new Callable<SftpPollingListenerException>(){
                            @Override
                            public SftpPollingListenerException call() {
                                try {
                                    handleFileForConnector( ssgActiveConnector, sftpClient, file );
                                } catch ( SftpPollingListenerException e ) {
                                    return e;
                                } catch ( Exception e ) {
                                    return new SftpPollingListenerException(e);
                                }
                                return null;
                            }
                        } );
                        final SftpPollingListenerException exception = result.get();
                        if ( exception != null ) {
                            throw exception;
                        }
                    } catch ( InterruptedException e ) {
                        Thread.currentThread().interrupt();
                    } catch ( ThreadPoolShutDownException e ) {
                        logger.log( Level.WARNING,
                                "Error handling file, thread pool is shutdown.",
                                getDebugException( e ) );
                    } catch ( ExecutionException e ) {
                        logger.log( Level.WARNING,
                                "Error handling file: " + getMessage( e ),
                                getDebugException( e ) );
                    }
                }
            };
            newListener.setErrorSleepTime(serverConfig.getProperty(SFTP_POLLING_CONNECT_ERROR_SLEEP_PROPERTY));
            newListener.setIgnoredFileExtensionList(
                    map(list(serverConfig.getProperty(SFTP_POLLING_IGNORED_FILE_EXTENSION_LIST_PROPERTY).split("\\s*,\\s*")), trim()));
            newListener.start();
            activeListeners.put( ssgActiveConnector.getOid(), newListener );
        } catch (LifecycleException e) {
            logger.log( Level.WARNING,
                    "Exception while initializing polling listener " + newListener.getDisplayName() + ": " + getMessage( e ),
                    getDebugException( e ) );
        } catch ( SftpPollingListenerConfigException e ) {
            logger.log( Level.WARNING,
                    "Exception while initializing polling listener " + ssgActiveConnector.getName() + ": " + getMessage( e ),
                    getDebugException( e ) );
        }
    }

    @Override
    protected void removeConnector( long oid ) {
        final SftpPollingListener listener = activeListeners.remove( oid );
        if  ( listener != null  ) {
            listener.stop();
        }
    }

    @Override
    protected Set<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    /**
     * Handle an incoming file.  Also takes care of sending the reply if appropriate.
     *
     * @param connector The SFTP listener configuration that this handler operates on
     * @param sftpClient The SFTP client connection
     * @param file The file to process
     * @throws SftpPollingListenerException if an error occurs
     */
    public void handleFileForConnector( final SsgActiveConnector connector,
                                        final SftpClient sftpClient,
                                        final SftpFile file ) throws SftpPollingListenerException {
        final ContentTypeHeader ctype;
        boolean fileTooLarge = false;
        final String directory = connector.getProperty( PROPERTIES_KEY_SFTP_DIRECTORY );
        final String processingFileName = file.getFilename() + PROCESSING_FILE_EXTENSION;
        try {
            // get the content type
            ctype = ContentTypeHeader.parseValue( connector.getProperty( PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE ) );

            // enforce size restriction
            final long size = sftpClient.getFilesize(processingFileName);
            int sizeLimit = serverConfig.getIntProperty( SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY, 5242880);
            if ( sizeLimit > 0 && size > (long) sizeLimit ) {
                fileTooLarge = true;
            }
        } catch (IOException ioe) {
            throw new SftpPollingListenerException("Error processing request message.  " + getMessage( ioe ), ioe);
        }

        final boolean replyExpected = connector.getBooleanProperty( PROPERTIES_KEY_ENABLE_RESPONSE_MESSAGES );

        PolicyEnforcementContext context = null;
        String faultMessage = null;
        String faultCode = null;
        try {
            PipedInputStream pis = new PipedInputStream();
            PipedOutputStream pos = new PipedOutputStream(pis);
            Message request = new Message();
            final long requestSizeLimit = connector.getLongProperty( PROPERTIES_KEY_REQUEST_SIZE_LIMIT, getMaxBytes() );
            request.initialize(stashManagerFactory.createStashManager(), ctype, pis, requestSizeLimit);

            SshKnob.FileMetadata metadata = new SshKnob.FileMetadata(file.getAccessTime() * 1000,
                                                                     file.getModificationTime() * 1000,
                                                                     file.getPermissions().intValue());
            request.attachKnob(MessageProcessingSshUtil.buildSshKnob( null, 0, null,
                    0, processingFileName, directory, null, null, metadata ), SshKnob.class, UriKnob.class); // Avoid advertising TcpKnob since we don't have actual transport-level data for it

            final Long hardwiredServiceOid = connector.getHardwiredServiceOid();
            if ( hardwiredServiceOid != null ) {
                request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
            }

            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, null, replyExpected );
            boolean stealthMode = false;
            InputStream responseStream = null;
            AssertionStatus status = AssertionStatus.UNDEFINED;
            if ( !fileTooLarge ) {
                try {
                    // download file on a new thread
                    Thread thread = sshDownloadOnNewThread(sftpClient, directory, processingFileName, pos, logger);

                    status = messageProcessor.processMessage(context);

                    context.setPolicyResult(status);
                    logger.finest("Policy resulted in status " + status);

                    Message contextResponse = context.getResponse();
                    if (contextResponse.getKnob(XmlKnob.class) != null || contextResponse.getKnob(MimeKnob.class) != null) {
                        // if the policy is not successful AND the stealth flag is on, drop connection
                        if (status != AssertionStatus.NONE && context.isStealthResponseMode()) {
                            logger.info("Policy returned error and stealth mode is set. " +
                                    "Not sending response message.");
                            stealthMode = true;
                        } else {
                            // add more detailed diagnosis message
                            if (!contextResponse.isXml()) {
                                responseStream = contextResponse.getMimeKnob().getEntireMessageBodyAsInputStream();
                            } else {
                                responseStream = new ByteArrayInputStream( XmlUtil.nodeToString(
                                        contextResponse.getXmlKnob().getDocumentReadOnly() ).getBytes());
                            }
                        }
                    } else {
                        logger.finer("No response received");
                        responseStream = null;

                        // make sure to close input pipe if there's no response from the Gateway
                        // e.g. invalid path causing service not found status
                        pis.close();
                    }

                    logger.log(Level.FINE, "Waiting for read thread join().");
                    int waitSeconds = serverConfig.getIntProperty( SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_PROPERTY, 3);
                    thread.join( (long) waitSeconds * 1000L);
                    logger.log(Level.FINE, "Done read thread join().");
                } catch ( PolicyVersionException pve ) {
                    String msg1 = "Request referred to an outdated version of policy";
                    logger.log( Level.INFO, msg1 );
                    faultMessage = msg1;
                    faultCode = SoapUtil.FC_CLIENT;
                } catch ( Throwable t ) {
                    logger.warning("Exception while processing file via SFTP: " + getMessage( t ));
                    faultMessage = t.getMessage();
                    if ( faultMessage == null ) faultMessage = t.toString();
                } finally {
                    try {
                        if( connector.getBooleanProperty( PROPERTIES_KEY_SFTP_DELETE_ON_RECEIVE )) {
                            sftpClient.deleteFile(processingFileName);
                        } else {
                            sftpClient.renameFile(processingFileName, file.getFilename() + PROCESSED_FILE_EXTENSION);
                        }
                    } catch (IOException ioe) {
                        logger.log( Level.SEVERE, "Could not delete or rename file.  Error: " + getDebugException( ioe ) );
                    }
                }
            } else {
                String msg1 = "File too large";
                logger.log( Level.INFO, msg1 );
                faultMessage = msg1;
                faultCode = SoapUtil.FC_CLIENT;
            }

            if ( responseStream == null ) {
                if (context.isStealthResponseMode()) {
                    logger.info("No response data available and stealth mode is set. " + "Not sending response message.");
                    stealthMode = true;
                } else {
                    if ( faultMessage == null ) {
                        faultMessage = status.getMessage();
                    }
                    try {
                        String faultXml = SoapFaultUtils.generateSoapFaultXml(
                                (context.getService() != null) ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN,
                                faultCode == null ? SoapUtil.FC_SERVER : faultCode,
                                faultMessage, null, "" );

                        responseStream = new ByteArrayInputStream(faultXml.getBytes( Charsets.UTF8));

                        if (faultXml != null) {
                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        }
                    } catch (SAXException e) {
                        throw new SftpPollingListenerException(e);
                    }
                }
            }

            if (!stealthMode && replyExpected) {
                long startResp = System.currentTimeMillis();
                sendResponse( responseStream, sftpClient, directory, file );
                logger.log(Level.INFO, "Send response took {0} millis; {1}; {2}", new Object[] {
                        (System.currentTimeMillis() - startResp), connector.getName(), file.getFilename() + RESPONSE_FILE_EXTENSION});
            }
        } catch (IOException e) {
            throw new SftpPollingListenerException(e);
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    /*
     * Download the given file on a new thread.
     */
    private static Thread sshDownloadOnNewThread(final SftpClient sftpClient, final String directory,
                                                 final String fileName, final PipedOutputStream pos, final Logger logger) throws IOException {
        final CountDownLatch startedSignal = new CountDownLatch(1);
        logger.log(Level.FINE, "Start new thread for downloading ...");
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    startedSignal.countDown();
                    sftpClient.download(pos, directory, fileName);
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE, getMessage( e ), getDebugException( e ));
                }
                finally {
                    logger.log(Level.FINE, "... downloading thread stopped.");
                    try {
                        pos.flush();
                        pos.close();
                    } catch(IOException ioe) {
                        logger.log(Level.SEVERE, getMessage( ioe ), getDebugException( ioe ));
                    }
                    startedSignal.countDown();
                }
            }
        }, "SshDownloadThread-" + System.currentTimeMillis());

        thread.setDaemon(true);
        thread.start();

        try {
            startedSignal.await();
        }
        catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, getMessage( ie ), getDebugException( ie ));
        }

        return thread;
    }

    private void sendResponse( final InputStream responseIn, SftpClient client, String directory, SftpFile file) {
        try {
            client.upload(responseIn, directory, file.getFilename() + RESPONSE_FILE_EXTENSION);
        } catch ( IOException e ) {
            logger.log( Level.WARNING, "Caught IOException while sending response", getDebugException( e ) );
        }
    }
}