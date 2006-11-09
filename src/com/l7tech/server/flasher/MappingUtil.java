package com.l7tech.server.flasher;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Comment;
import org.xml.sax.SAXException;

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

    private static final Pattern ipaddresspattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final String STAGINGMAPPINGNS = "http://www.layer7tech.com/flashing/stagingmapping";
    private static final String NS_PREFIX = "L7flash";
    private static final String IMPORTMAPPINGELNAME = "ssgimportmapping";
    private static final String BACKENDIPMAPPINGELNAME = "backendipmapping";
    private static final String IPMAPELNAME = "ipmap";
    private static final String SOURCEVALUEATTRNAME = "sourcevalue";
    private static final String TARGETVALUEATTRNAME = "targetvalue";
    private static final String VARMAPELNAME = "varmap";
    private static final String GLOBALVARMAPPINGELNAME = "globalvarmapping";

    public static void applyMappingChangesToDBDump(String dbDumpFilePath, String mappingFilePath) throws FlashUtilityLauncher.InvalidArgumentException, IOException, SAXException {
        // load mapping file, validate it and build two maps (one for backends, and one for global variables)
        FileInputStream fis = new FileInputStream(mappingFilePath);
        Document mappingDoc = XmlUtil.parse(fis);
        Element simEl = mappingDoc.getDocumentElement();
        if (!simEl.getLocalName().equals(IMPORTMAPPINGELNAME)) {
            throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " is not a valid mapping file");
        }
        if (!simEl.getNamespaceURI().equals(STAGINGMAPPINGNS)) {
            throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " is not a valid mapping file");
        }
        HashMap<String, String> backendIPMapping = new HashMap<String, String>();
        HashMap<String, String> varMapping = new HashMap<String, String>();
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
                    throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " has invalid values for ipmap element");
                }
                backendIPMapping.put(source, target);
            }
            Element gvmEl = XmlUtil.findOnlyOneChildElementByName(simEl, STAGINGMAPPINGNS, GLOBALVARMAPPINGELNAME);
            List varmaplist = XmlUtil.findChildElementsByName(gvmEl, STAGINGMAPPINGNS, VARMAPELNAME);
            for (Object aVarmaplist : varmaplist) {
                Element varmapel = (Element) aVarmaplist;
                String name = varmapel.getAttribute("name");
                String target = varmapel.getAttribute(TARGETVALUEATTRNAME);
                if (name == null || target == null) {
                    throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " has invalid values for varmap element");
                }
                varMapping.put(name, target);
            }
        } catch (TooManyChildElementsException e) {
            throw new FlashUtilityLauncher.InvalidArgumentException(mappingFilePath + " is not a valid mapping file. " + e.getMessage());
        }

        // todo, build regexes, replace values, log whenever something is changed
    }

    public static void produceTemplateMappingFileFromDatabaseConnection(String dburl, String dbuser,
                                                                        String dbpasswd, String outputTemplatePath) throws SQLException, SAXException, IOException {
        // create connection to database
        com.mysql.jdbc.Driver driver = new com.mysql.jdbc.Driver();
        Properties props = new Properties();
        props.put("user", dbuser);
        props.put("password", dbpasswd);
        Connection c = driver.connect(dburl, props);
        if (c == null) {
            throw new SQLException("could not connect using url: " + dburl + ". with username " + dbuser + ", and password: " + dbpasswd);
        }
        // go through the cluster properties
        HashMap<String, String> mapOfClusterProperties = new HashMap<String, String>();
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("select propkey, propvalue from cluster_properties;");
        while (rs.next()) {
            String value = rs.getString(2);
            String key = rs.getString(1);
            if (!key.equals("license")) mapOfClusterProperties.put(key, value);
        }
        rs.close();

        // go through the policies
        ArrayList<String> ipaddressesInRoutingAssertions = new ArrayList<String>();
        rs = s.executeQuery("select policy_xml from published_service;");
        while (rs.next()) {
            String xml = rs.getString(1);
            Document doc = XmlUtil.stringToDocument(xml);
            NodeList nl = doc.getElementsByTagNameNS("http://www.layer7tech.com/ws/policy", "HttpRoutingAssertion");
            for (int i = 0; i < nl.getLength(); i++) {
                Element ra = (Element)nl.item(i);
                Element cia = XmlUtil.findFirstChildElementByName(ra, "http://www.layer7tech.com/ws/policy",
                                                                      "CustomIpAddresses");
                List listofitems = XmlUtil.findChildElementsByName(cia, "http://www.layer7tech.com/ws/policy", "item");
                for (Object listofitem : listofitems) {
                    Element el = (Element) listofitem;
                    ipaddressesInRoutingAssertions.add(el.getAttribute("stringValue"));
                }
            }
        }
        rs.close();

        s.close();
        c.close();
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

    // for testing purposes only
    public static void main(String[] args) throws Exception {
        applyMappingChangesToDBDump("blah", "/home/flascell/tmp/template.xml");
    }
}
