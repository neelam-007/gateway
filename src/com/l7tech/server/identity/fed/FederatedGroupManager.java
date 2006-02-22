/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedGroupMembership;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.identity.PersistentGroupManager;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

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
    private FederatedIdentityProvider federatedProvider;
    private FederatedIdentityProviderConfig providerConfig;

    private static final Logger logger = Logger.getLogger(FederatedGroupManager.class.getName());

    public FederatedGroupManager(IdentityProvider identityProvider) {
        super(identityProvider);
        if (identityProvider instanceof FederatedIdentityProvider) {
            federatedProvider = (FederatedIdentityProvider)identityProvider;
            providerConfig = (FederatedIdentityProviderConfig)federatedProvider.getConfig();
        } else {
            throw new IllegalArgumentException("Expected Federated Provider");
        }
    }

    /**
     * empty subclassing constructor (required for class proxying)
     */
    protected FederatedGroupManager() {
    }

    public GroupMembership newMembership(Group group, User user) {
        FederatedGroup fgroup = (FederatedGroup)cast(group);
        return new FederatedGroupMembership(providerConfig.getOid(), fgroup.getOid(), Long.parseLong(user.getUniqueIdentifier()));
    }

    protected Class getMembershipClass() {
        return FederatedGroupMembership.class;
    }

    protected void preSave( PersistentGroup group ) throws SaveException {
        if ( group instanceof VirtualGroup && providerConfig.getTrustedCertOids().length == 0 )
            throw new SaveException("Virtual groups cannot be created in a Federated Identity Provider with no Trusted Certificates");
    }

    protected void addFindAllCriteria( Criteria allHeadersCriteria ) {
        allHeadersCriteria.add(Restrictions.eq("providerId", new Long(getProviderOid())));
    }

    public Group findByPrimaryKey( String oid ) throws FindException {
        try {
            Group g = super.findByPrimaryKey(oid);
            if ( g == null ) {
                g = (PersistentGroup)findByPrimaryKey(VirtualGroup.class, Long.parseLong(oid));
                if (g != null) {
                    g.getGroupBean().setProviderId(providerConfig.getOid());
                }
            }
            return g;
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
            if ( providerConfig.isX509Supported() || providerConfig.isSamlSupported()) {
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

            if (providerConfig.isSamlSupported()) {
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

    protected void addMembershipCriteria(Criteria crit, Group group, Identity identity) {
        crit.add(Restrictions.eq("thisGroupProviderOid", new Long(group.getProviderId())));
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
}
