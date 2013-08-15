package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordSigner;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
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
public class Upgrade71To80AuditRecords implements UpgradeTask {

    protected ApplicationContext applicationContext;
    protected SessionFactory sessionFactory;
    protected IdentityProviderConfigManager identityProviderConfigManager;
    protected List<Goid> federatedProviderGoids;

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        this.applicationContext = applicationContext;
        sessionFactory = getBean("sessionFactory", SessionFactory.class);

        final long fed_user_prefix = ServerGoidUpgradeMapper.getPrefix("fed_user");
        final long internal_user_prefix = ServerGoidUpgradeMapper.getPrefix("internal_user");

        identityProviderConfigManager = getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        federatedProviderGoids = new ArrayList<Goid>();

        final PrivateKey pk;
        try{
            DefaultKey defaultKey = getBean("defaultKey",DefaultKey.class);
            SignerInfo signerInfo = defaultKey.getAuditSigningInfo();
            if (signerInfo == null) signerInfo = defaultKey.getSslInfo();

            pk = signerInfo.getPrivate();
        } catch (UnrecoverableKeyException e) {
            throw new FatalUpgradeException("Could not retrieve key", e);
        } catch (IOException e) {
            throw new FatalUpgradeException("Could not retrieve key", e);
        }


        try {
            for(IdentityProviderConfig idProvider: identityProviderConfigManager.findAll()){
                if(IdentityProviderType.FEDERATED.equals(idProvider.type()))
                    federatedProviderGoids.add(idProvider.getGoid());
            }
        } catch (FindException e) {
            throw new FatalUpgradeException("Could not retrieve identity providers", e);
        }



        //  audit_main.user_id
        try {
            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(AuditRecord.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof AuditRecord) {
                            AuditRecord auditRecord = (AuditRecord) schemaCriteriaObj;
                            Goid providerId = auditRecord.getIdentityProviderGoid();
                            boolean updated = false;
                            if(isInternal(providerId) ){
                                auditRecord.setUserId(Goid.toString(new Goid(internal_user_prefix, Long.parseLong(auditRecord.getUserId()))));
                            }
                            else if(isFederated(providerId)){
                                auditRecord.setUserId(Goid.toString(new Goid(fed_user_prefix, Long.parseLong(auditRecord.getUserId()))));
                            }
                            if(updated){
                                try {
                                    new AuditRecordSigner(pk).signAuditRecord(auditRecord);
                                } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new NonfatalUpgradeException("Could not update audit records", e);
        }
    }

    private boolean isFederated(Goid providerId){
        return federatedProviderGoids.contains(providerId);
    }

    private boolean isInternal(Goid providerId) {
        return IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.equals(providerId);
    }

    /**
     * Get a bean safely.
     *
     * @param name      the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException if there is no application context or the requested bean was not found
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
