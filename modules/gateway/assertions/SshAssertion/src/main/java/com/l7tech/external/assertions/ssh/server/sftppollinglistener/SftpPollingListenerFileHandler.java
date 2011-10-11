package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.ssh.server.MessageProcessingSshUtil;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SftpPollingListenerFileHandler is responsible for processing SFTP file using the message processor.
 */
public class SftpPollingListenerFileHandler {
    private static final Logger _logger = Logger.getLogger(SftpPollingListenerFileHandler.class.getName());

    private MessageProcessor messageProcessor;
    private StashManagerFactory stashManagerFactory;
    private final ApplicationEventPublisher messageProcessingEventChannel;
    private final ServerConfig serverConfig;

    public SftpPollingListenerFileHandler(ApplicationContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Spring Context is required");
        }
        messageProcessor = ctx.getBean("messageProcessor", MessageProcessor.class);
        serverConfig = ctx.getBean("serverConfig", ServerConfig.class);
        stashManagerFactory = ctx.getBean("stashManagerFactory", StashManagerFactory.class);
        messageProcessingEventChannel = ctx.getBean("messageProcessingEventChannel", EventChannel.class);
    }

    /**
     * Handle an incoming file.  Also takes care of sending the reply if appropriate.
     *
     * @param settings The SFTP listener configuration that this handler operates on
     * @param sftpClient The SFTP client connection
     * @param fileName The file to process
     * @throws SftpPollingListenerRuntimeException if an error occurs
     */
    public void onMessage( final SftpPollingListenerResource settings,
                           final ThreadSafeSftpClient sftpClient,
                           final String fileName) throws SftpPollingListenerRuntimeException {

        final ContentTypeHeader ctype;
        boolean fileTooLarge = false;
        final String directory = settings.getDirectory();
        final String processingFileName = fileName + SftpPollingListener.PROCESSING_FILE_EXTENSION;
        try {
            // get the content type
            ctype = ContentTypeHeader.parseValue(settings.getContentType());

            // enforce size restriction
            long size = sftpClient.getFilesize(processingFileName);
            int sizeLimit = serverConfig.getIntProperty(SftpPollingListenerConstants.SFTP_POLLING_MESSAGE_MAX_BYTES_PROPERTY, 5242880);
            if ( sizeLimit > 0 && size > sizeLimit ) {
                fileTooLarge = true;
            }
        } catch (IOException ioe) {
            throw new SftpPollingListenerRuntimeException("Error processing request message", ExceptionUtils.getDebugException(ioe));
        }

        PolicyEnforcementContext context = null;
        String faultMessage = null;
        String faultCode = null;
        try {
            PipedInputStream pis = new PipedInputStream();
            PipedOutputStream pos = new PipedOutputStream(pis);
            Message request = new Message();
            request.initialize(stashManagerFactory.createStashManager(), ctype, pis);

            request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob("localhost", -1, settings.getHostname(),
                    settings.getPort(), processingFileName, directory, null, null));

            if (settings.isHardwiredService()) {
                request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(settings.getHardwiredServiceId()));
            }

            final boolean replyExpected = settings.isEnableResponses();
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, replyExpected);
            boolean stealthMode = false;
            InputStream responseStream = null;
            AssertionStatus status = AssertionStatus.UNDEFINED;
            if ( !fileTooLarge ) {
                try {
                    // download file on a new thread
                    Thread thread = sshDownloadOnNewThread(sftpClient, directory, processingFileName, pos, _logger);

                    status = messageProcessor.processMessage(context);

                    context.setPolicyResult(status);
                    _logger.finest("Policy resulted in status " + status);

                    Message contextResponse = context.getResponse();
                    if (contextResponse.getKnob(XmlKnob.class) != null || contextResponse.getKnob(MimeKnob.class) != null) {
                        // if the policy is not successful AND the stealth flag is on, drop connection
                        if (status != AssertionStatus.NONE && context.isStealthResponseMode()) {
                            _logger.info("Policy returned error and stealth mode is set. " +
                                    "Not sending response message.");
                            stealthMode = true;
                        } else {
                            // add more detailed diagnosis message
                            if (!contextResponse.isXml()) {
                                responseStream = contextResponse.getMimeKnob().getEntireMessageBodyAsInputStream();
                            } else {
                                responseStream = new ByteArrayInputStream(XmlUtil.nodeToString(
                                        contextResponse.getXmlKnob().getDocumentReadOnly()).getBytes());
                            }
                        }
                    } else {
                        _logger.finer("No response received");
                        responseStream = null;

                        // make sure to close input pipe if there's no response from the Gateway
                        // e.g. invalid path causing service not found status
                        pis.close();
                    }

                    _logger.log(Level.FINE, "Waiting for read thread join().");
                    int waitSeconds = serverConfig.getIntProperty(SftpPollingListenerConstants.SFTP_POLLING_DOWNLOAD_THREAD_WAIT_SECONDS_PROPERTY, 3);
                    thread.join(waitSeconds * 1000L);
                    _logger.log(Level.FINE, "Done read thread join().");
                } catch ( PolicyVersionException pve ) {
                    String msg1 = "Request referred to an outdated version of policy";
                    _logger.log( Level.INFO, msg1 );
                    faultMessage = msg1;
                    faultCode = SoapUtil.FC_CLIENT;
                } catch ( Throwable t ) {
                    _logger.warning("Exception while processing file via SFTP: " + ExceptionUtils.getMessage(t));
                    faultMessage = t.getMessage();
                    if ( faultMessage == null ) faultMessage = t.toString();
                } finally {
                    try {
                        if(settings.isDeleteOnReceive()) {
                            sftpClient.deleteFile(processingFileName);
                        } else {
                            sftpClient.renameFile(processingFileName, fileName + SftpPollingListener.PROCESSED_FILE_EXTENSION);
                        }
                    } catch (IOException ioe) {
                        _logger.log( Level.SEVERE, "Could not delete or rename file.  Error: " + ExceptionUtils.getDebugException(ioe) );
                    }
                }
            } else {
                String msg1 = "File too large";
                _logger.log( Level.INFO, msg1 );
                faultMessage = msg1;
                faultCode = SoapUtil.FC_CLIENT;
            }

            if ( responseStream == null ) {
                if (context.isStealthResponseMode()) {
                    _logger.info("No response data available and stealth mode is set. " + "Not sending response message.");
                    stealthMode = true;
                } else {
                    if ( faultMessage == null ) {
                        faultMessage = status.getMessage();
                    }
                    try {
                        String faultXml = SoapFaultUtils.generateSoapFaultXml(
                                (context.getService() != null) ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN,
                                faultCode == null ? SoapUtil.FC_SERVER : faultCode,
                                faultMessage, null, "");

                        responseStream = new ByteArrayInputStream(faultXml.getBytes(Charsets.UTF8));

                        if (faultXml != null) {
                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        }
                    } catch (SAXException e) {
                        throw new SftpPollingListenerRuntimeException(e);
                    }
                }
            }

            if (!stealthMode && settings.isEnableResponses()) {
                PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
                final byte[] responseBytes;
                try {
                    IOUtils.copyStream(responseStream, baos);
                    responseBytes = baos.toByteArray();
                } finally {
                    baos.close();
                }

                long startResp = System.currentTimeMillis();
                sendResponse( responseBytes, sftpClient, directory, fileName );
                _logger.log(Level.INFO, "Send response took {0} millis; listener {1}", new Object[] {
                        (System.currentTimeMillis() - startResp), settings.getName()});
            }
        } catch (IOException e) {
            throw new SftpPollingListenerRuntimeException(e);
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    /*
     * Download the given file on a new thread.
     */
    private static Thread sshDownloadOnNewThread(final ThreadSafeSftpClient sftpClient, final String directory,
                                                 final String fileName, final PipedOutputStream pos, final Logger logger) throws IOException {
        final CountDownLatch startedSignal = new CountDownLatch(1);
        logger.log(Level.FINE, "Start new thread for downloading ...");
        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {
                    startedSignal.countDown();
                    sftpClient.setDir(directory);
                    sftpClient.download(pos, fileName);
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
                finally {
                    logger.log(Level.FINE, "... downloading thread stopped.");
                    try {
                        pos.flush();
                        pos.close();
                    } catch(IOException ioe) {
                        logger.log(Level.SEVERE, ExceptionUtils.getMessage(ioe), ExceptionUtils.getDebugException(ioe));
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
            logger.log(Level.SEVERE, ExceptionUtils.getMessage(ie), ExceptionUtils.getDebugException(ie));
        }

        return thread;
    }

    private void sendResponse(byte[] responseMsg, ThreadSafeSftpClient client, String directory, String filename) {
        try {
            client.setDir(directory);
            client.upload(responseMsg, filename + SftpPollingListener.RESPONSE_FILE_EXTENSION);
        } catch ( IOException e ) {
            _logger.log( Level.WARNING, "Caught IOException while sending response", ExceptionUtils.getDebugException(e) );
        }
    }
}
