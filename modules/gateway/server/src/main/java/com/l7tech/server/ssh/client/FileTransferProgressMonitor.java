package com.l7tech.server.ssh.client;

/**
 * A file transfer progress monitor. This is used to hook into the file transferring thread in order to monitor the
 * progress of the file transfer and in retrieve initialization information like the file size that normally would
 * not be available.
 *
 * @author Victor Kazakov
 */
public interface FileTransferProgressMonitor {

    /**
     * This is the Download operation.
     */
    public final static int DOWNLOAD = 0;
    /**
     * This is the Upload operation
     */
    public final static int UPLOAD = 1;

    /**
     * This is called right before the file transfer started. It is given the operation type and an XmlSshFile
     *
     * @param op   The operation type. Either Upload or Download
     * @param file The file being transferred. It is populated with all available attributes.
     */
    public void start(int op, XmlSshFile file);

    /**
     * This is called while transferring the file. It can be used to track the progress of the file transfer.
     * NOTE: This method should be made to execute quickly otherwise it could slow down the file transfer
     *
     * @param totalBytesTransferred This is the total number of bytes transferred so far.
     */
    public void progress(long totalBytesTransferred);

    /**
     * This is called right after the file transfer ends successfully. The streams may still be open at this point.
     */
    public void end();
}
