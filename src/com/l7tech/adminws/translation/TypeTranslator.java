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
 *
 */
public class TypeTranslator {
    public static com.l7tech.objectmodel.EntityHeader serviceHeaderToGenHeader(com.l7tech.adminws.identity.Header stubHeader) throws ClassNotFoundException {
        if (stubHeader == null) return null;
        EntityHeaderImp ret = new EntityHeaderImp();
        ret.setName(stubHeader.getName());
        ret.setOid(stubHeader.getOid());
        ret.setType(Class.forName(stubHeader.getType()));
        return ret;
    }

    public static Collection headerArrayToCollection(com.l7tech.adminws.identity.Header[] headerArray) throws ClassNotFoundException {
        if (headerArray == null) return new java.util.ArrayList();
        Collection ret = new java.util.ArrayList(headerArray.length);
        for (int i = 0; i < headerArray.length; i++) {
            // add the header
            ret.add(TypeTranslator.serviceHeaderToGenHeader(headerArray[i]));
        }
        return ret;
    }

    public static com.l7tech.adminws.identity.Header[] collectionToServiceHeaders(Collection collectionOfGenHeaders) {
        if (collectionOfGenHeaders == null) return new com.l7tech.adminws.identity.Header[0];
        com.l7tech.adminws.identity.Header[] ret = new com.l7tech.adminws.identity.Header[collectionOfGenHeaders.size()];
        Iterator iter = collectionOfGenHeaders.iterator();
        int count = 0;
        while (iter.hasNext()) {
            EntityHeader colMember = (EntityHeader)iter.next();
            ret[count] = new com.l7tech.adminws.identity.Header(colMember.getOid(), colMember.getType().getName(), colMember.getName());
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

    public static com.l7tech.adminws.identity.User genUserToServiceUser(com.l7tech.identity.User genUser) {
        if (genUser == null) return null;
        com.l7tech.adminws.identity.User ret = new com.l7tech.adminws.identity.User();
        ret.setEmail(genUser.getEmail());
        ret.setFirstName(genUser.getFirstName());
        ret.setGroups(collectionToServiceHeaders(genUser.getGroupHeaders()));
        ret.setLastName(genUser.getLastName());
        ret.setLogin(genUser.getLogin());
        ret.setOid(genUser.getOid());
        ret.setPassword(genUser.getPassword());
        ret.setTitle(genUser.getTitle());
        return ret;
    }

    public static com.l7tech.identity.User serviceUserToGenUser(com.l7tech.adminws.identity.User svcUser) throws ClassNotFoundException {
        if (svcUser == null) return null;
        com.l7tech.identity.User ret = new com.l7tech.identity.internal.imp.UserImp();
        ret.setEmail(svcUser.getEmail());
        ret.setFirstName(svcUser.getFirstName());

        Collection groups = headerArrayToCollection(svcUser.getGroups());
        ret.getGroups().addAll(groups);
        ret.getGroupHeaders().addAll(groups);

        ret.setLastName(svcUser.getLastName());
        ret.setLogin(svcUser.getLogin());
        ret.setOid(svcUser.getOid());
        ret.setPassword(svcUser.getPassword());
        ret.setTitle(svcUser.getTitle());
        return ret;
    }

    public static com.l7tech.adminws.identity.Group genGroupToServiceGroup(com.l7tech.identity.Group genGroup) {
        if (genGroup == null) return null;
        com.l7tech.adminws.identity.Group ret = new com.l7tech.adminws.identity.Group();
        ret.setDescription(genGroup.getDescription());
        ret.setMembers(collectionToServiceHeaders(genGroup.getMemberHeaders()));
        ret.setName(genGroup.getName());
        ret.setOid(genGroup.getOid());
        return ret;
    }

    public static com.l7tech.identity.Group serviceGroupToGenGroup(com.l7tech.adminws.identity.Group svcGroup) throws ClassNotFoundException {
        if (svcGroup == null) return null;
        com.l7tech.identity.Group ret = new com.l7tech.identity.internal.imp.GroupImp();
        ret.setDescription(svcGroup.getDescription());

        Collection col = headerArrayToCollection(svcGroup.getMembers());
        // add it both places (just in case)
        ret.getMembers().addAll(col);
        ret.getMemberHeaders().addAll(col);

        ret.setName(svcGroup.getName());
        ret.setOid(svcGroup.getOid());
        return ret;
    }
}
