/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.DataType;
import com.l7tech.objectmodel.UsersOrGroups;
import com.l7tech.objectmodel.AttributeHeader;

/**
 * @author alex
 */
public class FederatedAttributeMapping extends PersistentAttributeMapping {
    public static final AttributeHeader VIRTUAL_SAML_PATTERN = new AttributeHeader("fedVirtualSamlPattern", "SAML NameIdentifier Pattern", DataType.STRING, UsersOrGroups.GROUPS, AttributeHeader.Builtin.BUILTIN);
    public static final AttributeHeader VIRTUAL_X509_PATTERN = new AttributeHeader("fedVirtualX509Pattern", "X.509 Subject DN Pattern", DataType.STRING, UsersOrGroups.GROUPS, AttributeHeader.Builtin.BUILTIN);

    public static AttributeHeader[] getBuiltinAttributes() {
        AttributeHeader[] superAtts = PersistentAttributeMapping.getBuiltinAttributes();
        AttributeHeader[] atts = new AttributeHeader[superAtts.length + 2];
        System.arraycopy(superAtts, 0, atts, 0, superAtts.length);
        atts[atts.length-2] = VIRTUAL_SAML_PATTERN;
        atts[atts.length-1] = VIRTUAL_X509_PATTERN;
        return atts;
    }

    public FederatedAttributeMapping() {
        super(null, IdentityProviderConfig.DEFAULT_GOID, UsersOrGroups.USERS);
    }

    public FederatedAttributeMapping(AttributeConfig parent, Goid providerOid, UsersOrGroups uog) {
        super(parent, providerOid, uog);
    }

}
