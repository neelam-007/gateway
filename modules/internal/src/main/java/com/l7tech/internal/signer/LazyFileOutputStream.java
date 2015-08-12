package com.l7tech.internal.signer;

import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * Utility class which wraps a standard FileOutputStream to simply create a lazy initialized FileOutputStream,
 * instead of creating the file when the constructor is called, it creates the file only when it is really need
 * (when some writing operation happens).
 * <p/>
 * This class is <b>not thread safe</b>!<br/>
 * Not ready for multi-threaded usage.
 */
@SuppressWarnings("UnusedDeclaration")
public class LazyFileOutputStream extends OutputStream {

    private final File file;
    private final boolean append;
    private FileOutputStream fos;

    /**
     * Creates a file output stream to write to the file with the specified name
     *
     * @param name the system-dependent filename
     * @see java.io.FileOutputStream#FileOutputStream(String)
     */
    public LazyFileOutputStream(final String name) {
        this(name != null ? new File(name) : null, false);
    }

    /**
     * Creates a file output stream to write to the file with the specified name.
     * If the second argument is {@code true}, then  bytes will be written to the end of the file rather than the beginning.
     *
     * @param name      the system-dependent file name
     * @param append    if {@code true}, then bytes will be written to the end of the file rather than the beginning
     * @see java.io.FileOutputStream#FileOutputStream(String, boolean)
     */
    public LazyFileOutputStream(final String name, final boolean append) {
        this(name != null ? new File(name) : null, append);
    }

    /**
     * Creates a file output stream to write to the file represented by the specified {@code File} object.
     *
     * @param file    the file to be opened for writing
     * @see java.io.FileOutputStream#FileOutputStream(java.io.File)
     */
    public LazyFileOutputStream(final File file) {
        this(file, false);
    }

    /**
     * Creates a file output stream to write to the file represented by the specified {@code File} object.
     * If the second argument is {@code true}, then bytes will be written to the end of the file rather than the beginning.
     *
     * @param file      the file to be opened for writing
     * @param append    if {@code true}, then bytes will be written to the end of the file rather than the beginning
     * @see java.io.FileOutputStream#FileOutputStream(java.io.File, boolean)
     */
    public LazyFileOutputStream(final File file, final boolean append) {
        this.file = file;
        this.append = append;
    }

    /**
     * Gets the wrapped {@code FileOutputStream} object if already initialized or creates a new instance if not.
     *
     * @return the wrapped {@code FileOutputStream} object, never {@code null}
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file,
     * does not exist but cannot be created, or cannot be opened for any other reason
     * @throws SecurityException  if a security manager exists and its {@code checkWrite} method denies write access to the file.
     */
    @NotNull
    private FileOutputStream getFileOutputStream() throws FileNotFoundException {
        if (fos == null) {
            fos = new FileOutputStream(file, append);
        }
        return fos;
    }

    @Override
    public void write(final int b) throws IOException {
        getFileOutputStream().write(b);
    }

    @Override
    public void write(@NotNull final byte[] b) throws IOException {
        getFileOutputStream().write(b);
    }

    @Override
    public void write(@NotNull final byte[] b, final int off, final int len) throws IOException {
        getFileOutputStream().write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (fos != null) {
            fos.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (fos != null) {
            fos.close();
        }
    }
}
