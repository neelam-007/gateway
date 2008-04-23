package com.l7tech.policy.migrator;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.*;
import java.util.Properties;
import java.util.Enumeration;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet4Address;

/**
 * Created by IntelliJ IDEA.
 * User: jules
 * Date: Apr 4, 2008
 * <p/>
 * This class is used to migrate policy settings contained in the AutoTest db to their IntegrationTest counterparts.
 */

public class PolicyMigrator {

    private static final String PROPERTIES_FILENAME = "policy_migrator.properties";


    private static final String DB_HOSTNAME_KEY = "policy.migrator.db.host";
    private static final String DB_NAME_KEY = "policy.migrator.db.name";
    private static final String DB_USERNAME_KEY = "policy.migrator.db.username";
    private static final String DB_PASSWORD_KEY = "policy.migrator.db.password";

    private static final String BRA_URIS_KEY = "policy.migrator.bra.uris";
    private static final String PRIVATE_KEYS_URIS_KEY = "policy.migrator.privatekey.uris";
    private static final String EMAIL_ALERT_URIS_KEY = "policy.migrator.emailalert.uris";
    private static final String IP_MATCH_URIS_KEY = "policy.migrator.ip.match.uris";
    private static final String IP_RANGE_URIS_KEY = "policy.migrator.ip.range.uris";
    private static final String USER_URIS_KEY = "policy.migrator.user.uris";

    private static final String BRA_ROUTE_HOSTNAME_KEY = "policy.migrator.bra.hostname";
    private static final String PRIVATE_KEY_HOSTNAME_KEY = "policy.migrator.privatekey.hostname";
    private static final String EMAIL_ALERT_ADDRESS_KEY = "policy.migrator.emailalert.address";
    private static final String EMAIL_ALERT_SMTP_HOST_KEY = "policy.migrator.emailalert.smtpHost";
    private static final String USER_HOSTNAME_KEY = "policy.migrator.user.hostname";

    private Properties properties = null;
    private Connection dbConnection = null;

    private static final String basePolicyXmlQuery = "SELECT p.xml FROM policy p, published_service ps WHERE ps.policy_oid=p.objectid";


    public static void main(String[] args) {

        new PolicyMigrator().run();
    }

