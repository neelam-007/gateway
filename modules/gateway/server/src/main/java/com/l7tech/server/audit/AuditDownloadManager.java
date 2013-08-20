package com.l7tech.server.audit;

import com.l7tech.objectmodel.Goid;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.OpaqueId;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.*;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class AuditDownloadManager implements ApplicationContextAware  {

    //- PUBLIC

    public AuditDownloadManager( final DefaultKey defaultKey ) {
        this.defaultKey = defaultKey;    
    }

    public OpaqueId createDownloadContext( final long fromTime,
                                           final long toTime,
                                           final Goid[] serviceOids ) throws IOException {
        X509Certificate signingCert;
        PrivateKey signingKey;

        SignerInfo sslKey = defaultKey==null ?  null : defaultKey.getSslInfo();
        if (sslKey != null) {
            signingCert = sslKey.getCertificateChain()[0];
            try {
                signingKey = sslKey.getPrivate();
            } catch (UnrecoverableKeyException e) {
                throw new RuntimeException("Unable to access default SSL key for signing audit download: " + ExceptionUtils.getMessage(e), e);
            }
        } else {
            throw new RuntimeException("Unable to sign exported audits: no default SSL key is currently designated");
        }
            
        final AuditExporter exporter = applicationContext.getBean("auditExporter", AuditExporter.class);
        final DownloadContext downloadContext = new DownloadContext(fromTime, toTime, serviceOids, 0, exporter , signingCert, signingKey);
        downloadContext.checkForException();

        downloadContexts.put( downloadContext.getOpaqueId(), downloadContext );

        return downloadContext.getOpaqueId();        
    }

    public void closeDownloadContext( final OpaqueId context ) {
        DownloadContext downloadContext = downloadContexts.get(context);
        if ( downloadContext != null ) {
            downloadContext.close();
        }        
    }

    public InputStream getDownloadInputStream( final OpaqueId context ) {
        DownloadContext downloadContext = downloadContexts.get(context);
        if (downloadContext == null)
            throw new RuntimeException("No such download context is pending");

        downloadContext.checkForException();

        return downloadContext.getInputStream();
    }

    public long getEstimatedTotalAudits( final OpaqueId context ) {
        DownloadContext downloadContext = downloadContexts.get(context);
        if (downloadContext == null)
            throw new RuntimeException("No such download context is pending");

        return downloadContext.getApproxTotalAudits();
    }

    public long getDownloadedAuditCount( final OpaqueId context ) {
        DownloadContext downloadContext = downloadContexts.get(context);
        if (downloadContext == null)
            throw new RuntimeException("No such download context is pending");

        return downloadContext.getAuditsDownloaded();            
    }

    public byte[] nextDownloadChunk( final OpaqueId context ) {
        DownloadContext downloadContext = downloadContexts.get(context);
        if (downloadContext == null)
            throw new RuntimeException("No such download context is pending");

        downloadContext.checkForException();
        byte[] chunk = downloadContext.nextChunk();
        if ( chunk == null )
            downloadContext.close();

        downloadContext.checkForException();

        return chunk;        
    }



    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AuditDownloadManager.class.getName());
    
    private static final long CONTEXT_TIMEOUT = 1000L * 90; // expire after 1 1/2 min of inactivity
    private static final int DEFAULT_DOWNLOAD_CHUNK_LENGTH = 8192;
    private static Map<OpaqueId, DownloadContext> downloadContexts = Collections.synchronizedMap(new HashMap<OpaqueId, DownloadContext>());
    private static final long MAX_CHUNK_WAIT = 10000; // Spend no more than ten seconds reading the next chunk
    private static final long CHUNK_SPIN_WAIT = 250;  // Check four times a second to see if we have anything to return yet

    private ApplicationContext applicationContext;
    private final DefaultKey defaultKey;    

    private static boolean downloadReaperScheduled = false;
    private static final TimerTask downloadReaperTask = new TimerTask() {
        @SuppressWarnings({"SynchronizeOnNonFinalField"})
        public void run() {
            Collection<DownloadContext> contexts;
            synchronized (downloadContexts) {
                contexts = new ArrayList<DownloadContext>(downloadContexts.values());
            }
            long now = System.currentTimeMillis();
            for (DownloadContext downloadContext : contexts) {
                if (now - downloadContext.getLastUsed() > CONTEXT_TIMEOUT) {
                    logger.log(Level.WARNING, "Closing stale audit download context " + downloadContext);
                    downloadContext.close(); // will remove itself from the master set
                }
            }
        }
    };

    private void scheduleBackgroundCleanupIfRequired() {
        synchronized( downloadReaperTask ) {
            if (!downloadReaperScheduled) {
                downloadReaperScheduled = true;
                Background.scheduleRepeated(downloadReaperTask, 1011L, 1011L);
            }
        }
    }

    /**
     * Set the ApplicationContext that this object runs in.
     */
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
        scheduleBackgroundCleanupIfRequired();
    }

    private static class DownloadContext {
        private final OpaqueId opaqueId = new OpaqueId();
        private final X509Certificate sslCert;
        private final PrivateKey sslPrivateKey;
        private final PipedOutputStream pos = new PipedOutputStream();
        private final PipedInputStream pis = new PipedInputStream(pos);
        private final Timer timer = new Timer("DownloadContextTimer", true);
        private final long fromTime;
        private final long toTime;
        private final Goid[] serviceOids;
        private final int chunkLength;
        private final Thread producerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    auditExporter.exportAuditsAsZipFile(fromTime,
                                                        toTime,
                                                        serviceOids,
                                                        pos,
                                                        sslCert,
                                                        sslPrivateKey);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Audit download producer error", e);
                    // avoid overwriting a real exception
                    if (getProducerException() == null)
                        setProducerException(e);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Audit download producer error", t);
                    setProducerException(t);
                }
            }
        });

        private final AuditExporter auditExporter;
        private volatile Throwable producerException = null;
        private long lastUsed = System.currentTimeMillis();

        private DownloadContext(long fromTime,
                                long toTime,
                                Goid[] serviceOids,
                                int chunkLength,
                                AuditExporter exporter,
                                X509Certificate sslCert,
                                PrivateKey sslPrivateKey) throws IOException {
            if (chunkLength < 1) chunkLength = DEFAULT_DOWNLOAD_CHUNK_LENGTH;
            this.fromTime = fromTime;
            this.toTime = toTime;
            this.serviceOids = serviceOids;
            this.chunkLength = chunkLength;
            this.sslCert = sslCert;
            this.sslPrivateKey = sslPrivateKey;
            this.auditExporter = exporter;
            producerThread.start();
            logger.info("Created audit download context " + this);
        }

        synchronized OpaqueId getOpaqueId() {
            return opaqueId;
        }

        synchronized long getLastUsed() {
            return lastUsed;
        }

        synchronized InputStream getInputStream() {
            return pis;
        }

        private synchronized void setLastUsed() {
            lastUsed = System.currentTimeMillis();
        }

        private class TimeoutReadResult {
            final int bytesRead; // always nonnegative
            final boolean eof;
            private TimeoutReadResult(int bytesRead, boolean eof) { this.bytesRead = bytesRead; this.eof = eof; }
        }

        // Spend no more than timeoutMillis attempting to read up to a full buffer from the specified PipedInputStream.
        // It must be a PipedInputStream to guarantee that its read method will throw InterruptedIOException.
        private TimeoutReadResult readWithTimeout(PipedInputStream pis, byte[] buff, long timeoutMillis) throws IOException {
            int amountRead = 0;
            int roomLeft = buff.length;

            // Schedule an interrupt before we read, and safely cancel it afterward
            final Thread readerThread = Thread.currentThread();
            final boolean[] wantInterrupt = { true };
            synchronized (DownloadContext.this) { wantInterrupt[0] = true; }
            TimerTask interruptMe = new TimerTask() {
                public void run() {
                    synchronized (DownloadContext.this) {
                        if (!wantInterrupt[0]) {
                            cancel();
                            return;
                        }
                        wantInterrupt[0] = false;
                        readerThread.interrupt();
                        cancel();
                    }
                }
            };

            try {
                timer.schedule(interruptMe, timeoutMillis);

                while (roomLeft > 0) {
                    try {
                        int got = pis.read(buff, amountRead, roomLeft);
                        if (got < 0)
                            return new TimeoutReadResult(amountRead, true);
                        if (got == 0) Thread.sleep(CHUNK_SPIN_WAIT); // (can't happen as of JDK 1.5 PipedInputStream)
                        amountRead += got;
                        roomLeft -= got;
                    } catch (InterruptedIOException e) {
                        // Read timed out.  Return whatever we've got so far (possibly nothing)
                        return new TimeoutReadResult(amountRead, false);
                    } catch (InterruptedException e) {
                        // Read timed out.  Return whatever we've got so far (possibly nothing)
                        return new TimeoutReadResult(amountRead, false);
                    }
                }

                return new TimeoutReadResult(amountRead, false);
            } finally {
                // Ensure the interrupt is safely canceled and cleared
                synchronized (DownloadContext.this) {
                    wantInterrupt[0] = false;
                    interruptMe.cancel();
                    Thread.interrupted();
                }
            }
        }

        private final Object chunkSerializer = new Object();
        private byte[] nextChunk() {
            synchronized (chunkSerializer) {
                return doNextChunk();
            }
        }

        private long getApproxTotalAudits() {
            long approxTotalAudits = 1;

            synchronized (this) {
                if (auditExporter != null) {
                    approxTotalAudits = auditExporter.getApproxNumToExport();
                }
            }

            return approxTotalAudits;
        }

        private long getAuditsDownloaded() {
            long auditsDownloaded = 0;

            synchronized (this) {
                if (auditExporter != null) {
                    auditsDownloaded = auditExporter.getNumExportedSoFar();
                }
            }

            return auditsDownloaded;
        }

        private byte[] doNextChunk() {
            setLastUsed();
            checkForException();
            byte[] chunk = new byte[chunkLength];
            try {
                TimeoutReadResult n = readWithTimeout(pis, chunk, MAX_CHUNK_WAIT);
                logger.log(Level.FINER, "Returning next audit download chunk for context " + this);

                final int num = n.bytesRead;
                if (n.eof) {
                    producerThread.join(5000);
                    checkForException();
                    if (num == 0) return null;
                }

                byte[] got = new byte[num];
                if (num > 0) System.arraycopy(chunk, 0, got, 0, num);
                setLastUsed();
                checkForException();
                return got;
            } catch (IOException e) {
                close();
                throw new RuntimeException("Unable to read exported audit stream", e);
            } catch (InterruptedException e) {
                close();
                throw new RuntimeException("Unable to read exported audit stream", e);
            }
        }

        public void checkForException() {
            final Throwable producerException = getProducerException();
            if (producerException != null) {
                close();
                throw new RuntimeException("Audit .zip producer thread exception: " + producerException.getMessage(), producerException);
            }
        }

        public synchronized void close() {
            logger.log(Level.INFO, "Closing audit download context " + this);
            downloadContexts.remove(this.getOpaqueId());
            try { producerThread.interrupt(); } catch (Throwable t) { /* ignored */ }
            try { pis.close(); } catch (Throwable t) { /* ignored */ }
            try { pos.close(); } catch (Throwable t) { /* ignored */ }
        }

        public synchronized boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DownloadContext)) return false;

            final DownloadContext downloadContext = (DownloadContext)o;

            //noinspection RedundantIfStatement
            if (!opaqueId.equals(downloadContext.opaqueId)) return false;

            return true;
        }

        public synchronized int hashCode() {
            return opaqueId.hashCode();
        }

        public synchronized String toString() {
            return "DownloadContext()[id=" + opaqueId.toString() + ", from=" + new Date(fromTime) + "; to=" + new Date(toTime) + "]";
        }

        public synchronized Throwable getProducerException() {
            return producerException;
        }

        public synchronized void setProducerException(Throwable producerException) {
            this.producerException = producerException;
        }
    }
}
