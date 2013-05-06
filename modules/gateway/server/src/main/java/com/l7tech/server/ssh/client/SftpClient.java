package com.l7tech.server.ssh.client;

import com.jcraft.jsch.SftpException;

/**
 * This is the interface for the sftp client.
 *
 * @author Victor Kazakov
 */
public interface SftpClient extends FileTransferClient {

    /**
     * Lists the contents of a directory.
     *
     * @param remoteDir  The path to the directory
     * @param remoteFile The directory name
     * @return This returns an XmlVirtualFileList that represents the contents of the directory.
     * @throws SftpException This is thrown f there was an error reading the directory contents.
     */
    public XmlVirtualFileList listDirectory(String remoteDir, String remoteFile) throws SftpException;

    /**
     * Retrieves file attributes.
     *
     * @param remoteDir  The path to the file or directory
     * @param remoteFile The name for the file or directory.
     * @return Returns an XmlSshFile representing the file. ITs attributes will be appropriately set
     * @throws SftpException This is thrown if there was an error reading the file attributes
     */
    public XmlSshFile getFileAttributes(String remoteDir, String remoteFile) throws SftpException;

    /**
     * Deletes a file. This cannot be used to delete directories.
     *
     * @param remoteDir  The directory that the file is in
     * @param remoteFile The name of the file
     * @throws SftpException This is thrown if there was an error deleting the file.
     */
    public void deleteFile(String remoteDir, String remoteFile) throws SftpException;

    /**
     * Rename a file or directory.
     *
     * @param remoteDir   The path to the file or directory
     * @param remoteFile  The name for the file or directory.
     * @param newFileName The new name and path of the directory or file
     * @throws SftpException This is thrown if there was an error renaming the directory or file
     */
    public void renameFile(String remoteDir, String remoteFile, String newFileName) throws SftpException;

    /**
     * Create a new empty directory
     *
     * @param remoteDir  The directory to create the new directory in.
     * @param remoteFile The name of the new directory
     * @throws SftpException This is thrown if there was an error creating the directory.
     */
    public void createDirectory(String remoteDir, String remoteFile) throws SftpException;

    /**
     * Delete a directory. This cannot be used to delete file or non empty directories.
     *
     * @param remoteDir  The path to the directory
     * @param remoteFile The name of the directory to delete
     * @throws SftpException This is thrown if there was an error deleting the directory
     */
    public void removeDirectory(String remoteDir, String remoteFile) throws SftpException;

    /**
     * Set the permissions of a directory or file.
     *
     * @param remoteDir   The path to the directory or file
     * @param remoteFile  The name of the directory or file to set permissions of.
     * @param permissions The permission to give the directory or file
     * @throws SftpException This is thrown if there was an error setting the permissions
     */
    public void setFilePermissions(String remoteDir, String remoteFile, int permissions) throws SftpException;
}
