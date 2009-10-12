package com.l7tech.console.policy.exporter;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnectionAdmin;
import com.l7tech.gateway.common.jdbcconnection.JdbcConnection;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.DomUtils;

import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * @author ghuang
 */
public class JdbcConnectionReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(TrustedCertReference.class.getName());

    public static final String ELMT_NAME_REF = "JdbcConnectionReference";
    public static final String ELMT_NAME_CONN_NAME = "ConnectionName";
    public static final String ELMT_NAME_DRV_CLASS = "DriverClass";
    public static final String ELMT_NAME_JDBC_URL = "JdbcUrl";
    public static final String ELMT_NAME_USR_NAME = "UserName";

    private String connectionName;
    private String driverClass;
    private String jdbcUrl;
    private String userName;

    private String localConnectionName;
    private LocalizeAction localizeType;

    public JdbcConnectionReference() {
    }

    public JdbcConnectionReference(JdbcConnectionable connable) {
        if (connable == null) throw new IllegalStateException("JdbcConnectionable must not be null.");

        connectionName = connable.getConnectionName();
        JdbcConnectionAdmin admin = getJdbcConnectionAdmin();
        if (admin != null) {
            try {
                JdbcConnection connection = admin.getJdbcConnection(connectionName);
                driverClass = connection.getDriverClass();
                jdbcUrl = connection.getJdbcUrl();
                userName = connection.getUserName();
            } catch (FindException e) {
                logger.warning("Cannot find the JDBC connection entity (ConnectionName = " + connable.getConnectionName() + ").");
            }
        }
        localizeType = LocalizeAction.IGNORE;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setLocalizeReplace(String connectionName) {
        localizeType = LocalizeAction.REPLACE;
        localConnectionName = connectionName;
    }

    public static JdbcConnectionReference parseFromElement(Element elmt) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!elmt.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + ELMT_NAME_REF);
        }

        JdbcConnectionReference output = new JdbcConnectionReference();
        output.connectionName = getParamFromEl(elmt, ELMT_NAME_CONN_NAME);
        output.driverClass = getParamFromEl(elmt, ELMT_NAME_DRV_CLASS);
        output.jdbcUrl = getParamFromEl(elmt, ELMT_NAME_JDBC_URL);
        output.userName = getParamFromEl(elmt, ELMT_NAME_USR_NAME);

        return output;
    }

    @Override
    void serializeToRefElement(Element referencesParentElement) {
        Element refElmt = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_REF);
        refElmt.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, JdbcConnectionReference.class.getName());
        referencesParentElement.appendChild(refElmt);

        Element connNameElmt = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_CONN_NAME);
        Text txt = DomUtils.createTextNode(referencesParentElement, connectionName);
        connNameElmt.appendChild(txt);
        refElmt.appendChild(connNameElmt);

        Element driverClassElmt = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_DRV_CLASS);
        txt = DomUtils.createTextNode(referencesParentElement, driverClass);
        driverClassElmt.appendChild(txt);
        refElmt.appendChild(driverClassElmt);

        Element jdbcUrlElmt = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_JDBC_URL);
        txt = DomUtils.createTextNode(referencesParentElement, jdbcUrl);
        jdbcUrlElmt.appendChild(txt);
        refElmt.appendChild(jdbcUrlElmt);

        Element userNameElmt = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_USR_NAME);
        txt = DomUtils.createTextNode(referencesParentElement, userName);
        userNameElmt.appendChild(txt);
        refElmt.appendChild(userNameElmt);
    }

    @Override
    boolean verifyReference() throws InvalidPolicyStreamException {
        JdbcConnectionAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) return false;

        try {
            JdbcConnection connection = admin.getJdbcConnection(connectionName);
            return
                connection.getName().equals(connectionName) &&
                connection.getDriverClass().equals(driverClass) &&
                connection.getJdbcUrl().equals(jdbcUrl) &&
                connection.getUserName().equals(userName);
        } catch (FindException e) {
            logger.warning("Cannot find a JDBC connection, " + connectionName);
            return false;
        }
    }

    @Override
    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType == LocalizeAction.REPLACE) {
            if (assertionToLocalize instanceof JdbcConnectionable) {
                JdbcConnectionable connable = (JdbcConnectionable)assertionToLocalize;
                if (connable.getConnectionName().equals(connectionName)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    connable.setConnectionName(localConnectionName);
                }
            }
        }  else if (localizeType == LocalizeAction.DELETE) {
            logger.info("Deleted this assertion from the tree.");
            return false;
        }

        return true;
    }

    /**
     * Enum-type class for the type of localization to use.
     */
    private static class LocalizeAction {
        private static final LocalizeAction IGNORE = new LocalizeAction(1);
        private static final LocalizeAction DELETE = new LocalizeAction(2);
        private static final LocalizeAction REPLACE = new LocalizeAction(3);

        private int val = 0;

        public LocalizeAction(int val) {
            this.val = val;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocalizeAction)) return false;

            final LocalizeAction localizeAction = (LocalizeAction) o;

            return val == localizeAction.val;
        }

        public int hashCode() {
            return val;
        }
    }

    private JdbcConnectionAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }
}
