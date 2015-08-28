package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.TransactionAssertion;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
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

    private final String connectionName;

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

        this.connectionName = getConnectionNames( data );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        //final String connName = getVariableExpander( context ).expandVariables( assertion.getConnectionName() );

        if ( connectionName == null ) {
            // No JDBC assertions under us, behave just like All assertion
            return iterateChildren( context, assertionResultListener );
        }

        final DataSource ds;
        try {
            ds = jdbcConnectionPoolManager.getDataSource( connectionName );
        } catch ( SQLException e ) {
            logAndAudit( AssertionMessages.JDBC_CONNECTION_ERROR, connectionName );
            return AssertionStatus.SERVER_ERROR;
        } catch ( NamingException e ) {
            logAndAudit( AssertionMessages.JDBC_CONNECTION_ERROR, connectionName );
            return AssertionStatus.SERVER_ERROR;
        }

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
