package com.l7tech.external.assertions.ssh.server;

import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.filesystem.NativeSshFile;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Represents a virtual file or directory.
 */
public class VirtualSshFile implements SshFile {
    //default the last modified time to the current time. This was existing behaviour, it may solve catching issues.
    private long lastModified = System.currentTimeMillis();
    private String fileName;
    //This is used to keep track of if this is a file or a directory.
    private boolean file;
    private long size;

    //The file output stream. Writing to this stream will write to the input stream of the request message
    private OutputStream outputStream;
    //The file input stream. Data in this stream will come from the response message stream.
    private InputStream inputStream;

    private int permission = -1;
    private long accessTime = System.currentTimeMillis();

    //This holds message processing status information.
    private MessageProcessingSshUtil.MessageProcessingStatus messageProcessingStatus = new MessageProcessingSshUtil.MessageProcessingStatus();

    /**
     * This is the list of files that are children of this file, if this file is a folder.
     */
    private List<SshFile> sshFiles;

    /**
     * This is an object used to notify when the file handle is closed by the sftpclient
     */
    private CountDownLatch fileOpenLatch = new CountDownLatch(1);
    private boolean exists = true;
    //This is the next expected read or write offset of the file.
    private long nextExpectedOffset = 0;
    private Functions.NullaryThrows<InputStream, IOException> inputStreamGetter;

    /**
     * Create a new virtualSshFile.             `
     *
     * @param fileName The full file name of the file. This must not be null or blank, and it must start with a /
     * @param file     True if this is a file, false if it is a directory
     */
    protected VirtualSshFile(@NotNull @NotBlank final String fileName, final boolean file) {
        setName(fileName);
        setFile(file);
    }

    /**
     * Get full name. This is the full file path
     *
     * @return The absolute file path
     */
    @Override
    public String getAbsolutePath() {
        // strip the last '/' if necessary
        String fullName = fileName;
        int filelen = fullName.length();
        if ((filelen != 1) && (fullName.charAt(filelen - 1) == '/')) {
            fullName = fullName.substring(0, filelen - 1);
        }

        return fullName;
    }

    /**
     * The path to the file without the file name.
     *
     * @return The path to the file without the file name.
     */
    public String getPath(){
        String path = getAbsolutePath();
        int pathLastIndex = path.lastIndexOf('/');
        if (pathLastIndex > 0) {
            path = path.substring(0, pathLastIndex);
        } else if (pathLastIndex == 0) {
            path = "/";
        }
        return path;
    }

    /**
     * Get short name. The file name after the last /
     *
     * @return The file name
     */
    @Override
    public String getName() {
        // root - the short name will be '/'
        if (fileName.equals("/")) {
            return "/";
        }

        // strip the last '/'
        String shortName = fileName;
        int filelen = fileName.length();
        if (shortName.charAt(filelen - 1) == '/') {
            shortName = shortName.substring(0, filelen - 1);
        }

        // return from the last '/'
        int slashIndex = shortName.lastIndexOf('/');
        if (slashIndex != -1) {
            shortName = shortName.substring(slashIndex + 1);
        }
        return shortName;
    }

    /**
     * Sets the file name. This cannot be null or blank. It must start with a /
     * @param fileName The name of the file.
     */
    public void setName(@NotNull @NotBlank String fileName) {
        //Validating that the file name is proper.
        if (fileName == null) {
            throw new IllegalArgumentException("fileName can not be null");
        }
        if (fileName.length() == 0) {
            throw new IllegalArgumentException("fileName can not be empty");
        } else if (fileName.charAt(0) != '/') {
            throw new IllegalArgumentException("fileName must be an absolute path");
        }
        this.fileName = fileName;
    }

    /**
     * Return's the file owner. Currently this always returns the empty String.
     *
     * @return The file owner.
     */
    @Override
    public String getOwner() {
        return "";
    }

    /**
     * Returns true if this is a directory. This is equivalent to !isFile()
     *
     * @return True if this is a directory, false otherwise
     */
    @Override
    public boolean isDirectory() {
        // Always return true to allow removal of directories
        return !isFile();
    }

