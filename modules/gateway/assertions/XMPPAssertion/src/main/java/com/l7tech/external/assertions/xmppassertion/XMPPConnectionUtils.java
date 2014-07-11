package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.InvalidGenericEntityException;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/6/12
 * Time: 4:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPConnectionUtils {
    /*
     * Converts a Generic Entity to its Concrete Entity implementation
     */
    public static <ET extends GenericEntity> ET asConcreteEntity(GenericEntity that, Class<ET> entityClass) throws InvalidGenericEntityException {
        final String entityClassName = that.getEntityClassName();
        if (!entityClass.getName().equals(entityClassName))
            throw new InvalidGenericEntityException("generic entity is not of expected class " + entityClassName + ": actual classname is " + entityClass.getName());

        final String xml = that.getValueXml();
        if (xml == null || xml.length() < 1) {
            // New object -- leave non-base fields as default
            try {
                ET ret = entityClass.newInstance();
                GenericEntity.copyBaseFields(that, ret);
                return ret;
            } catch (Exception e) {
                throw new InvalidGenericEntityException("Unable to instantiate " + entityClass.getName() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }

        XMLDecoder decoder = null;
        try {
            decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(Charsets.UTF8)), null, null, entityClass.getClassLoader());
            Object obj = decoder.readObject();
            if (entityClass.isInstance(obj)) {
                ET ret = entityClass.cast(obj);
                GenericEntity.copyBaseFields(that, ret);
                return ret;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidGenericEntityException("Stream does not contain any entities", e);
        } finally {
            if (decoder != null) decoder.close();
        }

        throw new InvalidGenericEntityException("Generic entity XML stream did not contain an instance of " + entityClassName);
    }
}
