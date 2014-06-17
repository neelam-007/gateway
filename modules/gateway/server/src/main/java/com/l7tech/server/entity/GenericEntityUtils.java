package com.l7tech.server.entity;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;

import java.beans.XMLEncoder;

/**
 * These are utility method for generic entities.
 */
public class GenericEntityUtils {

    /**
     * This will set the generic entity value xml from the current values on the generic entity.
     * @param that The generic entity to regenerate the value xml for.
     */
    public static void regenerateValueXml(GenericEntity that) {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream(1024);
        String xml = that.getValueXml();
        ClassLoader oldContext = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(that.getClass().getClassLoader());
        try {
            that.setValueXml(""); // set to empty while serializing to prevent including the XML in the XML
            XMLEncoder encoder = new XMLEncoder(new NonCloseableOutputStream(baos));
            //allowing Goid's to be serialization by default
            encoder.setPersistenceDelegate( Goid.class, Goid.getPersistenceDelegate() );
            encoder.writeObject(that);
            encoder.close();
            xml = baos.toString(Charsets.UTF8);
        } finally {
            ResourceUtils.closeQuietly(baos);
            that.setValueXml(xml);
            Thread.currentThread().setContextClassLoader(oldContext);
        }
    }
}
