/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.fed.*;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.PersistentGroupManager;

import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedGroupManager extends PersistentGroupManager {
    public FederatedGroupManager( IdentityProvider provider ) {
        super( provider );
        this.providerConfig = (FederatedIdentityProviderConfig)provider.getConfig();
    }

    protected GroupMembership newMembership( long userOid, long groupOid ) {
        return new FederatedGroupMembership(providerConfig.getOid(), userOid, groupOid);
    }

    protected Class getMembershipClass() {
        return FederatedGroupMembership.class;
    }

    public boolean isMember( User user, Group genericGroup ) throws FindException {
        PersistentGroup group = cast(genericGroup);
        if ( group instanceof VirtualGroup ) {
            if ( providerConfig.isX509Supported() ) {
                String pattern = ((VirtualGroup)group).getX509SubjectDnPattern();
                if ( pattern != null ) {
                    String dn = user.getSubjectDn();
                    return dn == null ? false : CertUtils.dnMatchesPattern( user.getSubjectDn(), pattern );
                }
            }

            if ( providerConfig.isSamlSupported() ) {
                SamlConfig config = providerConfig.getSamlConfig();
                logger.severe("SAML is not yet implemented!"); // TODO
                return false;
            }

            logger.warning("Neither X.509 nor SAML are supported by this Federated Identity Provider. Cannot use Virtual Groups.");
            return false;
        } else {
            // Same as internal groups
            return super.isMember( user, group );
        }
    }

    public PersistentGroup cast(Group group) {
        FederatedGroup imp;
        if ( group instanceof GroupBean ) {
            imp = new FederatedGroup( (GroupBean)group );
        } else {
            imp = (FederatedGroup)group;
        }
        return imp;
    }

    public Class getImpClass() {
        return FederatedGroup.class;
    }

    public Class getInterfaceClass() {
        return Group.class;
    }

    public String getTableName() {
        return "fed_group";
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private FederatedIdentityProviderConfig providerConfig;
}
