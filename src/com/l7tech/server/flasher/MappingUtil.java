package com.l7tech.server.flasher;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.OSSpecificFunctions;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Methods facilitating the mapping of ip addresses between staging SSG instances.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class MappingUtil {
    private static final Logger logger = Logger.getLogger(MappingUtil.class.getName());
    private static final Pattern ipaddresspattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final String STAGINGMAPPINGNS = "http://www.layer7tech.com/migration/stagingmapping";
    private static final String NS_PREFIX = "L7flash";
    private static final String IMPORTMAPPINGELNAME = "ssgimportmapping";
    private static final String BACKENDIPMAPPINGELNAME = "backendipmapping";
    private static final String IPMAPELNAME = "ipmap";
    private static final String SOURCEVALUEATTRNAME = "sourcevalue";
    private static final String TARGETVALUEATTRNAME = "targetvalue";
    private static final String VARMAPELNAME = "varmap";
    private static final String GLOBALVARMAPPINGELNAME = "globalvarmapping";
    private static DBActions dbActions;

    public static class CorrespondanceMap {
        public HashMap<String, String> backendIPMapping = new HashMap<String, String>();
        public HashMap<String, String> varMapping = new HashMap<String, String>();
    }

    public static void applyMappingChangesToDB(OSSpecificFunctions osFunctions, String dburl, String dbuser, String dbpasswd,
                                               CorrespondanceMap mappingResults) throws SQLException {
        Connection c = getConnection(osFunctions, dburl, dbuser, dbpasswd);
        try {
            System.out.println("Applying mappings");
            Statement selStatement = c.createStatement();

            // iterate through policies
            ResultSet selrs = selStatement.executeQuery("select policy_xml, objectid, name from published_service");
            while (selrs.next()) {
                String policy = selrs.getString(1);
                boolean changed = false;
                for (String fromip : mappingResults.backendIPMapping.keySet()) {
                    if (policy.indexOf("stringValue=\"" + fromip + "\"") >= 0) {
                        String toip = mappingResults.backendIPMapping.get(fromip);
                        logger.info("changing " + fromip + " to " + toip + " in service named " + selrs.getString(3));
                        System.out.println("\tchanging " + fromip + " to " + toip + " in service named " + selrs.getString(3));
                        policy = policy.replace("stringValue=\"" + fromip + "\"", "stringValue=\"" + toip + "\"");
                        changed = true;
                    }
                }
                if (changed) {
                    PreparedStatement updateps = c.prepareStatement("update published_service set policy_xml=? where objectid=?");
                    updateps.setString(1, policy);
                    updateps.setLong(2, selrs.getLong(2));
                    updateps.executeUpdate();
                    updateps.close();
                }
            }
            selStatement.close();

            // iterate through global variables
            for (String varname : mappingResults.varMapping.keySet()) {
                String varval = mappingResults.varMapping.get(varname);
                PreparedStatement updateps = c.prepareStatement("update cluster_properties set propvalue=? where propkey=?");
                updateps.setString(1, varval);
                updateps.setString(2, varname);
                int res = updateps.executeUpdate();
                if (res > 0) {
                    logger.info("Setting cluster property " + varname + " to value " + varval);
                    System.out.println("\tSetting cluster property " + varname + " to value " + varval);
                } else {
                    logger.info("Target system does not have cluster property " + varname + ". Ignoring this mapping.");
                    System.out.println("\tTarget system does not have cluster property " + varname + ". Ignoring this mapping.");
                }
                updateps.close();
            }
            System.out.println("Mapping Complete.");
        } finally {
            c.close();
        }
    }
    
    public static CorrespondanceMap loadMapping(String mappingFilePath) throws FlashUtilityLauncher.InvalidArgumentException, IOException, SAXException {
        // load mapping file, validate it and build two maps (one for backends, and one for global variables)
        FileInputStream fis = new FileInputStream(mappingFilePath);
        Document mappingDoc = XmlUtil.parse(fis);
        Element simEl = mappingDoc.getDocumentElement();
        if (!simEl.getLocalName().equals(IMPORTMAPPINGELNAME)) {
            logger.info("Error, provided mapping file is not valid as the root element is not " + IMPORTMAPPINGELNAME);
            throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " is not a valid mapping file");
        }
        if (!simEl.getNamespaceURI().equals(STAGINGMAPPINGNS)) {
            logger.info("Error, provided mapping file is not valid as the root element is not of namespace " + STAGINGMAPPINGNS);
            throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " is not a valid mapping file");
        }
        CorrespondanceMap output = new CorrespondanceMap();
        try {
            Element bimEl = XmlUtil.findOnlyOneChildElementByName(simEl, STAGINGMAPPINGNS, BACKENDIPMAPPINGELNAME);
            List ipmaplist = XmlUtil.findChildElementsByName(bimEl, STAGINGMAPPINGNS, IPMAPELNAME);
            for (Object anIpmaplist : ipmaplist) {
                Element ipmapel = (Element) anIpmaplist;
                String source = ipmapel.getAttribute(SOURCEVALUEATTRNAME);
                String target = ipmapel.getAttribute(TARGETVALUEATTRNAME);
                if (source != null && target != null) {
                    source = extractIpAddressFromString(source);
                    target = extractIpAddressFromString(target);
                }
                if (source == null || target == null) {
                    logger.info("Error, an element " + IPMAPELNAME + " does not contain expected attributes");
                    throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " has invalid values for ipmap element");
                }
                output.backendIPMapping.put(source, target);
            }
            Element gvmEl = XmlUtil.findOnlyOneChildElementByName(simEl, STAGINGMAPPINGNS, GLOBALVARMAPPINGELNAME);
            List varmaplist = XmlUtil.findChildElementsByName(gvmEl, STAGINGMAPPINGNS, VARMAPELNAME);
            for (Object aVarmaplist : varmaplist) {
                Element varmapel = (Element) aVarmaplist;
                String name = varmapel.getAttribute("name");
                String target = varmapel.getAttribute(TARGETVALUEATTRNAME);
                if (name == null || target == null) {
                    logger.info("Error, an element " + VARMAPELNAME + " does not contain expected attributes");
                    throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " has invalid values for varmap element");
                }
                output.varMapping.put(name, target);
            }
        } catch (TooManyChildElementsException e) {
            logger.log(Level.INFO, "error loading mapping file", e);
            throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " is not a valid mapping file. " + e.getMessage());
        }
        return output;
    }

    private static Connection getConnection(OSSpecificFunctions osFunctions, String databaseURL, String databaseUser, String databasePasswd) throws SQLException {
        Connection c;
        c = getDbActions(osFunctions).getConnection(databaseURL, databaseUser, databasePasswd);
        if (c == null) {
            throw new SQLException("could not connect using url: " + databaseURL +
                                   ". with username " + databaseUser +
                                   ", and password: " + databasePasswd);
        }
        return c;
    }

    public static void produceTemplateMappingFileFromDB(OSSpecificFunctions osFunctions, String dburl, String dbuser,
                                                        String dbpasswd, String outputTemplatePath) throws SQLException, SAXException, IOException {

        Connection c = getConnection(osFunctions, dburl, dbuser, dbpasswd);
        ArrayList<String> ipaddressesInRoutingAssertions = new ArrayList<String>();
        HashMap<String, String> mapOfClusterProperties = new HashMap<String, String>();
        try {
            // go through the cluster properties
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("select propkey, propvalue from cluster_properties;");
            while (rs.next()) {
                String value = rs.getString(2);
                String key = rs.getString(1);
                if (!key.equals("license")) mapOfClusterProperties.put(key, value);
            }
            rs.close();
            // go through the policies
            rs = s.executeQuery("select policy_xml from published_service;");
            while (rs.next()) {
                String xml = rs.getString(1);
                Document doc = XmlUtil.stringToDocument(xml);
                List<Element> routingAssertionElements = getRoutingAssertionElementsFromPolicy(doc);
                for (Element ra : routingAssertionElements) {
                    Element cia = XmlUtil.findFirstChildElementByName(ra, "http://www.layer7tech.com/ws/policy",
                                                                          "CustomIpAddresses");
                    List listofitems = XmlUtil.findChildElementsByName(cia, "http://www.layer7tech.com/ws/policy", "item");
                    for (Object listofitem : listofitems) {
                        Element el = (Element) listofitem;
                        String val = el.getAttribute("stringValue");
                        if (val != null && !ipaddressesInRoutingAssertions.contains(val)) {
                            ipaddressesInRoutingAssertions.add(val);
                        }
                    }
                }
            }
            rs.close();
            s.close();
        } finally {
            c.close();
        }
        Document outputdoc = XmlUtil.createEmptyDocument(IMPORTMAPPINGELNAME, NS_PREFIX,
                                                         STAGINGMAPPINGNS);
        Comment comment = outputdoc.createComment("Please review backend ip addresses and global variables" +
                                                 "\n\tand provide corresponding values for the target system");
        outputdoc.getDocumentElement().appendChild(comment);
        Element backendipmappingEl = XmlUtil.createAndAppendElementNS(outputdoc.getDocumentElement(),
                                                                      BACKENDIPMAPPINGELNAME,
                                                                      STAGINGMAPPINGNS,
                                                                      NS_PREFIX);
        for (String routingip : ipaddressesInRoutingAssertions) {
            Element ipmapel = XmlUtil.createAndAppendElementNS(backendipmappingEl, IPMAPELNAME,
                    STAGINGMAPPINGNS,
                    NS_PREFIX);
            ipmapel.setAttribute(SOURCEVALUEATTRNAME, routingip);
            ipmapel.setAttribute(TARGETVALUEATTRNAME, "__add_your_value__");

        }

        Element globalvarmappingEl = XmlUtil.createAndAppendElementNS(outputdoc.getDocumentElement(),
                                                                      GLOBALVARMAPPINGELNAME,
                                                                      STAGINGMAPPINGNS,
                                                                      NS_PREFIX);

        for (String propKey: mapOfClusterProperties.keySet()) {
            Element varmapEl = XmlUtil.createAndAppendElementNS(globalvarmappingEl, VARMAPELNAME,
                    STAGINGMAPPINGNS,
                    NS_PREFIX);
            varmapEl.setAttribute("name", propKey);
            varmapEl.setAttribute(SOURCEVALUEATTRNAME, mapOfClusterProperties.get(propKey));
            varmapEl.setAttribute(TARGETVALUEATTRNAME, "__add_your_value__");
        }

        System.out.print("Outputing template mapping file at " + outputTemplatePath + " ..");
        FileOutputStream fos = new FileOutputStream(outputTemplatePath);
        fos.write(XmlUtil.nodeToFormattedString(outputdoc).getBytes());
        System.out.println(". Done");
        fos.close();
    }

    private static DBActions getDbActions(OSSpecificFunctions osFunctions) throws SQLException {
        if (dbActions == null) {
            try {
                dbActions = new DBActions(osFunctions);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.getMessage());
            }
        }
        return dbActions;
    }

    private static List<Element> getRoutingAssertionElementsFromPolicy(Document policyxml) {
        ArrayList<Element> output = new ArrayList<Element>();
        NodeList nl = policyxml.getElementsByTagNameNS("http://www.layer7tech.com/ws/policy", "HttpRoutingAssertion");
        for (int i = 0; i < nl.getLength(); i++) {
            output.add((Element)nl.item(i));
        }
        return output;
    }

    public static String extractIpAddressFromString(String input) {
        Matcher m = ipaddresspattern.matcher(input);
        if (m.find()) {
            return input.substring(m.start(), m.end());
        } else {
            return null;
        }
    }

    /*
    NOTES

    in a sql dump,

    an ip address inside a cluster property has the following pattern: ",'192.168.19.5')"
    two ip address inside a routing assertions has the following pattern: "<L7p:CustomIpAddresses stringArrayValue=\"included\">\n                <L7p:item stringValue=\"192.168.19.19\"/>\n                <L7p:item stringValue=\"192.168.19.20\"/>\n            </L7p:CustomIpAddresses>\n"


    in policy xml format, ip addresses added to a routing assertion look like:
           <L7p:HttpRoutingAssertion>
                <L7p:ProtectedServiceUrl stringValue="http://localhost:8080/parlayx/services/SendSms"/>
                <L7p:CustomIpAddresses stringArrayValue="included">
                    <L7p:item stringValue="192.168.19.1"/>
                    <L7p:item stringValue="192.168.19.2"/>
                </L7p:CustomIpAddresses>
            </L7p:HttpRoutingAssertion>

     otherwise, the policy xml is like
            <L7p:HttpRoutingAssertion>
                <L7p:ProtectedServiceUrl stringValue="http://localhost:8080/parlayx/services/SendSms"/>
            </L7p:HttpRoutingAssertion>

    */
}
