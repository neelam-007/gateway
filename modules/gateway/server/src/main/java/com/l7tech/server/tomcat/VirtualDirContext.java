package com.l7tech.server.tomcat;

import org.apache.naming.NamingContextBindingsEnumeration;
import org.apache.naming.NamingContextEnumeration;
import org.apache.naming.NamingEntry;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.Resource;

import javax.naming.*;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import java.util.*;

/**
 * A DirContext whose (read-only) contents are completely configurable by the caller.
 */
public class VirtualDirContext extends BaseDirContext implements VirtualDirEntry {
    private final Map<String, VirtualDirEntry> entries;
    private final DirContext delegate;
    private final VirtualDirEntryImpl thisEntry;

    /**
     * Create a VirtualDirContext that provides access to the specified virtual directory entries.
     *
     * @param localName the local name of this entry within its parent directory, ie "lib", or the empty
     *                  string if this is a new virtual filesystem root directory.
     * @param entries zero or more directory entries to make available in this virtual directory.
     */
    public VirtualDirContext(String localName, VirtualDirEntry... entries) {
        this.entries = new LinkedHashMap<String, VirtualDirEntry>();
        for (VirtualDirEntry entry : entries) {
            this.entries.put(entry.getLocalName(), entry);
            entry.setParent(this);
        }
        this.delegate = null;
        this.thisEntry = new VirtualDirEntryImpl(localName, this);
    }

    /**
     * Adapt the specified delegate DirContext into a VirtualDirContext so it can be mounted underneath
     * another VirtualDirContext instance.
     *
     * @param localName the local name of this entry within its parent directory, ie "lib"
     * @param delegate the DirContext from which files and subdirectories will be taken
     */
    public VirtualDirContext(String localName, DirContext delegate) {
        this.entries = null;
        this.delegate = delegate;
        this.thisEntry = new VirtualDirEntryImpl(localName, this);
    }

    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty())
            return this;
        VirtualDirEntry entry = treeLookup(name);
        if (entry == null)
            throw new NamingException("Resource not found: " + name);
        if (entry instanceof DirContext)
            return entry;
        else
            return entry.getFileResource();
    }

    private VirtualDirEntry treeLookup(Name name) {
        if (name.isEmpty())
            return this;
        VirtualDirEntry currentEntry = this;
        for (int i = 0; i < name.size(); i++) {
            if (name.get(i).length() == 0)
                continue;
            if (!currentEntry.isDirectory())
                return null;
            VirtualDirContext dir = currentEntry.getDirectory();
            currentEntry = dir.getImmediateChild(name.get(i));
            if (currentEntry == null)
                return null;
        }
        return currentEntry;
    }

    private VirtualDirEntry getImmediateChild(final String kidLocalName) {
        if (entries != null)
            return entries.get(kidLocalName);
        if (delegate == null)
            throw new IllegalStateException("VirtualDirContext has neither entries nor a delegate");

        VirtualDirEntry ret = null;
        try {
            final Object got = delegate.lookup(kidLocalName);
            ret = makeEntry(kidLocalName, got);
        } catch (NamingException e) {
            // FALLTHROUGH and return null
        }
        return ret;
    }

    private VirtualDirEntry makeEntry(final String objectsLocalName, final Object lookedUpObject) {
        VirtualDirEntry ret = null;
        if (lookedUpObject instanceof Resource) {
            ret = new VirtualDirEntryImpl(objectsLocalName) {
                protected Resource findResource() {
                    return (Resource)lookedUpObject;
                }

                protected Attributes findAttributes() throws NamingException {
                    return delegate.getAttributes(objectsLocalName);
                }
            };
        } else if (lookedUpObject instanceof DirContext) {
            ret = new VirtualDirContext(objectsLocalName, (DirContext)lookedUpObject);
        }
        return ret;
    }

    public NamingEnumeration list(Name name) throws NamingException {
        if (name.isEmpty())
            return new NamingContextEnumeration(list(this).iterator());
        VirtualDirEntry entry = treeLookup(name);
        if (entry == null)
            throw new NamingException
                (sm.getString("resources.notFound", name));

        return new NamingContextEnumeration(list(entry).iterator());
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name.isEmpty())
            return new NamingContextBindingsEnumeration(list(this).iterator(), this);
        VirtualDirEntry entry = treeLookup(name);
        if (entry == null)
            throw new NamingException(sm.getString("resources.notFound", name));
        return new NamingContextBindingsEnumeration(list(entry).iterator(), this);
    }

    public void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration list(String name) throws NamingException {
        return list(new CompositeName(name));
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        return listBindings(new CompositeName(name));
    }

    public void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public Object lookupLink(String name) throws NamingException {
        // Links not supported here; turn into normal lookup
        return lookup(name);
    }

    public String getNameInNamespace() throws NamingException {
        if (docBase == null) {
            VirtualDirContext parent = getParent();
            if (parent != null) {
                docBase = parent.getNameInNamespace() + "/" + getLocalName();
            }
        }
        return docBase;
    }

    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return getAttributes(new CompositeName(name), attrIds);
    }

    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {

        VirtualDirEntry entry;
        if (name.isEmpty())
            entry = this;
        else
            entry = treeLookup(name);
        if (entry == null)
            throw new NamingException(sm.getString("resources.notFound", name));

        return entry.getAttributes();
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext getSchema(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    protected ArrayList list(VirtualDirEntry entry) throws NamingException {
        ArrayList<NamingEntry> entries = new ArrayList<NamingEntry>();
        if (!(entry instanceof VirtualDirContext))
            return entries;

        VirtualDirContext vdc = (VirtualDirContext)entry;
        
        VirtualDirEntry[] children;
        if (vdc.entries != null) {
            children = vdc.entries.values().toArray(new VirtualDirEntry[0]);
            Arrays.sort(children);
        } else if (vdc.delegate != null) {
            List<VirtualDirEntry> kids = new ArrayList<VirtualDirEntry>();
            NamingEnumeration<Binding> bindings = vdc.delegate.listBindings("");
            while (bindings.hasMoreElements()) {
                Binding binding = bindings.nextElement();
                String name = binding.getName();
                if (binding.isRelative()) {
                    CompositeName cn = new CompositeName(name);
                    if (cn.size() > 0)
                        name = cn.get(cn.size() - 1);
                }
                kids.add(makeEntry(name, binding.getObject()));
            }
            children = kids.toArray(new VirtualDirEntry[0]);
        } else
            throw new IllegalStateException("VirtualDirContext has neither entries nor a delegate");

        NamingEntry namingEntry;

        for (VirtualDirEntry current : children) {
            Object object = current instanceof DirContext ? current : current.getFileResource();
            namingEntry = new NamingEntry(current.getLocalName(), object, NamingEntry.ENTRY);
            entries.add(namingEntry);
        }
        return entries;
    }

    public String getLocalName() {
        return thisEntry.getLocalName();
    }

    public boolean isDirectory() {
        return true;
    }

    public Resource getFileResource() {
        return null;
    }

    public VirtualDirContext getDirectory() {
        return this;
    }

    public VirtualDirContext getParent() {
        return thisEntry.getParent();
    }

    public void setParent(VirtualDirContext parent) {
        thisEntry.setParent(parent);
    }

    public Attributes getAttributes() throws NamingException {
        return thisEntry.getAttributes();
    }

    public int compareTo(Object o) {
        return thisEntry.compareTo(o);
    }
}
