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
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The {@link GroupManager} for {@link FederatedIdentityProvider}s.
 *
 * Supports {@link FederatedGroup}s and {@link VirtualGroup}s.
 * 
 * @author alex
 * @version $Revision$
 */
public class FederatedGroupManager extends PersistentGroupManager {
    public FederatedGroupManager( IdentityProvider provider ) {
        super( provider );
        this.federatedProvider = (FederatedIdentityProvider)provider;
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
                if (g != null) {
                    g.setProviderId(providerConfig.getOid());
                }
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
            // fix for bugzilla 1101 virtual group membership enforces trusted fip cert
            Set provCerts = federatedProvider.getValidTrustedCertOids();
            if (provCerts == null || provCerts.size() < 1) {
                logger.warning("The virtual group " + group.getUniqueIdentifier() + " is itself invalid because " +
                               "the parent provider has no trusted certs declared (virtual group must rely on fip " +
                               "trusted cert). Returning false.");
                return false;
            }

            if (!checkProvider(user)) return false;
            SamlConfig samlConfig = providerConfig.getSamlConfig();
            if ( providerConfig.isX509Supported() || (providerConfig.isSamlSupported() && samlConfig.isNameIdX509SubjectName()) ) {
                String dnPattern = ((VirtualGroup)group).getX509SubjectDnPattern();
                if ( dnPattern != null && dnPattern.length() > 0 ) {
                    String userDn = user.getSubjectDn();
                    try {
                        if ( userDn != null && userDn.length() > 0 && CertUtils.dnMatchesPattern( user.getSubjectDn(), dnPattern ) )
                            return true;
                    } catch (IllegalArgumentException e) {
                        logger.warning("X.509 Subject DN pattern '" + dnPattern +"' is not a valid DN" );
                    }
                }
            }

            if (providerConfig.isSamlSupported() && samlConfig.isNameIdEmail()) {
                String emailPattern = ((VirtualGroup)group).getSamlEmailPattern();
                String userEmail = user.getEmail();
                if (emailPattern != null && emailPattern.length() > 0 &&
                    userEmail != null && userEmail.length() > 0) {
                    try {
                        Pattern emailRegexp = Pattern.compile('^' + emailPattern + '$');
                        Matcher m = emailRegexp.matcher(userEmail);
                        return m.matches();
                    } catch (PatternSyntaxException e) {
                        logger.warning("Email pattern '" + emailPattern + "' is not a valid regular expression");
                    }
                }
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
    private FederatedIdentityProvider federatedProvider;
}
