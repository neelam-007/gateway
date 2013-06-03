package com.l7tech.external.assertions.ftprouting.server;

import org.apache.ftpserver.ftplet.FileObject;
import java.io.*;

/**
 * @author nilic
 */
public class VirtualFileObject implements FileObject {

    //- PUBLIC

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

    //- PACKAGE

    VirtualFileObject(boolean file, String path) {
        this.file = file;
        this.path = path;
    }

    //- PRIVATE

    private static final long date = System.currentTimeMillis();
    private final boolean file;
    private final String path;
}
