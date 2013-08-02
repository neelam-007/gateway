/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.upgrade;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * A database upgrade task that creates an initial version for all existing policies.
 */
public class Upgrade42To43AddInitialPolicyVersions implements UpgradeTask {
    protected static final Logger logger = Logger.getLogger(Upgrade42To43AddInitialPolicyVersions.class.getName());
    private ApplicationContext applicationContext;

    private Object getBean(String name) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        Object bean = applicationContext.getBean(name);
        if (bean == null) throw new FatalUpgradeException("No bean " + name + " is available");
        return bean;
    }

    public void upgrade(ApplicationContext applicationContext) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;
        Session session;
        try {
            session = ((SessionFactory)getBean("sessionFactory")).getCurrentSession();
            if (session == null)
                throw new FatalUpgradeException("Couldn't get required components (session)");
        } catch (BeansException e) {
            throw new FatalUpgradeException("Couldn't get required components");
        }

        PolicyManager policyManager = (PolicyManager)getBean("policyManager");
        PolicyVersionManager policyVersionManager = (PolicyVersionManager)getBean("policyVersionManager");

        try {
            // Create initial version in policy_version table for policies which lack any versions
            for ( Policy policy : policyManager.findAll()) {
                Integer count = (Integer) session.createQuery("select count(*) from PolicyVersion where policyGoid = :policyGoid")
                        .setParameter("policyGoid", policy.getGoid())
                        .uniqueResult();
                
                if (count != null && count < 1) {
                    logger.info("Creating initial policy revision checkpoint for policy oid " + policy.getGoid() + " (" + policy.getName() + ')');
                    PolicyVersion version = new PolicyVersion();
                    version.setPolicyGoid(policy.getGoid());
                    version.setActive(true);
                    version.setOrdinal(policy.getVersion() - 1);
                    version.setXml(policy.getXml());
                    version.setTime(System.currentTimeMillis());
                    policyVersionManager.save(version);
                }
            }
        } catch (FindException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        } catch (SaveException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }
    }
}