    public PolicyMigrator() {
        loadProperties();

        // configure the database access properties
        String dbHostname = properties.getProperty(PolicyMigrator.DB_HOSTNAME_KEY);
        String dbName = properties.getProperty(PolicyMigrator.DB_NAME_KEY);
        String dbUsername = properties.getProperty(PolicyMigrator.DB_USERNAME_KEY);
        String dbPassword = properties.getProperty(PolicyMigrator.DB_PASSWORD_KEY);

        //setup the database connection
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            dbConnection = DriverManager.getConnection("jdbc:mysql://" + dbHostname + "/" + dbName, dbUsername, dbPassword);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private Properties getProperties() {
        if (properties == null) {
            loadProperties();
        }
        return properties;
    }

    private void loadProperties() {
        properties = new Properties();
        try {
            properties.load(new FileReader("src/" + PolicyMigrator.PROPERTIES_FILENAME));
        } catch (IOException e) {
            System.err.println("Failed to load properties file: " + PolicyMigrator.PROPERTIES_FILENAME);
            System.exit(-1);
        }
    }

    private void run() {
        updateBraRoutes();
        updatePrivateKeyRoutes();
        updateEmailAlerts();
        updateIpRestrictedServices();
        updateTestMachineUserPolicies();
    }

    private String[] getBraUris() {
        return getProperties().getProperty(PolicyMigrator.BRA_URIS_KEY).split(",");
    }

    private String[] getPrivateKeyUris() {
        return getProperties().getProperty(PolicyMigrator.PRIVATE_KEYS_URIS_KEY).split(",");
    }

    private String[] getEmailAlertUris() {
        return getProperties().getProperty(PolicyMigrator.EMAIL_ALERT_URIS_KEY).split(",");
    }

    private String[] getIpMatchUris() {
        return getProperties().getProperty(PolicyMigrator.IP_MATCH_URIS_KEY).split(",");
    }

    private String[] getIpRangeUris() {
        return getProperties().getProperty(PolicyMigrator.IP_RANGE_URIS_KEY).split(",");
    }

    private String[] getTestMachineUserUris() {
        return getProperties().getProperty(PolicyMigrator.USER_URIS_KEY).split(",");
    }

    private void updateBraRoutes() {
        String[] uris = getBraUris();

        for (String uri : uris) {
            uri = uri.trim();
            String querySql = PolicyMigrator.basePolicyXmlQuery + " AND ps.routing_uri='" + uri + "'";
            Document policy;
            try {
                //get the current policy Document
                policy = getPolicyDocument(querySql);
                updateBraRoute(policy);

                //insert the updated policy Document back into the database
                String updatedXml = getStringFromDocument(policy);
                String updateSql = "UPDATE policy, published_service SET policy.xml='" + updatedXml + "' WHERE published_service.policy_oid=policy.objectid" + " AND published_service.routing_uri='" + uri + "'";
                insertUpdatedPolicy(updateSql);
            } catch (IllegalStateException ise) {
                System.err.println("Error getting policy xml for URI: " + uri);
            }
        }
    }

    private void updateBraRoute(Document dom) {
        String oldHostnameFullyQualified = "ssgautotest1.l7tech.com";
        String oldHostname = "ssgautotest1";

        String tagName = "L7p:ProtectedServiceUrl";
        String newHostname = properties.getProperty(PolicyMigrator.BRA_ROUTE_HOSTNAME_KEY);

        NodeList elements = dom.getElementsByTagName(tagName);
        if (elements.getLength() != 1) {
            //this should never happen
            System.err.println("Policy contains " + elements.getLength() + " element(s) of type: " + tagName + ".");
            System.exit(-1);
        }
        Node node = elements.item(0);
        Element element = (Element) node;
        String route = element.getAttribute("stringValue");

        String updatedRoute;
        if (route.contains(oldHostnameFullyQualified)) {
            updatedRoute = route.replace(oldHostnameFullyQualified, newHostname);
            element.setAttribute("stringValue", updatedRoute);
        } else if (route.contains(oldHostname)) {
            updatedRoute = route.replace(oldHostname, newHostname);
            element.setAttribute("stringValue", updatedRoute);
        } else {
            //should never get here, unless AutoTest changes its Bra endpoin URL
            System.out.println("Unexpected hostname in BRA URL. Skipping...");
        }
    }

    private void updatePrivateKeyRoutes() {
        String[] uris = getPrivateKeyUris();

        for (String uri : uris) {
            uri = uri.trim();
            String querySql = PolicyMigrator.basePolicyXmlQuery + " AND ps.routing_uri='" + uri + "'";
            Document policy;
            try {
                //get the current policy Document
                policy = getPolicyDocument(querySql);
                updatePrivateKeyRoute(policy);

                //insert the updated policy Document back into the database
                String updatedXml = getStringFromDocument(policy);
                String updateSql = "UPDATE policy, published_service SET policy.xml='" + updatedXml + "' WHERE published_service.policy_oid=policy.objectid" + " AND published_service.routing_uri='" + uri + "'";
                insertUpdatedPolicy(updateSql);
            } catch (IllegalStateException ise) {
                System.err.println("Error getting policy xml for URI: " + uri);
            }
        }
    }

    private void updatePrivateKeyRoute(Document dom) {
        String oldHostnameFullyQualified = "autotest.l7tech.com";
        String oldHostname = "autotest";

        String tagName = "L7p:ProtectedServiceUrl";
        String newHostname = properties.getProperty(PolicyMigrator.PRIVATE_KEY_HOSTNAME_KEY);

        NodeList elements = dom.getElementsByTagName(tagName);
        if (elements.getLength() != 1) {
            //this should never happen
            System.err.println("Policy contains " + elements.getLength() + " element(s) of type: " + tagName + ".");
            System.exit(-1);
        }
        Node node = elements.item(0);
        Element element = (Element) node;
        String route = element.getAttribute("stringValue");

        String updatedRoute;
        if (route.contains(oldHostnameFullyQualified)) {
            updatedRoute = route.replace(oldHostnameFullyQualified, newHostname);
            element.setAttribute("stringValue", updatedRoute);
        } else if (route.contains(oldHostname)) {
            updatedRoute = route.replace(oldHostname, newHostname);
            element.setAttribute("stringValue", updatedRoute);
        } else {
            //should never get here, unless AutoTest changes its private key endpoint URL
            System.out.println("Unexpected hostname in private key URL. Skipping...");
        }
    }

    private void updateEmailAlert(Document dom) {
        String[] oldEmailAddresses = new String[] {"ssgautotest@layer7tech.com", "mqiu@layer7tech.com", "jchen@l7tech.com"};
        String tagName = "L7p:TargetEmailAddress";
        String newEmailAddress = properties.getProperty(PolicyMigrator.EMAIL_ALERT_ADDRESS_KEY);

        NodeList elements = dom.getElementsByTagName(tagName);
        for(int i = 0; i < elements.getLength(); i++){
            Node node = elements.item(i);
            Element element = (Element)node;
            String emailAddress = element.getAttribute("stringValue");
            String updatedEmailAddress = null;
            for(String oldEmailAddress : oldEmailAddresses) {
                updatedEmailAddress = emailAddress.replace(oldEmailAddress, newEmailAddress);
                if(!emailAddress.equals(updatedEmailAddress)) {
                    element.setAttribute("stringValue", updatedEmailAddress);
                    break;
                }
            }
        }

        tagName = "L7p:EmailAlert";
        elements = dom.getElementsByTagName(tagName);
        for(int i = 0;i < elements.getLength();i++) {
            Element element = (Element)elements.item(i);
            NodeList smtpHostElements = element.getElementsByTagName("L7p:SmtpHost");
            if(smtpHostElements.getLength() == 0) {
                Element smtpHostElement = dom.createElement("L7p:SmtpHost");
                smtpHostElement.setAttribute("stringValue", properties.getProperty(PolicyMigrator.EMAIL_ALERT_SMTP_HOST_KEY));
                element.appendChild(smtpHostElement);
            } else {
                for(int j = 0;j < smtpHostElements.getLength();j++) {
                    Element smtpHostElement = (Element)smtpHostElements.item(j);
                    smtpHostElement.setAttribute("stringValue", properties.getProperty(PolicyMigrator.EMAIL_ALERT_SMTP_HOST_KEY));
                }
            }
        }
    }

    private void updateEmailAlerts() {
        String[] uris = getEmailAlertUris();


        for (String uri : uris) {
            uri = uri.trim();
            String querySql = PolicyMigrator.basePolicyXmlQuery + " AND ps.routing_uri='" + uri + "'";
            Document policy;
            try {
                //get the current policy Document
                policy = getPolicyDocument(querySql);
                updateEmailAlert(policy);

                //insert the updated policy Document back into the database
                String updatedXml = getStringFromDocument(policy);
                String updateSql = "UPDATE policy, published_service SET policy.xml='" + updatedXml + "' WHERE published_service.policy_oid=policy.objectid" + " AND published_service.routing_uri='" + uri + "'";
                insertUpdatedPolicy(updateSql);
            } catch (IllegalStateException ise) {
                System.err.println("Error getting policy xml for URI: " + uri);
                ise.printStackTrace();
            }
        }
    }

    private void updateIpRestrictedServices() {
        try {
            InetAddress currentIp = null;
            for (Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces(); e1.hasMoreElements();) {
                NetworkInterface ni = e1.nextElement();

                for(Enumeration<InetAddress> e2 = ni.getInetAddresses();e2.hasMoreElements();) {
                    InetAddress address = e2.nextElement();
                    if(address instanceof Inet4Address) {
                        byte[] rawAddress = address.getAddress();
                        if(rawAddress[0] == (byte)192 && rawAddress[1] == (byte)168 && rawAddress[2] == (byte)1) {
                            currentIp = address;
                            break;
                        }
                    }
                }

                if(currentIp != null) {
                    break;
                }
            }

            if(currentIp == null) {
                throw new SocketException();
            }

            String[] uris = getIpMatchUris();
            for (String uri : uris) {
                uri = uri.trim();
                String querySql = PolicyMigrator.basePolicyXmlQuery + " AND ps.routing_uri='" + uri + "'";
                Document policy;
                try {
                    //get the current policy Document
                    policy = getPolicyDocument(querySql);
                    updateIpMatchPolicy(policy, currentIp);

                    //insert the updated policy Document back into the database
                    String updatedXml = getStringFromDocument(policy);
                    String updateSql = "UPDATE policy, published_service SET policy.xml='" + updatedXml + "' WHERE published_service.policy_oid=policy.objectid" + " AND published_service.routing_uri='" + uri + "'";
                    insertUpdatedPolicy(updateSql);
                } catch (IllegalStateException ise) {
                    System.err.println("Error getting policy xml for URI: " + uri);
                    ise.printStackTrace();
                }
            }

            uris = getIpRangeUris();
            for (String uri : uris) {
                uri = uri.trim();
                String querySql = PolicyMigrator.basePolicyXmlQuery + " AND ps.routing_uri='" + uri + "'";
                Document policy;
                try {
                    //get the current policy Document
                    policy = getPolicyDocument(querySql);
                    updateIpRangePolicy(policy, currentIp);

                    //insert the updated policy Document back into the database
                    String updatedXml = getStringFromDocument(policy);
                    String updateSql = "UPDATE policy, published_service SET policy.xml='" + updatedXml + "' WHERE published_service.policy_oid=policy.objectid" + " AND published_service.routing_uri='" + uri + "'";
                    insertUpdatedPolicy(updateSql);
                } catch (IllegalStateException ise) {
                    System.err.println("Error getting policy xml for URI: " + uri);
                    ise.printStackTrace();
                }
            }
        } catch(SocketException e) {
            System.err.println("!!!! Failed to retrieve the IP address of the test machine.");
            System.exit(1);
        }
    }

    private void updateIpMatchPolicy(Document dom, InetAddress currentIp) {
        String tagName = "L7p:StartIp";

        NodeList elements = dom.getElementsByTagName(tagName);
        if (elements.getLength() != 1) {
            //this should never happen
            System.err.println("Policy contains " + elements.getLength() + " element(s) of type: " + tagName + ".");
            System.exit(-1);
        }
        Node node = elements.item(0);
        Element element = (Element) node;
        element.setAttribute("stringValue", currentIp.toString().substring(1));
    }

    private void updateIpRangePolicy(Document dom, InetAddress currentIp) {
        String tagName = "L7p:StartIp";

        NodeList elements = dom.getElementsByTagName(tagName);
        if (elements.getLength() != 1) {
            //this should never happen
            System.err.println("Policy contains " + elements.getLength() + " element(s) of type: " + tagName + ".");
            System.exit(-1);
        }
        Node node = elements.item(0);
        Element element = (Element) node;
        if(currentIp.getAddress()[3] >= (byte)0) {
            element.setAttribute("stringValue", "192.168.1.0");
        } else {
            element.setAttribute("stringValue", "192.168.1.128");
        }
    }

    private void updateTestMachineUserPolicy(Document dom) {
        String userTagName = "L7p:SpecificUser";
        String loginTagName = "L7p:UserLogin";
        String usernameTagName = "L7p:UserName";

        String oldHostnameFullyQualified = "autotest.l7tech.com";
        String newHostname = properties.getProperty(PolicyMigrator.USER_HOSTNAME_KEY);
        
        NodeList elements = dom.getElementsByTagName(userTagName);
        for(int i = 0;i < elements.getLength();i++) {
            Element userElement = (Element)elements.item(i);

            NodeList childElements = userElement.getElementsByTagName(loginTagName);
            if(childElements.getLength() > 0) {
                Element childElement = (Element)childElements.item(0);
                if(childElement.getAttribute("stringValue").equals(oldHostnameFullyQualified)) {
                    childElement.setAttribute("stringValue", newHostname);
                }
            }

            childElements = userElement.getElementsByTagName(usernameTagName);
            if(childElements.getLength() > 0) {
                Element childElement = (Element)childElements.item(0);
                if(childElement.getAttribute("stringValue").equals(oldHostnameFullyQualified)) {
                    childElement.setAttribute("stringValue", newHostname);
                }
            }
        }
    }

    private void updateTestMachineUserPolicies() {
        String[] uris = getTestMachineUserUris();

        for (String uri : uris) {
            uri = uri.trim();
            String querySql = PolicyMigrator.basePolicyXmlQuery + " AND ps.routing_uri='" + uri + "'";
            Document policy;
            try {
                //get the current policy Document
                policy = getPolicyDocument(querySql);
                updateTestMachineUserPolicy(policy);

                //insert the updated policy Document back into the database
                String updatedXml = getStringFromDocument(policy);
                String updateSql = "UPDATE policy, published_service SET policy.xml='" + updatedXml + "' WHERE published_service.policy_oid=policy.objectid" + " AND published_service.routing_uri='" + uri + "'";
                insertUpdatedPolicy(updateSql);
            } catch (IllegalStateException ise) {
                System.err.println("Error getting policy xml for URI: " + uri);
            }
        }
    }

    private void insertUpdatedPolicy(String sql) {
        PreparedStatement ps;
        try {
            ps = dbConnection.prepareStatement(sql);
            ps.executeUpdate();

        } catch (SQLException se) {
            System.err.println(se.getMessage());
            se.printStackTrace();
            System.exit(-1);
        }
    }

    private Document getPolicyDocument(String sql) throws IllegalStateException {
        Statement stmnt = null;
        ResultSet rs;
        String policyXml = null;
        try {
            stmnt = dbConnection.createStatement();
            rs = stmnt.executeQuery(sql);

            if (!rs.next() || !rs.isFirst() || !rs.isLast()) {
                throw new IllegalStateException("Could not get policy xml.");
            }
            policyXml = rs.getString("xml");
        } catch (SQLException se) {
            System.err.println(se.getMessage());
            se.printStackTrace();
            System.exit(-1);
        } finally {
            if (stmnt != null) {
                try {
                    stmnt.close();
                } catch (SQLException e) {
                    //do nothing
                }
            }
        }

        return getDocument(policyXml);
    }

    private Document getDocument(String xml) {
        Document dom = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            dom = builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

        return dom;
    }

    private String getStringFromDocument(Document doc) {
        StringWriter writer = null;
        try {
            DOMSource domSource = new DOMSource(doc);
            writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
        }
        catch (TransformerException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        return writer.toString();
    }
}
