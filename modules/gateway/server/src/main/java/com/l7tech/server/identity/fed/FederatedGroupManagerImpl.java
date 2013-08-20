/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.fed.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.identity.PersistentGroupManagerImpl;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
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
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class FederatedGroupManagerImpl
        extends PersistentGroupManagerImpl<FederatedUser, FederatedGroup, FederatedUserManager, FederatedGroupManager>
        implements FederatedGroupManager
{
    private FederatedIdentityProvider federatedProvider;
    private FederatedIdentityProviderConfig providerConfig;

    private static final Logger logger = Logger.getLogger(FederatedGroupManagerImpl.class.getName());

    public FederatedGroupManagerImpl() {
    }

    @Override
    public void configure(final FederatedIdentityProvider provider) {
        this.setIdentityProvider( provider );
        federatedProvider = provider;
        providerConfig = (FederatedIdentityProviderConfig)federatedProvider.getConfig();
    }

    @Override
    public FederatedGroup reify(GroupBean bean) {
        FederatedGroup fg = new FederatedGroup(bean.getProviderId(), bean.getName(), bean.getProperties());
        fg.setDescription(bean.getDescription());
        fg.setGoid(bean.getId() == null ? FederatedGroup.DEFAULT_GOID : Goid.parseGoid(bean.getId()));
        return fg;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public GroupMembership newMembership(FederatedGroup group, FederatedUser user) {
        FederatedGroup fgroup = cast(group);
        return new FederatedGroupMembership(providerConfig.getGoid(), fgroup.getGoid(), Goid.parseGoid(user.getId()));
    }

    @Override
    protected Class getMembershipClass() {
        return FederatedGroupMembership.class;
    }

    @Override
    protected void preSave(FederatedGroup group) throws SaveException {
        if ( group instanceof VirtualGroup && providerConfig.getTrustedCertGoids().length == 0 )
            throw new NoTrustedCertsSaveException("Virtual groups cannot be created in a Federated Identity Provider with no Trusted Certificates");
    }

    @Override
    protected void addFindByNameCriteria(Criteria crit) {
        crit.add(Restrictions.eq("providerId", getProviderGoid()));
    }

    @Override
    protected void addFindAllCriteria( Criteria allHeadersCriteria ) {
        allHeadersCriteria.add(Restrictions.eq("providerId", getProviderGoid()));
    }

    @Override
    @Transactional(readOnly=true)
    public FederatedGroup findByPrimaryKey( String oid ) throws FindException {
        try {
            FederatedGroup g = super.findByPrimaryKey(oid);
            if ( g == null ) {
                g = findByPrimaryKey(VirtualGroup.class, Goid.parseGoid(oid));
                if (g != null) {
                    g.setProviderId(providerConfig.getGoid());
                }
            }
            return g;
        } catch (IllegalArgumentException nfe) {
            throw new FindException("Can't find groups with malformed GOIDs!", nfe);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public boolean isMember(User user, FederatedGroup genericGroup) throws FindException {
        FederatedGroup group = cast(genericGroup);
        if ( group instanceof VirtualGroup ) {
            // fix for bugzilla 1101 virtual group membership enforces trusted fip cert
            Set provCerts = federatedProvider.getValidTrustedCertOids();
            if (provCerts == null || provCerts.size() < 1) {
                logger.warning("The virtual group " + group.getId() + " is itself invalid because " +
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
                        if ( userDn != null && userDn.length() > 0 && CertUtils.dnMatchesPattern( user.getSubjectDn(), dnPattern, ((VirtualGroup)group).isUseRegex() ) )
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
            return super.isMember(user, group);
        }
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final FederatedGroup entity) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("providerId", entity.getProviderId());
        map.put("name", entity.getName());
        return Collections.singletonList(map);
    }

    @Override
    protected void addMembershipCriteria(Criteria crit, Group group, Identity identity) {
        crit.add(Restrictions.eq("thisGroupProviderGoid", group.getProviderId()));
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public FederatedGroup cast(Group group) {
        if ( group instanceof GroupBean ) {
            return reify((GroupBean) group);
        } else {
            return (FederatedGroup)group;
        }
    }

    @Override
    public Class<FederatedGroup> getImpClass() {
        return FederatedGroup.class;
    }

    @Override
    public Class<Group> getInterfaceClass() {
        return Group.class;
    }

    @Override
    public String getTableName() {
        return "fed_group";
    }
}
