package com.l7tech.common.io;

import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extension of ObjectInputStream that uses a specified ClassLoader.
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream {

    //- PUBLIC

    public ClassLoaderObjectInputStream( final InputStream in,
                                         final ClassLoader classLoader ) throws IOException {
        super(in);
        this.classLoader = classLoader;
    }

    //- PROTECTED

    protected Class resolveClass( final ObjectStreamClass desc ) throws IOException, ClassNotFoundException {
        Class clazz = null;
        ClassNotFoundException recnfe = null;
        try {
            //Note that the classes version is checked later so no need to do so here
            clazz = classLoader!=null ? classLoader.loadClass(desc.getName()) : null;
        }
        catch(ClassNotFoundException cnfe) {
            recnfe = cnfe;
        }

        if (clazz == null) {
            try {
                clazz = super.resolveClass(desc);
            }
            catch(ClassNotFoundException cnfe) {
                // ignore
            }
        }

        if (clazz == null) {
            if (recnfe != null)
                throw recnfe;
            else
                throw new ClassNotFoundException(desc.getName());
        }

        return clazz;
    }

    //- PRIVATE

    private final ClassLoader classLoader;
}
