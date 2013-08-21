package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.DomUtils;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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
class MappingUtil {

    private static final Logger logger = Logger.getLogger(MappingUtil.class.getName());
    private static final Pattern ipaddresspattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final String STAGINGMAPPINGNS = "http://www.layer7tech.com/migration/stagingmapping";
    private static final String NS_PREFIX = "L7migrate";
    private static final String IMPORTMAPPINGELNAME = "ssgimportmapping";
    private static final String BACKENDIPMAPPINGELNAME = "backendipmapping";
    private static final String IPMAPELNAME = "ipmap";
    private static final String SOURCEVALUEATTRNAME = "sourcevalue";
    private static final String TARGETVALUEATTRNAME = "targetvalue";
    private static final String VARMAPELNAME = "varmap";
    private static final String GLOBALVARMAPPINGELNAME = "globalvarmapping";

    public static class CorrespondanceMap {
        public HashMap<String, String> backendIPMapping = new HashMap<String, String>();
        public HashMap<String, String> varMapping = new HashMap<String, String>();
    }

    public static void applyMappingChangesToDB(final DatabaseConfig config,
                                               final CorrespondanceMap mappingResults,
                                               final boolean verbose,
                                               final PrintStream printStream) throws SQLException {
        Connection c = new DBActions().getConnection(config, false);
        try {
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Applying mappings", verbose, printStream);

            // iterate through policies
            // column exists if upgraded from 4.2
            applyRoutingIpMapping(c, "published_service", "policy_xml", mappingResults, verbose, printStream);
            applyRoutingIpMapping(c, "policy", "xml", mappingResults, verbose, printStream);
            applyRoutingIpMapping(c, "policy_version", "xml", mappingResults, verbose, printStream);

            // iterate through global variables
            for (String varname : mappingResults.varMapping.keySet()) {
                String varval = mappingResults.varMapping.get(varname);
                PreparedStatement updateps = c.prepareStatement("update cluster_properties set propvalue=? where propkey=?");
                updateps.setString(1, varval);
                updateps.setString(2, varname);
                int res = updateps.executeUpdate();
                if (res > 0) {
                    final String msg = "\tSetting cluster property " + varname + " to value " + varval;
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, verbose, printStream);
                } else {
                    final String msg = "\tTarget system does not have cluster property " + varname
                            + ". Ignoring this mapping";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, verbose, printStream);
                }
                updateps.close();
            }
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Mapping Complete", verbose, printStream);
        } finally {
            c.close();
        }
    }

    public static CorrespondanceMap loadMapping(String mappingFilePath) throws IOException, SAXException {
        // load mapping file, validate it and build two maps (one for backends, and one for global variables)
        FileInputStream fis = new FileInputStream(mappingFilePath);
        Document mappingDoc = XmlUtil.parse(fis);
        Element simEl = mappingDoc.getDocumentElement();
        if (!simEl.getLocalName().equals(IMPORTMAPPINGELNAME)) {
            logger.info("Error, provided mapping file is not valid as the root element is not " + IMPORTMAPPINGELNAME);
            throw new IllegalArgumentException(mappingFilePath + " is not a valid mapping file");
        }
        if (!simEl.getNamespaceURI().equals(STAGINGMAPPINGNS)) {
            logger.info("Error, provided mapping file is not valid as the root element is not of namespace " + STAGINGMAPPINGNS);
            throw new IllegalArgumentException(mappingFilePath + " is not a valid mapping file");
        }
        CorrespondanceMap output = new CorrespondanceMap();
        try {
            Element bimEl = DomUtils.findOnlyOneChildElementByName(simEl, STAGINGMAPPINGNS, BACKENDIPMAPPINGELNAME);
            List ipmaplist = DomUtils.findChildElementsByName(bimEl, STAGINGMAPPINGNS, IPMAPELNAME);
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
                    throw new IllegalArgumentException(mappingFilePath + " has invalid values for ipmap element");
                }
                output.backendIPMapping.put(source, target);
            }
            Element gvmEl = DomUtils.findOnlyOneChildElementByName(simEl, STAGINGMAPPINGNS, GLOBALVARMAPPINGELNAME);
            List varmaplist = DomUtils.findChildElementsByName(gvmEl, STAGINGMAPPINGNS, VARMAPELNAME);
            for (Object aVarmaplist : varmaplist) {
                Element varmapel = (Element) aVarmaplist;
                String name = varmapel.getAttribute("name");
                String target = varmapel.getAttribute(TARGETVALUEATTRNAME);
                if (name == null || target == null) {
                    logger.info("Error, an element " + VARMAPELNAME + " does not contain expected attributes");
                    throw new IllegalArgumentException(mappingFilePath + " has invalid values for varmap element");
                }

                //only map non-hidden hidden cluster properties
                if (!isHiddenClusterProperty(name, target)) {
                    output.varMapping.put(name, target);
                }
            }
        } catch (TooManyChildElementsException e) {
            logger.log(Level.INFO, "error loading mapping file", e);
            throw new IllegalArgumentException(mappingFilePath + " is not a valid mapping file. " + e.getMessage());
        }
        return output;
    }

    public static void produceTemplateMappingFileFromDB(final DatabaseConfig config,
                                                        final String outputTemplatePath,
                                                        final boolean isVerbose,
                                                        final PrintStream printStream) throws SQLException, SAXException, IOException {
        Connection conn = null;
        Set<String> ipaddressesInRoutingAssertions = new HashSet<String>();
        HashMap<String, String> mapOfClusterProperties = new HashMap<String, String>();
        try {
            conn = new DBActions().getConnection(config, false);
            // go through the cluster properties
            Statement statement = null;
            ResultSet resultSet = null;
            try{
                statement = conn.createStatement();
                resultSet = statement.executeQuery("select propkey, propvalue from cluster_properties;");
                while (resultSet.next()) {
                    String value = resultSet.getString(2);
                    String key = resultSet.getString(1);

                    //only map non-hidden cluster property
                    if (!isHiddenClusterProperty(key, value)) {
                        mapOfClusterProperties.put(key, value);
                    }
                }
            }finally{
                ResourceUtils.closeQuietly(resultSet);
                ResourceUtils.closeQuietly(statement);
            }
            // go through the policies
            ipaddressesInRoutingAssertions.addAll(getRoutingIpAddressesFromPolicies(conn, "published_service", "policy_xml"));  // column exists if upgraded from 4.2
            ipaddressesInRoutingAssertions.addAll(getRoutingIpAddressesFromPolicies(conn, "policy", "xml"));
            ipaddressesInRoutingAssertions.addAll(getRoutingIpAddressesFromPolicies(conn, "policy_version", "xml"));
        } finally {
            ResourceUtils.closeQuietly(conn);
        }
        Document outputdoc = XmlUtil.createEmptyDocument(IMPORTMAPPINGELNAME, NS_PREFIX,
                                                         STAGINGMAPPINGNS);
        Comment comment = outputdoc.createComment("Please review backend ip addresses and global variables" +
                                                 "\n\tand provide corresponding values for the target system." +
                                                 "\n\tWhen in doubt, duplicate the sourcevalue in the targetvalue. " +
                                                 "\n\tDo not leave properties with targetvalue=\"__add_your_value__\"");
        outputdoc.getDocumentElement().appendChild(comment);
        Element backendipmappingEl = DomUtils.createAndAppendElementNS(outputdoc.getDocumentElement(),
                                                                      BACKENDIPMAPPINGELNAME,
                                                                      STAGINGMAPPINGNS,
                                                                      NS_PREFIX);
        for (String routingip : ipaddressesInRoutingAssertions) {
            Element ipmapel = DomUtils.createAndAppendElementNS(backendipmappingEl, IPMAPELNAME,
                    STAGINGMAPPINGNS,
                    NS_PREFIX);
            ipmapel.setAttribute(SOURCEVALUEATTRNAME, routingip);
            ipmapel.setAttribute(TARGETVALUEATTRNAME, "__add_your_value__");

        }

        Element globalvarmappingEl = DomUtils.createAndAppendElementNS(outputdoc.getDocumentElement(),
                                                                      GLOBALVARMAPPINGELNAME,
                                                                      STAGINGMAPPINGNS,
                                                                      NS_PREFIX);

        for (String propKey: mapOfClusterProperties.keySet()) {
            Element varmapEl = DomUtils.createAndAppendElementNS(globalvarmappingEl, VARMAPELNAME,
                    STAGINGMAPPINGNS,
                    NS_PREFIX);
            varmapEl.setAttribute("name", propKey);
            varmapEl.setAttribute(SOURCEVALUEATTRNAME, mapOfClusterProperties.get(propKey));
            varmapEl.setAttribute(TARGETVALUEATTRNAME, "__add_your_value__");
        }

        final String msg = "Outputting template mapping file at " + outputTemplatePath + " ..";
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream, false);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputTemplatePath);
            fos.write(XmlUtil.nodeToFormattedString(outputdoc).getBytes());
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, ". Done\n", isVerbose, printStream, false);
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    /**
     * Determines if the cluster property is considered to be hidden or not hidden.
     *
     * @param key   Key of the cluster property (ie name)
     * @param value Value of the cluster property
     * @return  TRUE if the key is a hidden cluster property, otherwise FALSE.
     */
    private static boolean isHiddenClusterProperty(String key, String value) {
        ClusterProperty cp = new ClusterProperty(key, value);
        return cp.isHiddenProperty();
    }

    /**
     * Searches for routing IP addresses in policies stored in a table column.
     *
     * <p>Example policy XML with custom routing IP addresses:
     * <blockquote><pre>
     * &lt;wsp:Policy xmlns:L7p="http://www.layer7tech.com/ws/policy" xmlns:wsp="http://schemas.xmlsoap.org/ws/2002/12/policy">
     *     ...
     *     &lt;L7p:HttpRoutingAssertion>
     *         ...
     *         &lt;L7p:CustomIpAddresses stringArrayValue="included">
     *             &lt;L7p:item stringValue="123.0.0.1"/>
     *             &lt;L7p:item stringValue="123.0.0.2"/>
     *             ...
     *         &lt;/L7p:CustomIpAddresses>
     *         ...
     *     &lt;/L7p:HttpRoutingAssertion>
     *     ...
     * &lt;/wsp:Policy>
     * </pre></blockquote>
     *
     * @param connection    opened connection to the database
     * @param table         name of database table to search in
     * @param column        name of table column with policy XMLs
     * @return Set of routing IP addresses found; empty Set if table or column does not exist; never null
     * @throws SQLException if database error occurs
     * @throws SAXException if XML parsing error occurs
     * @throws NullPointerException if any parameter is <code>null</code>
     */
    private static Set<String> getRoutingIpAddressesFromPolicies(final Connection connection,
                                                                 final String table,
                                                                 final String column)
            throws SQLException, SAXException {
        if (connection == null) throw new NullPointerException("connection must not be null");
        if (table == null) throw new NullPointerException("table must not be null");
        if (column == null) throw new NullPointerException("column must not be null");

        final Statement statement = connection.createStatement();
        try {
            // Tests if table exists.
            ResultSet resultSet = statement.executeQuery("SHOW TABLES LIKE '" + table + "';");
            final boolean tableExists = resultSet.next();
            resultSet.close();
            if (!tableExists) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Table \"" + table + "\" does not exists; skipped in search for routing IP addresses.");
                }
                return Collections.emptySet();
            }

            // Tests if column exists.
            resultSet = statement.executeQuery("SHOW COLUMNS FROM " + table + " LIKE '" + column + "';");
            final boolean columnExists = resultSet.next();
            resultSet.close();
            if (!columnExists) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Column \"" + table + "." + column + "\" does not exist; skipped in search for routing IP addresses.");
                }
                return Collections.emptySet();
            }

            final Set<String> ipAddresses = new HashSet<String>();
            resultSet = statement.executeQuery("select " + column + " from " + table + ";");
            while (resultSet.next()) {
                final String policyXml = resultSet.getString(1);
                if (policyXml != null) {
                    final Document doc = XmlUtil.stringToDocument(policyXml);
                    final List<Element> routingAssertions = getRoutingAssertionElementsFromPolicy(doc);
                    for (Element routingAssertion : routingAssertions) {
                        final Element cia = DomUtils.findFirstChildElementByName(routingAssertion,
                                                                                "http://www.layer7tech.com/ws/policy",
                                                                                "CustomIpAddresses");
                        if (cia != null) {
                            final List<Element> items = DomUtils.findChildElementsByName(cia, "http://www.layer7tech.com/ws/policy", "item");
                            for (Element item : items) {
                                String value = item.getAttribute("stringValue");
                                if (value != null) {
                                    ipAddresses.add(value);
                                }
                            }
                        }
                    }
                }
            }
            resultSet.close();

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Custom routing IP addresses found in \"" + table + "." + column + "\": " + ipAddresses);
            }
            return ipAddresses;
        } finally {
            statement.close();
        }
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

    /**
     * Applies mapping of routing IP addresses to a database table.
     *
     * @param connection    opened connection to the database
     * @param table         name of database table to apply to
     * @param policyColumn  name of column containing policy XML
     * @param mapping       mapping to apply
     * @throws SQLException if database error occurs
     * @throws NullPointerException if any parameter is <code>null</code>
     */
    private static void applyRoutingIpMapping(final Connection connection,
                                              final String table,
                                              final String policyColumn,
                                              final CorrespondanceMap mapping,
                                              final boolean verbose,
                                              final PrintStream printStream)
            throws SQLException {
        if (connection == null) throw new NullPointerException("connection must not be null");
        if (table == null) throw new NullPointerException("table must not be null");
        if (policyColumn == null) throw new NullPointerException("policyColumn must not be null");
        if (mapping == null) throw new NullPointerException("mapping must not be null");

        final Statement selectStatement = connection.createStatement();
        try {
            // Tests if table exists.
            ResultSet resultSet = selectStatement.executeQuery("SHOW TABLES LIKE '" + table + "';");
            final boolean tableExists = resultSet.next();
            resultSet.close();
            if (!tableExists) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Table \"" + table + "\" does not exists; skipped in mapping routing IP addresses.");
                }
                return;
            }

            // Tests if column exists.
            resultSet = selectStatement.executeQuery("SHOW COLUMNS FROM " + table + " LIKE '" + policyColumn + "';");
            final boolean columnExists = resultSet.next();
            resultSet.close();
            if (!columnExists) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Column \"" + table + "." + policyColumn + "\" does not exist; skipped in mapping routing IP addresses.");
                }
                return;
            }

            resultSet = selectStatement.executeQuery("select goid, " + policyColumn + " from " + table + ";");
            while (resultSet.next()) {
                final byte[] goid = resultSet.getBytes(1);
                String policyXml = resultSet.getString(2);
                boolean changed = false;
                if (policyXml != null) {
                    for (String fromIP : mapping.backendIPMapping.keySet()) {
                        if (policyXml.indexOf("stringValue=\"" + fromIP + "\"") >= 0) {
                            String toIP = mapping.backendIPMapping.get(fromIP);
                            final String msg = "\tMapping routing IP address for " +
                                    getDescription(connection, table, goid) + ": from " + fromIP + " to " + toIP;
                            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, verbose, printStream);
                            policyXml = policyXml.replace("stringValue=\"" + fromIP + "\"", "stringValue=\"" + toIP + "\"");
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    final PreparedStatement updateStatement = connection.prepareStatement("UPDATE " + table + " SET " + policyColumn + "=? WHERE goid=?");
                    updateStatement.setString(1, policyXml);
                    updateStatement.setBytes(2, goid);
                    updateStatement.executeUpdate();
                    updateStatement.close();
                }
            }
            resultSet.close();
        } finally {
            selectStatement.close();
        }
    }

    /**
     * Gets a printable description of a policy-related item.
     *
     * @param connection    opened connection to the database
     * @param table         name of table containing the item
     * @param goid          goid of the item
     * @return an appropriate description of the policy item; or "N/A" if database error occurs; never null
     * @throws NullPointerException if any parameter is <code>null</code>
     * @throws RuntimeException if <code>table</code> is not related to policies
     */
    private static String getDescription(final Connection connection,
                                         final String table,
                                         final byte[] goid) {
        if (connection == null) throw new NullPointerException("connection must not be null");
        if (table == null) throw new NullPointerException("table must not be null");

        String description;
        try {
            if (table.equalsIgnoreCase("published_service")) {
                final String serviceName = getNameField(connection, "published_service", "goid", goid);
                description = "\"" + serviceName + "\" service";
            } else if (table.equalsIgnoreCase("policy")) {
                description = getPolicyDescription(connection, goid);
            } else if (table.equalsIgnoreCase("policy_version")) {
                final PreparedStatement statement = connection.prepareStatement("SELECT ordinal, policy_goid FROM policy_version WHERE goid=?");
                statement.setBytes(1, goid);
                final ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                final int ordinal = resultSet.getInt(1);
                final byte[] policy_goid = resultSet.getBytes(2);
                resultSet.close();
                statement.close();
                description = getPolicyDescription(connection, policy_goid) + " version " + ordinal;
            } else {
                throw new RuntimeException("Table not related to policies: " + table);
            }
        } catch (SQLException e) {
            description = "N/A";
        }

        return description;
    }

    /**
     * Gets a printable description of a policy item.
     *
     * <p>If the item is a policy fragment, returns a string of the form
     * <blockquote><pre>policy fragment "foo"</pre></blockquote>
     * If the item is a published service policy, returns a string of the form
     * <blockquote><pre>"bar" service policy</pre></blockquote>
     *
     * @param connection    opened connection to the database
     * @param policyGoid     policy goid
     * @return an appropriate description of the policy item; never null
     * @throws SQLException if database error occurs
     */
    private static String getPolicyDescription(final Connection connection, final byte[] policyGoid) throws SQLException {
        final String policyName = getNameField(connection, "policy", "goid", policyGoid);
        if (policyName != null) {
            // This is a named policy fragment.
            return "policy fragment \"" + policyName + "\"";
        } else {
            // This is a published service policy.
            final String serviceName = getNameField(connection, "published_service", "policy_goid", policyGoid);
            return "\"" + serviceName + "\" service policy";
        }
    }

    /**
     * Gets the "name" column value for a row in a table with matching objectid.
     * If more than one row have matching objectid, only the first row is used.
     *
     * @param connection    opened connection to the database
     * @param table         name of database table
     * @param goidColumn     name of column containing goid
     * @param goid           value of goid
     * @return value of "name" column; null if no row has matching goid or the SQL value is NULL
     * @throws SQLException if database error occurs
     */
    private static String getNameField(final Connection connection, final String table, final String goidColumn, final byte[] goid) throws SQLException {
        String name = null;
        final PreparedStatement statement = connection.prepareStatement("SELECT name FROM " + table + " WHERE " + goidColumn + "=?;");
        statement.setBytes(1, goid);
        final ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            name = resultSet.getString(1);
        }
        resultSet.close();
        statement.close();
        return name;
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