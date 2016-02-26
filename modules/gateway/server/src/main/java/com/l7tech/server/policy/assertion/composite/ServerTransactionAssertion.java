package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.TransactionAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class ServerTransactionAssertion extends ServerCompositeAssertion<TransactionAssertion> {

    @Inject
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    private final String[] variablesUsed;
    private final String derivedConnectionName;

    private final AssertionResultListener assertionResultListener = new AssertionResultListener() {
        @Override
        public boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result) {
            if (result != AssertionStatus.NONE) {
                seenAssertionStatus(context, result);
                rollbackDeferredAssertions(context);
                return false;
            }
            return true;
        }
    };

    public ServerTransactionAssertion(TransactionAssertion data, ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(data, applicationContext);

      variablesUsed = assertion.getVariablesUsed();
      if(assertion.getConnectionName()==null || assertion.getConnectionName().isEmpty()) {
          this.derivedConnectionName = getConnectionNames(data);
      }else{
          this.derivedConnectionName=null;
      }
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
      String connectionName = derivedConnectionName;

      // use explicit connection name if specified
      if(assertion.getConnectionName()!=null && !assertion.getConnectionName().isEmpty()) {
        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        connectionName = ExpandVariables.process(assertion.getConnectionName(), variableMap, getAudit());
      }

      if ( connectionName == null || connectionName.isEmpty()) {
            // No JDBC assertions under us, behave just like All assertion
            return iterateChildren( context, assertionResultListener );
        }

        final DataSource ds;
        try {
            ds = jdbcConnectionPoolManager.getDataSource( connectionName );
        } catch ( SQLException|NamingException e ) {
            logAndAudit( AssertionMessages.JDBC_CONNECTION_ERROR, "connection name \"" + connectionName + "\": " +
                    ExceptionUtils.getMessage( e ) );
            return AssertionStatus.SERVER_ERROR;
        }

        if ( ds == null ) {
            logAndAudit( AssertionMessages.JDBC_CONNECTION_ERROR, "connection name \"" + connectionName + "\": " +
                    "JDBC connection data source not found" );
            return AssertionStatus.SERVER_ERROR;
        }

        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate( new DataSourceTransactionManager( ds ) );
            return transactionTemplate.execute( new TransactionCallback<AssertionStatus>() {
                @Override
                public AssertionStatus doInTransaction( TransactionStatus transactionStatus ) {
                    try {
                        AssertionStatus status = iterateChildren( context, assertionResultListener );
                        if ( status != AssertionStatus.NONE )
                            transactionStatus.setRollbackOnly();
                        return status;
                    } catch (PolicyAssertionException | IOException e ) {
                        transactionStatus.setRollbackOnly();
                    }
                    return AssertionStatus.FAILED;
                }
            } );
        } catch ( TransactionException | DataAccessException e ) {
            String message = ExceptionUtils.getMessage( e );
            logAndAudit( AssertionMessages.JDBC_CONNECTION_ERROR, "connection name \"" + connectionName + "\": " +
                    "Transaction failed: " + message );
            return AssertionStatus.FAILED;
        }
    }

    // Hack -- get connection names by scanning children
    private String getConnectionNames(Assertion root){
        Iterator<Assertion> it = root.preorderIterator();
        while (it.hasNext()) {
            Assertion kid = it.next();
            if (kid == null || !kid.isEnabled() || root.equals(kid))
                continue;

            if(kid instanceof JdbcConnectionable ){
                return ((JdbcConnectionable) kid).getConnectionName();
            }

            if(kid instanceof CompositeAssertion ){
                String name = getConnectionNames(kid);
                if(name!=null)
                    return name;
            }
        }
        return null;
    }

}
