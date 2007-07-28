package com.l7tech.server.tomcat;

import org.apache.naming.resources.Resource;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/**
 *
 */
public interface VirtualDirEntry extends Comparable {
    /**
     * Get the local part of the name of this entry.
     *
     * @return the local part of the name of this entry, ie "blah.xml".  Never null.
     */
    String getLocalName();

    /**
     * Check if this entry represents a directory.
     *
     * @return true if this is a subdirectory entry.
     */
    boolean isDirectory();

    /**
     * Get the Resource represented by this entry, if it is a file entry.
     *
     * @return the Resource, or null if this entry is a directory.
     */
    Resource getFileResource();

    /**
     * Get the VirtualDirContext represented by this entry, if it is a directory entry.
     *
     * @return a VirtualDirContext, or null if this entry isn't a directory.
     */
    VirtualDirContext getDirectory();

    /**
     * Get the parent directory of this entry, if known.
     *
     * @return the parent directory of this entry or null if unknown or this is the root directory.
     */
    VirtualDirContext getParent();

    /**
     * Set the parent directory of this entry.
     *
     * @param parent the new parent directory.  May be null.
     */
    void setParent(VirtualDirContext parent);

    /**
     * Get any additional attributes for this resource.
     *
     * @return the attributes or null to request that default attributes be created.
     * @throws javax.naming.NamingException if there is a problem fetching the attributes
     */
    Attributes getAttributes() throws NamingException;
}
