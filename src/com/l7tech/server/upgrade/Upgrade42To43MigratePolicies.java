/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.upgrade;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.service.PublishedService;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class Upgrade42To43MigratePolicies implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade42To43MigratePolicies.class.getName());

    private static final String PUBLISHED_SERVICE_TABLE = "published_service";
    private static final String OBJECTID_COLUMN = "objectid";
    private static final String POLICY_XML_COLUMN = "policy_xml";
    private static final String SOAP_COLUMN = "soap";
    private static final String NAME_COLUMN = "name";

    private static final String SQL_SANITY_CHECK =
            "SELECT COUNT(*) FROM " + PUBLISHED_SERVICE_TABLE +
                    " WHERE " + POLICY_XML_COLUMN + " IS NOT NULL";

    private static final String SQL_GET_POLICY_XML =
            "SELECT " + OBJECTID_COLUMN + ", " +
                    POLICY_XML_COLUMN + "," +
                    SOAP_COLUMN + "," +
                    NAME_COLUMN + " FROM " + PUBLISHED_SERVICE_TABLE;

    private static final String SQL_CLEAR_POLICY_XML =
            "UPDATE " + PUBLISHED_SERVICE_TABLE +
            " SET " + POLICY_XML_COLUMN + " = NULL";

    public void upgrade(ApplicationContext spring) throws NonfatalUpgradeException, FatalUpgradeException {
        SessionFactory sessionFactory;
        Session session;
        try {
            sessionFactory = (SessionFactory)spring.getBean("sessionFactory");
            if (sessionFactory == null) throw new FatalUpgradeException("Couldn't get required components (sessionFactory)");
            session = sessionFactory.getCurrentSession();
            if (session == null) throw new FatalUpgradeException("Couldn't get required components (session)");
        } catch (BeansException e) {
            throw new FatalUpgradeException("Couldn't get required components");
        }

        Map<Long, Policy> policies = collectPolicies(session);
        createPolicyRecords(policies, session);
        clearOldPolicies(session);
        // session must dangle open; caller will commit transaction, which will flush the session
    }

    /**
     * Needs to use JDBC, since the PublishedService class has lost its policyXml property
     */
    private Map<Long, Policy> collectPolicies(Session session) throws FatalUpgradeException, NonfatalUpgradeException {
        Statement stmt = null;
        ResultSet rs = null;

        Map<Long, Policy> policies = new HashMap<Long, Policy>();
        try {
            stmt = session.connection().createStatement();

            try {
                stmt.executeQuery(SQL_SANITY_CHECK);
            } catch (SQLException e) {
                throw new NonfatalUpgradeException("Upgrade task cannot be performed on this database--no " +
                        PUBLISHED_SERVICE_TABLE + "." + POLICY_XML_COLUMN + " column", e);
            }

            rs = stmt.executeQuery(SQL_GET_POLICY_XML);

            while (rs.next()) {
                long oid = rs.getLong(1);
                Clob clob = rs.getClob(2);
                boolean soap = rs.getInt(3) == 1;
                String name = rs.getString(4);
                if (clob == null) continue; // Maybe someone else has already gotten here

                Reader xmlReader = clob.getCharacterStream();
                StringWriter sw = new StringWriter();
                try {
                    HexUtils.copyReader(xmlReader, sw);
                } catch (IOException e) {
                    throw new FatalUpgradeException("Couldn't read policy_xml field");
                }
                PublishedService service = new PublishedService();
                service.setOid( oid );
                service.setName( name );
                policies.put(oid, new Policy(PolicyType.PRIVATE_SERVICE, service.generatePolicyName(), sw.getBuffer().toString(), soap));
            }

            return policies;
        } catch (SQLException e) {
            throw new FatalUpgradeException("Couldn't get PublishedService policies", e);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(stmt);
        }
    }

    private void clearOldPolicies(Session session) throws FatalUpgradeException {
        Statement stmt = null;
        try {
            stmt = session.connection().createStatement();
            stmt.executeUpdate(SQL_CLEAR_POLICY_XML);
        } catch (SQLException e) {
            throw new FatalUpgradeException("Couldn't clear old service policies", e);
        } finally {
            ResourceUtils.closeQuietly(stmt);
        }
    }

    private void createPolicyRecords(Map<Long, Policy> policies, Session session) throws FatalUpgradeException {
        for (Map.Entry<Long, Policy> entry : policies.entrySet()) {
            long serviceOid = entry.getKey();

            PublishedService service;
            try {
                service = (PublishedService) session.load(PublishedService.class, serviceOid);
                if (service == null) {
                    logger.log(Level.SEVERE, "Service #" + serviceOid + " has been deleted; skipping");
                    continue;
                }
            } catch (HibernateException e) {
                throw new FatalUpgradeException("Couldn't find Service #" + serviceOid, e);
            }

            Policy policy = entry.getValue();
            try {
                session.save(policy);
                service.setPolicy(policy);
                session.update(service);
            } catch (Exception e) {
                throw new FatalUpgradeException("Couldn't save new Policy for Service #" + serviceOid);
            }
        }
    }
}
