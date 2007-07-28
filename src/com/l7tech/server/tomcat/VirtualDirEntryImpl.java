package com.l7tech.server.tomcat;

import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/**
 * Represents an entry in a {@link VirtualDirContext}.
 */
public class VirtualDirEntryImpl implements VirtualDirEntry {
    protected final String localName;
    protected Resource resource;
    protected Attributes attrs;
    protected boolean isDirectory = false;
    protected VirtualDirContext directory;
    protected VirtualDirContext parent;

    /**
     * Create a VirtualDirEntry with the specified name.
     *
     * @param localName the local part of the name, not fully qualified, ie "blah.xml".  Use empty string
     *                  if this resource is a root directory.
     */
    public VirtualDirEntryImpl(String localName) {
        this.localName = localName;
    }

    /**
     * Create a VirtualDirEntry with the specified name that represents a subdirectory entry.
     *
     * @param localName the local part of the name, not fully qualified, ie "blah.xml".  Use empty string
     *                  if this resource is a root directory.
     * @param subdir subdirectory represented by this directory entry.  Required.
     */
    public VirtualDirEntryImpl(String localName, VirtualDirContext subdir) {
        if (localName == null) throw new NullPointerException();
        if (subdir == null) throw new NullPointerException();
        this.localName = localName;
        this.isDirectory = true;
        this.directory = subdir;
        ResourceAttributes rats = new ResourceAttributes();
        this.attrs = rats;
        rats.setName(localName);
        rats.setCollection(true);
    }

    /**
     * Create a VirtualDirEntry with the specified name and file contents.
     *
     * @param localName the local part of the name, not fully qualified, ie "blah.xml".  Use empty string
     *                  if this resource is a root directory.
     * @param resourceBytes the bytes of the file to keep at this name.
     */
    public VirtualDirEntryImpl(String localName, byte[] resourceBytes) {
        this.localName = localName;
        this.resource = new Resource(resourceBytes);
        ResourceAttributes rats = new ResourceAttributes();
        this.attrs = rats;
        rats.setName(localName);
        rats.setContentLength(resourceBytes.length);
    }

    /**
     * Create a VirtualDirEntry for the specified resource with the specified attributes.
     * The local name will be extracted from the attributes.
     *
     * @param resource  resource to create.  required
     * @param attrs     attributes to use.  required
     */
    public VirtualDirEntryImpl(Resource resource, ResourceAttributes attrs) {
        this.resource = resource;
        this.attrs = attrs;
        this.localName = attrs.getName();
    }

    public String getLocalName() {
        return localName;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public Resource getFileResource() {
        if (isDirectory())
            return null;
        if (resource == null)
            resource = findResource();
        return resource;
    }

    public VirtualDirContext getDirectory() {
        if (!isDirectory())
            return null;
        if (directory == null)
            directory = findDirectory();
        return directory;
    }

    public VirtualDirContext getParent() {
        return parent;
    }

    public void setParent(VirtualDirContext parent) {
        this.parent = parent;
    }

    /**
     * Subclasses can override this to support lazily creating the VirtualDirContext instance.
     *
     * @return a VirtualDirContext instance.  Never null.
     */
    protected VirtualDirContext findDirectory() {
        throw new IllegalStateException("no VirtualDirContext set for virtual directory resource");
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Attributes getAttributes() throws NamingException {
        if (attrs == null) {
            attrs = findAttributes();
            initAttributes(attrs);
        }
        return attrs;
    }

    public void setAttributes(ResourceAttributes attrs) {
        this.attrs = attrs;
    }

    /**
     * Subclasses can override this to support lazily creating the Resource instance.
     * <p/>
     * If not overridden, this method always throws IllegalStateException.
     *
     * @return a Resource instance.  Never null.
     */
    protected Resource findResource() {
        throw new IllegalStateException("no Resource set for virtual file resource");
    }

    /**
     * Subclasses can override this to support lazily creating the ResourceAttributes instance.
     * <p/>
     * If not overridden, this method always returns a new ResourceAttributes instance.
     *
     * @return a ResourceAttributes instance.  Never null.
     * @throws javax.naming.NamingException if the attributes can't be found
     */
    protected Attributes findAttributes() throws NamingException {
        return new ResourceAttributes();
    }

    /**
     * Subclasses can override this to support lazily populating a newly-created ResourceAttributes instance.
     * <p/>
     * If not overridden, this method always sets the NAME attribute and sets Collection to true
     * if this is a directory entry.
     *
     * @param attrs the newly-created ResourceAttributes instance.  Required.
     */
    protected void initAttributes(Attributes attrs) {
        if (attrs instanceof ResourceAttributes) {
            ResourceAttributes rats = (ResourceAttributes)attrs;
            rats.setName(localName);
            rats.setCollection(isDirectory());
        }
    }

    public int compareTo(Object o) {
        return getLocalName().compareTo(((VirtualDirEntry)o).getLocalName());
    }
}
