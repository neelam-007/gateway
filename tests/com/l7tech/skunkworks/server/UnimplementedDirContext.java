package com.l7tech.skunkworks.server;

import org.apache.naming.resources.BaseDirContext;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

/** A DirContext that always fails all operations. */
class UnimplementedDirContext extends BaseDirContext {
    public Object lookup(String name) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public void unbind(String name) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public void rename(String oldName, String newName) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public NamingEnumeration list(String name) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        throw new NamingException("Not found");
    }

    public void destroySubcontext(String name) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public Object lookupLink(String name) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public String getNameInNamespace() throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        throw new NamingException("Not implemented");
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public DirContext getSchema(String name) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
        throw new NamingException("Unimplemented");
    }

    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        throw new NamingException("Unimplemented");
    }
}
