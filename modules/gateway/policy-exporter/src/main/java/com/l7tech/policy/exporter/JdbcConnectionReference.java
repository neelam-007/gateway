package com.l7tech.policy.exporter;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
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

    public JdbcConnectionReference( final ExternalReferenceFinder finder ) {
        super( finder );
    }

    public JdbcConnectionReference( final ExternalReferenceFinder context, final JdbcConnectionable connable) {
        this( context );
        if (connable == null) throw new IllegalStateException("JdbcConnectionable must not be null.");

        connectionName = connable.getConnectionName();
        try {
            JdbcConnection connection = getFinder().getJdbcConnection(connectionName);
            driverClass = connection.getDriverClass();
            jdbcUrl = connection.getJdbcUrl();
            userName = connection.getUserName();
        } catch (FindException e) {
            logger.warning("Cannot find the JDBC connection entity (ConnectionName = " + connable.getConnectionName() + ").");
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

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    public void setLocalizeReplaceByName(String connectionName) {
        localizeType = LocalizeAction.REPLACE;
        localConnectionName = connectionName;
    }

    public static JdbcConnectionReference parseFromElement( final ExternalReferenceFinder context, final Element elmt) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!elmt.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + ELMT_NAME_REF);
        }

        JdbcConnectionReference output = new JdbcConnectionReference( context );
        output.connectionName = getParamFromEl(elmt, ELMT_NAME_CONN_NAME);
        output.driverClass = getParamFromEl(elmt, ELMT_NAME_DRV_CLASS);
        output.jdbcUrl = getParamFromEl(elmt, ELMT_NAME_JDBC_URL);
        output.userName = getParamFromEl(elmt, ELMT_NAME_USR_NAME);

        return output;
    }

    @Override
    void serializeToRefElement(Element referencesParentElement) {
        Element refElmt = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_REF);
        setTypeAttribute( refElmt );
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
        try {
            JdbcConnection connection = getFinder().getJdbcConnection(connectionName);
            return
                connection != null && 
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
        if ( localizeType == LocalizeAction.REPLACE) {
            if (assertionToLocalize instanceof JdbcConnectionable) {
                JdbcConnectionable connable = (JdbcConnectionable)assertionToLocalize;
                if (connable.getConnectionName().equals(connectionName)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    connable.setConnectionName(localConnectionName);
                }
            }
        }  else if ( localizeType == LocalizeAction.DELETE) {
            logger.info("Deleted this assertion from the tree.");
            return false;
        }

        return true;
    }

}
