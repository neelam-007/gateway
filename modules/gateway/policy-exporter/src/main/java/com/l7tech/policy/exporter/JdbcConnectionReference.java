package com.l7tech.policy.exporter;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.variable.Syntax;
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
            if ( connection != null ) {
                driverClass = connection.getDriverClass();
                jdbcUrl = connection.getJdbcUrl();
                userName = connection.getUserName();
            }
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
    void serializeToRefElement(final Element referencesParentElement) {
        Element referenceElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_REF);
        setTypeAttribute( referenceElement );
        referencesParentElement.appendChild(referenceElement);

        addParameterElement( ELMT_NAME_CONN_NAME, connectionName, referenceElement );
        addParameterElement( ELMT_NAME_DRV_CLASS, driverClass, referenceElement );
        addParameterElement( ELMT_NAME_JDBC_URL, jdbcUrl, referenceElement );
        addParameterElement( ELMT_NAME_USR_NAME, userName, referenceElement );
    }

    private void addParameterElement( final String name, final String value, final Element parent ) {
        if ( value != null ) {
            final Element parameterElement = parent.getOwnerDocument().createElement(name);
            final Text txt = DomUtils.createTextNode(parent, value);
            parameterElement.appendChild(txt);
            parent.appendChild(parameterElement);
        }
    }

    @Override
    boolean verifyReference() throws InvalidPolicyStreamException {
        if (Syntax.getReferencedNames(connectionName).length > 0) {
            return true;
        }
        try {
            JdbcConnection connection = getFinder().getJdbcConnection(connectionName);
            return
                connection != null && 
                connection.getName().equalsIgnoreCase(connectionName) &&
                (driverClass==null || connection.getDriverClass().equals(driverClass)) &&
                (jdbcUrl==null || connection.getJdbcUrl().equals(jdbcUrl)) &&
                (userName==null || connection.getUserName().equals(userName));
        } catch (FindException e) {
            logger.warning("Cannot find a JDBC connection, " + connectionName);
            return false;
        }
    }

    @Override
    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE){
            if (assertionToLocalize instanceof JdbcConnectionable) {
                final JdbcConnectionable connable = (JdbcConnectionable)assertionToLocalize;
                if (connable.getConnectionName().equalsIgnoreCase(connectionName)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if ( localizeType == LocalizeAction.REPLACE) {
                        connable.setConnectionName(localConnectionName);
                    }  else if ( localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final JdbcConnectionReference that = (JdbcConnectionReference) o;

        if ( connectionName != null ? !connectionName.equals( that.connectionName ) : that.connectionName != null )
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return connectionName != null ? connectionName.hashCode() : 0;
    }
}
