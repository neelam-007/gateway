package com.l7tech.server.upgrade;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.hibernate.SessionFactory;
import org.hibernate.Session;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 8-May-2008
 * Time: 12:16:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class Upgrade44To45SwitchPolicyIncludesToGuids implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade42To43MigratePolicies.class.getName());

    private static final String POLICY_TABLE = "policy";
    private static final String OBJECTID_COLUMN = "objectid";
    private static final String NAME_COLUMN = "name";
    private static final String GUID_COLUMN = "guid";
    private static final String POLICY_XML_COLUMN = "xml";

    private static final String SQL_GET_POLICY_NAME =
            "SELECT " + OBJECTID_COLUMN + ", " +
                    NAME_COLUMN + " FROM " + POLICY_TABLE;

    private static final String SQL_GET_POLICY_XML =
            "SELECT " + OBJECTID_COLUMN + ", " +
                    POLICY_XML_COLUMN + " FROM " + POLICY_TABLE;

    private static final String SQL_SET_GUID =
            "UPDATE " + POLICY_TABLE +
                    " SET " + GUID_COLUMN + " = ? WHERE " +
                    OBJECTID_COLUMN + " = ?";

    private static final String SQL_UPDATE_POLICY_XML =
            "UPDATE " + POLICY_TABLE +
            " SET " + POLICY_XML_COLUMN + " = ? WHERE " +
            OBJECTID_COLUMN + " = ?";

    private static final String SQL_ADD_GUID_UNIQUE_KEY =
            "ALTER TABLE " + POLICY_TABLE +
                    " ADD UNIQUE KEY i_guid (" + GUID_COLUMN +  ")";

    private HashMap<Long, String> oidToGuidMap = new HashMap<Long, String>();

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

        generateGuids(session);
        migratePolicies(session);
        addUniqueKeyConstraint(session);
    }

    private void generateGuids(Session session) throws FatalUpgradeException {
        Statement stmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            stmt = session.connection().createStatement();
            rs = stmt.executeQuery(SQL_GET_POLICY_NAME);

            updateStmt = session.connection().prepareStatement(SQL_SET_GUID);
            while(rs.next()) {
                long policyOid = rs.getLong(1);
                String name = rs.getString(2);

                String uuidName = Long.toString(policyOid) + "#" + name;
                UUID guid = UUID.nameUUIDFromBytes(uuidName.getBytes());
                updateStmt.setLong(2, policyOid);
                updateStmt.setString(1, guid.toString());
                updateStmt.execute();

                oidToGuidMap.put(policyOid, guid.toString());
            }
        } catch(SQLException e) {
            throw new FatalUpgradeException("Failed to set the GUID values for the policies", e);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(stmt);
        }
    }

    private void migratePolicies(Session session) throws FatalUpgradeException {
        Statement stmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            stmt = session.connection().createStatement();
            rs = stmt.executeQuery(SQL_GET_POLICY_XML);

            updateStmt = session.connection().prepareStatement(SQL_UPDATE_POLICY_XML);
            while(rs.next()) {
                long policyOid = rs.getLong(1);
                Clob clob = rs.getClob(2);

                if(clob == null) {
                    continue; // Nothing to check
                }

                Reader xmlReader = clob.getCharacterStream();
                StringWriter sw = new StringWriter();
                try {
                    HexUtils.copyReader(xmlReader, sw);
                } catch (IOException e) {
                    throw new FatalUpgradeException("Couldn't read policy_xml field");
                }

                String updatedXml = getMigratedPolicyXml(sw.getBuffer().toString());

                if(updatedXml != null) {
                    updateStmt.setLong(2, policyOid);
                    updateStmt.setString(1, updatedXml);
                    updateStmt.execute();
                }
            }
        } catch(SQLException e) {
            throw new FatalUpgradeException("Failed to policy XML field");
        }
    }

    private String getMigratedPolicyXml(String policyXml) throws FatalUpgradeException {
        try {
            Document document = XmlUtil.parse(new StringReader(policyXml), false);
            NodeList includeElements = document.getElementsByTagName("L7p:Include");
            if(includeElements.getLength() > 0) {
                for(int i = 0;i < includeElements.getLength();i++) {
                    Element includeElement = (Element)includeElements.item(i);

                    NodeList policyOidElements = includeElement.getElementsByTagName("L7p:PolicyOid");
                    if(policyOidElements.getLength() > 0) {
                        Element policyOidElement = (Element)policyOidElements.item(0);
                        String guid = oidToGuidMap.get(new Long(policyOidElement.getAttribute("boxedLongValue")));
                        if(guid != null) {
                            Node child = includeElement.getFirstChild();
                            while(child != null) {
                                includeElement.removeChild(child);
                                child = includeElement.getFirstChild();
                            }
                            Element guidElement = document.createElement("L7p:PolicyGuid");
                            guidElement.setAttribute("stringValue", guid);
                            includeElement.appendChild(guidElement);
                        }
                    }
                }

                return XmlUtil.nodeToFormattedString(document);
            } else {
                return null;
            }
        } catch(Exception e) {
            throw new FatalUpgradeException("Failed to update include assertions", e);
        }
    }

    private void addUniqueKeyConstraint(Session session) throws FatalUpgradeException {
        Statement stmt = null;

        try {
            stmt = session.connection().createStatement();
            stmt.execute(SQL_ADD_GUID_UNIQUE_KEY);
        } catch(SQLException e) {
            throw new FatalUpgradeException("Failed to add GUID unique key constraint to policy table");
        }
    }
}