    /**
     * Returns true if this is a file (not a directory)
     *
     * @return True if this is a file, false otherwise
     */
    @Override
    public boolean isFile() {
        return file;
    }

    /**
     * Sets if this is a file or directory
     *
     * @param file true if this is a file. false if it is a directory
     */
    public void setFile(boolean file) {
        this.file = file;
    }

    /**
     * Does this file exists?
     *
     * @return true if the file exists.
     */
    @Override
    public boolean doesExist() {
        return exists;
    }

    /**
     * Returns the file size in bytes
     *
     * @return The file size in bytes
     */
    @Override
    public long getSize() {
        return size;
    }

    /**
     * Set the file size in bytes.
     *
     * @param size The files size in bytes
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Get the last modified time
     *
     * @return The timestamp of the last modified time for the {@link SshFile}
     */
    @Override
    public long getLastModified() {
        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLastModified(long lastModified) {
        this.lastModified = lastModified;
        return true;
    }

    /**
     * Check read permission. Virtual ssh files are always readable
     *
     * @return true if the file is readable
     */
    @Override
    public boolean isReadable() {
        return true;
    }

    /**
     * Check file write permission. Virtual ssh files are always writable
     *
     * @return true if the file is writable
     */
    @Override
    public boolean isWritable() {
        return true;
    }

    /**
     * Check file exec permission. Virtual ssh files are never executable
     *
     * @return true if the file is executable
     */
    @Override
    public boolean isExecutable() {
        return false;
    }

    /**
     * Has delete permission. Virtual ssh files are never deletable
     *
     * @return true is the file is deletable
     */
    @Override
    public boolean isRemovable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SshFile getParentFile() {
        int indexOfSlash = getAbsolutePath().lastIndexOf('/');
        String parentFullName;
        if (indexOfSlash == 0) {
            parentFullName = "/";
        } else {
            parentFullName = getAbsolutePath().substring(0, indexOfSlash);
        }

        return new VirtualSshFile(parentFullName, false);
    }

    /**
     * Delete file. Virtual ssh files can never be deleted. Always returns false
     *
     * @return true if the operation was successful
     */
    @Override
    public boolean delete() {
        return false;
    }

    /**
     * Create a new file. Can never create a new virtual ssh file.
     *
     * @return true if the file has been created and false if it already exist
     */
    @Override
    public boolean create() {
        return false;
    }

    /**
     * Truncate file to length 0. Virtual files are not actually truncated, this method does nothing.
     */
    @Override
    public void truncate() {
        // do nothing for virtual file
    }

    /**
     * Move file object. Cannot move virtual ssh files
     *
     * @param destination The target {@link SshFile} to move the current {@link SshFile} to
     * @return true if the operation was successful
     */
    @Override
    public boolean move(final SshFile destination) {
        return false;
    }

    /**
     * Create directory. Can never make a virtual ssh directory
     *
     * @return true if the operation was successful
     */
    @Override
    public boolean mkdir() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SshFile> listSshFiles() {
        return sshFiles == null ? null : Collections.unmodifiableList(sshFiles);
    }

    /**
     * Set the child files of this directory. A @IllegalStateException will be thrown if this is not a directory.
     *
     * @param sshFiles The children of this directory.
     */
    public void setSshFiles(List<SshFile> sshFiles) {
        if (!isDirectory()) {
            throw new IllegalStateException("Cannot set child files on a file that is not a directory");
        }
        this.sshFiles = sshFiles;
    }

    /**
     * Create output stream for writing. Virtual SSH files will always throw an exception if this is called.
     */
    @Override
    public OutputStream createOutputStream(final long offset) throws IOException {
        throw new IOException();
    }

    /**
     * Create input stream for reading. Virtual SSH files will always throw an exception if this is called.
     */
    @Override
    public InputStream createInputStream(final long offset) throws IOException {
        throw new IOException();
    }

    /**
     * Handle post-handle-close functionality.
     */
    @Override
    public void handleClose() {
        //flush and close the output stream.
        ResourceUtils.flushQuietly(outputStream);
        ResourceUtils.closeQuietly(outputStream);
        //Close the input stream
        ResourceUtils.closeQuietly(inputStream);
        //countdown the file open latch to notify that the file handle has been closed.
        fileOpenLatch.countDown();
    }

    /**
     * two virtual ssh files are considered equal if the absolute paths match.
     *
     * @param obj The object to compare this virtual ssh file to.
     * @return True if the virtual ssh files are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof VirtualSshFile) {
            return this.getAbsolutePath().equals(((VirtualSshFile) obj).getAbsolutePath());
        }
        return false;
    }

    /**
     * Gets the output stream for this file. Data written to the output stream will get piped to the policy processing this file.
     *
     * @return The virtual file output stream
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Sets the output steam of this virtual ssh file. This stream must be piped to the policy processing this file. This throws an @IllegalStateException if the output stream has already been set.
     *
     * @param outputStream The virtual file output stream.
     */
    public void setOutputStream(OutputStream outputStream) {
        if (this.outputStream != null) {
            throw new IllegalStateException("The output stream for this virtual ssh file has already been set.");
        }
        this.outputStream = outputStream;
    }

    /**
     * Gets the input stream for this file. Data read from the input stream will come from the policy processing this request.
     *
     * @return The virtual file input stream
     */
    public InputStream getInputStream() throws IOException {
        if(inputStream == null){
            inputStream = inputStreamGetter.call();
        }
        return inputStream;
    }

    /**
     * Sets the input steam of this virtual ssh file. This stream must be piped to the policy processing this file. This throws an @IllegalStateException if the input stream has already been set.
     *
     * @param inputStream The virtual file input stream.
     */
    public void setInputStreamGetter(Functions.NullaryThrows<InputStream, IOException> inputStream) {
        if (this.inputStreamGetter != null) {
            throw new IllegalStateException("The input stream for this virtual ssh file has already been set.");
        }
        this.inputStreamGetter = inputStream;
    }

    /**
     * Returns the file permissions for this file.
     *
     * @return The file permissions
     */
    public int getPermission() {
        return permission;
    }

    /**
     * Sets the file permissions.
     *
     * @param permission The file permissions
     */
    public void setPermission(final int permission) {
        this.permission = permission;
    }

    /**
     * Returns the last access time for the file
     *
     * @return The last access time.
     */
    public long getAccessTime() {
        return accessTime;
    }

    /**
     * Sets the last access time for the file.
     *
     * @param accessTime The last access time
     */
    public void setAccessTime(final long accessTime) {
        this.accessTime = accessTime;
    }

    /**
     * Blocks this thread till the file handle has been closed.
     *
     * @throws InterruptedException
     */
    public void waitForHandleClosed() throws InterruptedException {
        fileOpenLatch.await();
    }

    /**
     * Gets the physical name of the file. This delegates to NativeSshFile to do the processing.
     *
     * @param rootDir         The root directory.
     * @param currDir         The current directory. It will always be with respect to the root directory.
     * @param fileName        The input file name.
     * @param caseInsensitive Is the file case insensitive
     * @return The return string will always begin with the root directory. It will never be null.
     */
    public static String getPhysicalName(final String rootDir, final String currDir, final String fileName, final boolean caseInsensitive) {
        return NativeSshFile.getPhysicalName(rootDir, currDir, fileName, caseInsensitive);
    }

    /**
     * Gets the message processing status object.
     *
     * @return The message processing status.
     */
    public MessageProcessingSshUtil.MessageProcessingStatus getMessageProcessingStatus() {
        return messageProcessingStatus;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public long getNextExpectedOffset() {
        return nextExpectedOffset;
    }

    public void setNextExpectedOffset(long nextExpectedOffset) {
        this.nextExpectedOffset = nextExpectedOffset;
    }

    /**
     * Resets the file, leaving only its name and basic stats.
     */
    public void reset() {
        outputStream = null;
        inputStream = null;
        messageProcessingStatus = new MessageProcessingSshUtil.MessageProcessingStatus();
        fileOpenLatch = new CountDownLatch(1);
        nextExpectedOffset = 0;
        inputStreamGetter = null;
    }
}
