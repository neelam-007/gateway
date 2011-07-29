package com.l7tech.external.assertions.ssh.server;

import com.l7tech.policy.assertion.AssertionStatus;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.filesystem.NameEqualsFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Represents a virtual file or directory.
 */
public class VirtualSshFile implements SshFile {

    private final Logger LOG = LoggerFactory.getLogger(VirtualSshFile.class);

    private static final long date = System.currentTimeMillis();
    private String fileName;
    private boolean file;
    private PipedOutputStream pipedOutputStream;
    private AssertionStatus messageProcessStatus;
    private Thread messageProcessThread;

    /**
     * Constructor, internal do not use directly.
     */
    protected VirtualSshFile(final String fileName, final boolean file) {
        this.fileName = fileName;
        this.file = file;
    }

    /**
     * Get full name.
     */
    public String getAbsolutePath() {
        return fileName;
    }

    /**
     * Get short name.
     */
    public String getName() {
        String name = fileName;

        if (name.indexOf('/') > -1) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }

        return name;
    }

    /**
     * Is it a directory?
     */
    public boolean isDirectory() {
        // Always return true to allow removal of directories
        return true;
    }

    /**
     * Is it a file?
     */
    public boolean isFile() {
        return file && doesExist();
    }

    /**
     * Does this file exists?
     */
    public boolean doesExist() {
        return !file;
    }

    /**
     * Get file size.
     */
    public long getSize() {
        return file ? 0 : 4096;
    }

    /**
     * Get last modified time.
     */
    public long getLastModified() {
        return date;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setLastModified(long time) {
        return true;
    }

    /**
     * Check read permission.
     */
    public boolean isReadable() {
        return true;
    }

    /**
     * Check file write permission.
     */
    public boolean isWritable() {
        return true;
    }

    /**
     * Has delete permission.
     */
    public boolean isRemovable() {
        return true;
    }

    public SshFile getParentFile() {
        int indexOfSlash = getAbsolutePath().lastIndexOf('/');
        String parentFullName;
        if (indexOfSlash == 0) {
            parentFullName = "/";
        } else {
            parentFullName = getAbsolutePath().substring(0, indexOfSlash);
        }

        // we check if the parent FileObject is writable.
        return new VirtualSshFile(parentFullName, false);
    }

    /**
     * Delete file.
     */
    public boolean delete() {
        return true;
    }

    /**
     * Truncate file to length 0.
     */
    public void truncate() throws IOException{
        // do nothing for virtual file
    }

    /**
     * Move file object.
     */
    public boolean move(final SshFile dest) {
        return false;
    }

    /**
     * Create directory.
     */
    public boolean mkdir() {
        return true;
    }

    /**
     * List files. If not a directory or does not exist, null will be returned.
     */
    public List<SshFile> listSshFiles() {
        SshFile[] virtualFiles = new SshFile[0];
        return Collections.unmodifiableList(Arrays.asList(virtualFiles));
    }

    /**
     * Create output stream for writing.
     */
    public OutputStream createOutputStream(final long offset) throws IOException {
        throw new IOException();
    }

    /**
     * Create input stream for reading.
     */
    public InputStream createInputStream(final long offset) throws IOException {
        throw new IOException();
    }

    public void handleClose() {
        // Noop
    }

    /**
     * Normalize separate character. Separate character should be '/' always.
     */
    public final static String normalizeSeparateChar(final String pathName) {
        String normalizedPathName = pathName.replace(File.separatorChar, '/');
        normalizedPathName = normalizedPathName.replace('\\', '/');
        return normalizedPathName;
    }

    /**
     * Get the physical canonical file name. It works like
     * File.getCanonicalPath().
     *
     * @param rootDir
     *            The root directory.
     * @param currDir
     *            The current directory. It will always be with respect to the
     *            root directory.
     * @param fileName
     *            The input file name.
     * @return The return string will always begin with the root directory. It
     *         will never be null.
     */
    public final static String getPhysicalName(final String rootDir, final String currDir, final String fileName) {
        return getPhysicalName(rootDir, currDir, fileName, false);
    }

    public final static String getPhysicalName(final String rootDir,
            final String currDir, final String fileName,
            final boolean caseInsensitive) {

        // get the starting directory
        String normalizedRootDir = normalizeSeparateChar(rootDir);
        if (normalizedRootDir.charAt(normalizedRootDir.length() - 1) != '/') {
            normalizedRootDir += '/';
        }

        String normalizedFileName = normalizeSeparateChar(fileName);
        String resArg;
        String normalizedCurrDir = currDir;
        if (normalizedFileName.charAt(0) != '/') {
            if (normalizedCurrDir == null) {
                normalizedCurrDir = "/";
            }
            if (normalizedCurrDir.length() == 0) {
                normalizedCurrDir = "/";
            }

            normalizedCurrDir = normalizeSeparateChar(normalizedCurrDir);

            if (normalizedCurrDir.charAt(0) != '/') {
                normalizedCurrDir = '/' + normalizedCurrDir;
            }
            if (normalizedCurrDir.charAt(normalizedCurrDir.length() - 1) != '/') {
                normalizedCurrDir += '/';
            }

            resArg = normalizedRootDir + normalizedCurrDir.substring(1);
        } else {
            resArg = normalizedRootDir;
        }

        // strip last '/'
        if (resArg.charAt(resArg.length() - 1) == '/') {
            resArg = resArg.substring(0, resArg.length() - 1);
        }

        // replace ., ~ and ..
        // in this loop resArg will never end with '/'
        StringTokenizer st = new StringTokenizer(normalizedFileName, "/");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();

            // . => current directory
            if (tok.equals(".")) {
                continue;
            }

            // .. => parent directory (if not root)
            if (tok.equals("..")) {
                if (resArg.startsWith(normalizedRootDir)) {
                    int slashIndex = resArg.lastIndexOf('/');
                    if (slashIndex != -1) {
                        resArg = resArg.substring(0, slashIndex);
                    }
                }
                continue;
            }

            // ~ => home directory (in this case the root directory)
            if (tok.equals("~")) {
                resArg = normalizedRootDir.substring(0, normalizedRootDir
                        .length() - 1);
                continue;
            }

            if (caseInsensitive) {
                File[] matches = new File(resArg)
                        .listFiles(new NameEqualsFileFilter(tok, true));

                if (matches != null && matches.length > 0) {
                    tok = matches[0].getName();
                }
            }

            resArg = resArg + '/' + tok;
        }

        // add last slash if necessary
        if ((resArg.length()) + 1 == normalizedRootDir.length()) {
            resArg += '/';
        }

        // final check
        if (!resArg.regionMatches(0, normalizedRootDir, 0, normalizedRootDir
                .length())) {
            resArg = normalizedRootDir;
        }

        return resArg;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof VirtualSshFile) {
            return this.getAbsolutePath().equals(((VirtualSshFile) obj).getAbsolutePath());
        }
        return false;
    }

    public PipedOutputStream getPipedOutputStream() {
        return pipedOutputStream;
    }
    public void setPipedOutputStream(PipedOutputStream pipedOutputStream) {
        this.pipedOutputStream = pipedOutputStream;
    }

    public AssertionStatus getMessageProcessStatus() {
        return messageProcessStatus;
    }
    public void setMessageProcessStatus(AssertionStatus messageProcessStatus) {
        this.messageProcessStatus = messageProcessStatus;
    }

    public Thread getMessageProcessThread() {
        return messageProcessThread;
    }
    public void setMessageProcessThread(Thread messageProcessThread) {
        this.messageProcessThread = messageProcessThread;
    }
}
