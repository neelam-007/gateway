package com.l7tech.server.transport.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import org.apache.ftpserver.ftplet.FtpFile;

/**
 * Represents a virtual file or directory.
 *
 * @author Steve Jones
 */
class VirtualFileObject implements FtpFile {
    private static final long date = System.currentTimeMillis();

    private final boolean file;
    private final String path;

    VirtualFileObject(boolean file, String path) {
        this.file = file;
        this.path = path;
    }

    @Override
    public InputStream createInputStream(long offset) throws IOException {
        throw new IOException();
    }

    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        throw new IOException();
    }

    @Override
    public List<FtpFile> listFiles() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean delete() {
        return true;
    }

    @Override
    public boolean doesExist() {
        return !file;
    }

    @Override
    public String getAbsolutePath() {
        return path;
    }

    @Override
    public String getGroupName() {
        return "gateway";
    }

    @Override
    public long getLastModified() {
        return date;
    }

    @Override
    public boolean setLastModified(long date) {
//        this.date = date; // TODO jwilliams: should we set this? either make date mutable, or ignore set requests
        return true;
    }

    @Override
    public int getLinkCount() {
        return 1;
    }

    @Override
    public String getOwnerName() {
        return "gateway";
    }

    @Override
    public String getName() {
        String name = path;

        if (name.indexOf('/') > -1) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }

        return name;
    }

    @Override
    public long getSize() {
        return file ? 0 : 4096;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        // Always return true to allow removal of directories
        return true;
    }

    @Override
    public boolean isFile() {
        return file && doesExist();
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public boolean mkdir() {
        return true;
    }

    @Override
    public boolean move(FtpFile destination) {
        return false;
    }
}
