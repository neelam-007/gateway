package com.l7tech.server.transport.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.ftpserver.ftplet.FileObject;

/**
 * Represents a virtual file or directory.
 *
 * @author Steve Jones
 */
class VirtualFileObject implements FileObject {
    private static final long date = System.currentTimeMillis();

    private final boolean file;
    private final String path;

    VirtualFileObject(boolean file, String path) {
        this.file = file;
        this.path = path;
    }

    public InputStream createInputStream(long offset) throws IOException {
        throw new IOException();
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        throw new IOException();
    }

    public FileObject[] listFiles() {
        return new FileObject[0];
    }

    public boolean delete() {
        return true;
    }

    public boolean doesExist() {
        return !file;
    }

    public String getFullName() {
        return path;
    }

    public String getGroupName() {
        return "gateway";
    }

    public long getLastModified() {
        return date;
    }

    public int getLinkCount() {
        return 1;
    }

    public String getOwnerName() {
        return "gateway";
    }

    public String getShortName() {
        String name = path;

        if (name.indexOf('/') > -1) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }

        return name;
    }

    public long getSize() {
        return file ? 0 : 4096;
    }

    public boolean hasDeletePermission() {
        return true;
    }

    public boolean hasReadPermission() {
        return true;
    }

    public boolean hasWritePermission() {
        return true;
    }

    public boolean isDirectory() {
        // Always return true to allow removal of directories
        return true;
    }

    public boolean isFile() {
        return file && doesExist();
    }

    public boolean isHidden() {
        return true;
    }

    public boolean mkdir() {
        return true;
    }

    public boolean move(FileObject destination) {
        return false;
    }
}
