package com.l7tech.adminws.translation;

import com.l7tech.identity.imp.IdentityProviderConfigImp;
import com.l7tech.identity.imp.IdentityProviderTypeImp;
import com.l7tech.objectmodel.imp.EntityHeaderImp;
import com.l7tech.objectmodel.EntityHeader;

import java.util.Collection;
import java.util.Iterator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 * Util class used by the console-side implementations of Manager to translate
 * between types used in the admin service and generic types used by the model.
 */
public class TypeTranslator {
    public static com.l7tech.objectmodel.EntityHeader serviceHeaderToGenHeader(com.l7tech.adminws.identity.Header stubHeader) {
        if (stubHeader == null) return null;
        EntityHeaderImp ret = new EntityHeaderImp();
        ret.setName(stubHeader.getName());
        ret.setOid(stubHeader.getOid());
        try {
            ret.setType(Class.forName(stubHeader.getType()));
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return ret;
    }

    public static Collection headerArrayToCollection(com.l7tech.adminws.identity.Header[] headerArray) {
        if (headerArray != null && headerArray.length > 0) {
            Collection ret = new java.util.ArrayList(headerArray.length);
            for (int i = 0; i < headerArray.length; i++) {
                // add the header
                ret.add(TypeTranslator.serviceHeaderToGenHeader(headerArray[i]));
            }
            return ret;
        }
        else return new java.util.ArrayList();
    }

    public static com.l7tech.adminws.identity.Header[] collectionToServiceHeaders(Collection collectionOfGenHeaders) {
        if (collectionOfGenHeaders == null) return new com.l7tech.adminws.identity.Header[0];
        com.l7tech.adminws.identity.Header[] ret = new com.l7tech.adminws.identity.Header[collectionOfGenHeaders.size()];
        Iterator iter = collectionOfGenHeaders.iterator();
        int count = 0;
        while(iter.hasNext()){
            EntityHeader colMember = (EntityHeader)iter.next();
            ret[count] = new com.l7tech.adminws.identity.Header(colMember.getOid(), colMember.getType().toString(), colMember.getName());
            ++count;
        }
        return ret;
    }

    public static com.l7tech.adminws.identity.IdentityProviderConfig genericToServiceIdProviderConfig(com.l7tech.identity.IdentityProviderConfig serviceConfig) {
        if (serviceConfig == null) return null;
        com.l7tech.adminws.identity.IdentityProviderConfig ret = new com.l7tech.adminws.identity.IdentityProviderConfig();
        ret.setDescription(serviceConfig.getDescription());
        ret.setName(serviceConfig.getName());
        ret.setOid(serviceConfig.getOid());
        ret.setTypeClassName(serviceConfig.getType().getClassName());
        ret.setTypeDescription(serviceConfig.getType().getDescription());
        ret.setTypeName(serviceConfig.getType().getName());
        ret.setTypeOid(serviceConfig.getType().getOid());
        return ret;
    }

    public static com.l7tech.identity.IdentityProviderConfig serviceIdentityProviderConfigToGenericOne(com.l7tech.adminws.identity.IdentityProviderConfig stub) {
        if (stub == null) return null;
        IdentityProviderConfigImp ret = new IdentityProviderConfigImp();
        ret.setDescription(stub.getDescription());
        ret.setName(stub.getName());
        ret.setOid(stub.getOid());
        IdentityProviderTypeImp retType = new IdentityProviderTypeImp();
        retType.setClassName(stub.getTypeClassName());
        retType.setDescription(stub.getTypeDescription());
        retType.setName(stub.getTypeName());
        retType.setOid(stub.getTypeOid());
        ret.setType(retType);
        return ret;
    }
}
