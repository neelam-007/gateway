package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.cert.CertEntryRow;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.secureconversation.StoredSecureConversationSession;
import com.l7tech.server.util.ServerGoidUpgradeMapper;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.security.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is used to upgrade the user id references from other entities
 */
public class Upgrade71To80IdProviderReferences implements UpgradeTask {

    protected ApplicationContext applicationContext;
    protected SessionFactory sessionFactory;
    protected IdentityProviderConfigManager identityProviderConfigManager;
    protected List<Goid> federatedProviderGoids;
    protected static String DEFAULT_ADMIN_USER_ID = Goid.toString(new Goid(0,3));

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        this.applicationContext = applicationContext;
        sessionFactory = getBean("sessionFactory", SessionFactory.class);

        final long fed_user_prefix = ServerGoidUpgradeMapper.getPrefix("fed_user");
        final long internal_user_prefix = ServerGoidUpgradeMapper.getPrefix("internal_user");
        final long internal_group_prefix = ServerGoidUpgradeMapper.getPrefix("internal_group");
        final long fed_group_prefix = ServerGoidUpgradeMapper.getPrefix("fed_group");

        identityProviderConfigManager = getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        federatedProviderGoids = new ArrayList<Goid>();


        try {
            for(IdentityProviderConfig idProvider: identityProviderConfigManager.findAll()){
                if(IdentityProviderType.FEDERATED.equals(idProvider.type()))
                    federatedProviderGoids.add(idProvider.getGoid());
            }
        } catch (FindException e) {
            throw new FatalUpgradeException("Could not retrieve identity providers", e);
        }

        // rbac_assignment.identity_id
        try {

            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(RoleAssignment.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof RoleAssignment) {
                            RoleAssignment assignment = (RoleAssignment) schemaCriteriaObj;
                            Goid providerId = assignment.getProviderId();
                            if(isInternal(providerId) ){
                                if(assignment.getEntityType().equals(EntityType.USER.getName())){
                                    assignment.setIdentityId(getInternalUserId(internal_user_prefix, assignment.getIdentityId()));
                                }else if(assignment.getEntityType().equals(EntityType.GROUP.getName())){
                                    assignment.setIdentityId(getInternalUserId(internal_group_prefix, assignment.getIdentityId()));
                                }
                            }
                            else if(isFederated(providerId)){
                                if(assignment.getEntityType().equals(EntityType.USER.getName())){
                                    assignment.setIdentityId(getInternalUserId(fed_user_prefix, assignment.getIdentityId()));
                                }else if(assignment.getEntityType().equals(EntityType.GROUP.getName())){
                                    assignment.setIdentityId(getInternalUserId(fed_group_prefix, assignment.getIdentityId()));
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new FatalUpgradeException("Could not update rbac assignemnt", e);
        }

        // client_cert.user_id
        try {

            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(CertEntryRow.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof CertEntryRow) {
                            CertEntryRow clientCert = (CertEntryRow) schemaCriteriaObj;
                            Goid providerId = clientCert.getProvider();
                            if(isInternal(providerId) ){
                                clientCert.setUserId(getInternalUserId(internal_user_prefix, clientCert.getUserId()));
                            }
                            else if(isFederated(providerId)){
                                clientCert.setUserId(Goid.toString(new Goid(fed_user_prefix,Long.parseLong(clientCert.getUserId()))));
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new FatalUpgradeException("Could not update cert entry row", e);
        }

        //  trusted_esm_user.user_id
        try {

            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(TrustedEsmUser.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof TrustedEsmUser) {
                            TrustedEsmUser esmUser = (TrustedEsmUser) schemaCriteriaObj;
                            Goid providerId = esmUser.getProviderGoid();
                            if(isInternal(providerId) ){
                                esmUser.setSsgUserId( getInternalUserId(internal_user_prefix, esmUser.getSsgUserId()));
                            }
                            else if(isFederated(providerId)){
                                esmUser.setSsgUserId(Goid.toString(new Goid(fed_user_prefix, Long.parseLong(esmUser.getSsgUserId()))));
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new FatalUpgradeException("Could not update trusted esm user", e);
        }

        // message_context_mapping_values.auth_user_id
        try {
            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(MessageContextMappingValues.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof MessageContextMappingValues) {
                            MessageContextMappingValues mappingValues = (MessageContextMappingValues) schemaCriteriaObj;
                            Goid providerId = mappingValues.getAuthUserProviderId();
                            boolean updated = false;
                            if(isInternal(providerId) ){
                                mappingValues.setAuthUserId(getInternalUserId(internal_user_prefix, mappingValues.getAuthUserId()));
                            }
                            else if(isFederated(providerId)){
                                mappingValues.setAuthUserId(Goid.toString(new Goid(fed_user_prefix, Long.parseLong(mappingValues.getAuthUserId()))));
                            }
                            if(updated){
                                mappingValues.setDigested(mappingValues.generateDigest());
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new FatalUpgradeException("Could not update message context mapping values", e);
        }

        // wssc_session.uesr_id
        try {
            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(StoredSecureConversationSession.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof StoredSecureConversationSession) {
                            StoredSecureConversationSession secureConversation = (StoredSecureConversationSession) schemaCriteriaObj;
                            Goid providerId = secureConversation.getProviderId();
                            if(isInternal(providerId) ){
                                secureConversation.setUserId(getInternalUserId(internal_user_prefix,secureConversation.getUserId()));
                            }
                            else if(isFederated(providerId)){
                                secureConversation.setUserId(Goid.toString(new Goid(fed_user_prefix, Long.parseLong(secureConversation.getUserId()))));
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new NonfatalUpgradeException("Could not update secured stored conversation session", e);
        }

    }

    private boolean isFederated(Goid providerId){
        return federatedProviderGoids.contains(providerId);
    }

    private boolean isInternal(Goid providerId) {
        return IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.equals(providerId);
    }

    private String getInternalUserId(long internal_user_prefix, String userIdStr){
        return isDefaultAdministrator(userIdStr)? DEFAULT_ADMIN_USER_ID: Goid.toString(new Goid(internal_user_prefix, Long.parseLong(userIdStr)));
    }
    private boolean isDefaultAdministrator(String userId) {
        return userId.equals("3");
    }

    /**
     * Get a bean safely.
     *
     * @param name      the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws FatalUpgradeException if there is no application context or the requested bean was not found
     */
    @SuppressWarnings({"unchecked"})
    private <T> T getBean(final String name,
                          final Class<T> beanClass) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            return applicationContext.getBean(name, beanClass);
        } catch (BeansException be) {
            throw new FatalUpgradeException("Error accessing  bean '" + name + "' from ApplicationContext.");
        }
    }
}
