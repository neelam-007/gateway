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
import com.l7tech.objectmodel.PersistenceManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.identity.PersistentGroupManager;
import net.sf.hibernate.Criteria;
import net.sf.hibernate.expression.Expression;

import java.sql.SQLException;
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

    protected void preSave( PersistentGroup group ) throws SaveException {
        if ( group instanceof VirtualGroup && providerConfig.getTrustedCertOids().length == 0 )
            throw new SaveException("Virtual groups cannot be created in a Federated Identity Provider with no Trusted Certificates");
    }

    protected void addFindAllCriteria( Criteria allHeadersCriteria ) {
        allHeadersCriteria.add(Expression.eq("providerId", new Long(getProviderOid())));
    }

    public Group findByPrimaryKey( String oid ) throws FindException {
        try {
            Group g = super.findByPrimaryKey(oid);
            if ( g == null ) {
                g = (PersistentGroup)PersistenceManager.findByPrimaryKey(getContext(), VirtualGroup.class, Long.parseLong(oid));
                g.setProviderId(providerConfig.getOid());
            }
            return g;
        } catch (SQLException se) {
            throw new FindException(se.toString(), se);
        } catch (NumberFormatException nfe) {
            throw new FindException("Can't find groups with non-numeric OIDs!", nfe);
        }
    }

    public boolean isMember( User user, Group genericGroup ) throws FindException {
        PersistentGroup group = cast(genericGroup);
        if ( group instanceof VirtualGroup ) {
            if ( providerConfig.isX509Supported() ) {
                String pattern = ((VirtualGroup)group).getX509SubjectDnPattern();
                if ( pattern != null ) {
                    String dn = user.getSubjectDn();
                    if ( dn != null &&
                         CertUtils.dnMatchesPattern( user.getSubjectDn(), pattern ) )
                        return true;
                }
            }

            if ( providerConfig.isSamlSupported() ) {
                SamlConfig config = providerConfig.getSamlConfig();
                logger.severe("SAML is not yet implemented!"); // TODO
                return false;
            }

            logger.info("User '" + user.getSubjectDn() + "' does not appear to be a member of the selected VirtualGroup");
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
