package com.l7tech.adminws.translation;

import com.l7tech.identity.imp.IdentityProviderConfigImp;
import com.l7tech.identity.imp.IdentityProviderTypeImp;
import com.l7tech.objectmodel.imp.EntityHeaderImp;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 */
public class TypeTranslator {
    public static com.l7tech.identity.IdentityProviderConfig transferStubIdentityProviderConfigToGenericOne(com.l7tech.adminws.clientstub.IdentityProviderConfig stub) {
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

    public static com.l7tech.adminws.clientstub.IdentityProviderConfig transferGenericIdentityProviderConfigToStubOne(com.l7tech.identity.IdentityProviderConfig gen) {
        com.l7tech.adminws.clientstub.IdentityProviderConfig ret = new com.l7tech.adminws.clientstub.IdentityProviderConfig();
        ret.setDescription(gen.getDescription());
        ret.setName(gen.getName());
        ret.setOid(gen.getOid());
        ret.setTypeClassName(gen.getType().getClassName());
        ret.setTypeClassName(gen.getType().getClassName());
        ret.setTypeDescription(gen.getType().getDescription());
        ret.setTypeName(gen.getType().getName());
        ret.setTypeOid(gen.getType().getOid());
        return ret;
    }

    public static com.l7tech.objectmodel.EntityHeader transferStubHeaderToGenHeader(com.l7tech.adminws.clientstub.Header stubHeader) {
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
}
