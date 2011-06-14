package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Variable selector that supports audit details of the current policy.
 */
public class AuditSelector implements ExpandVariables.Selector<AuditSelector.AuditHolder> {

    //- PUBLIC

    @Override
    public Class<AuditHolder> getContextObjectClass() {
        return AuditHolder.class;
    }

    @Override
    public Selection select( final String contextName,
                             final AuditHolder context,
                             String name,
                             final Syntax.SyntaxErrorHandler handler,
                             final boolean strict ) {

        if (name.toLowerCase().startsWith("details.last")) {
            final AuditDetail[] details = context.getDetailsInOrder();
            if (details == null || details.length == 0)
                return new Selection(null);
            String remainingName = name.substring( 12 );
            if ( remainingName.startsWith( "." ) ) remainingName = remainingName.substring( 1 );
            return new Selection(details[details.length-1], remainingName);
        } else if (name.toLowerCase().startsWith("details.")) {
            if (name.length() < "details.".length() + 1)
                return null;
            final AuditDetail[] details = context.getDetailsInOrder();
            if (details == null || details.length == 0)
                return new Selection(null);
            return AuditRecordSelector.selectDetails( name, details, logger );
        }

        return null;
    }

    public static final class AuditHolder {
        private final AuditContext context;

        public AuditHolder( final AuditContext context ) {
            this.context = context;
        }

        private AuditDetail[] getDetailsInOrder() {
            final Collection<AuditDetail> details = CollectionUtils.join( context.getDetails().values() );
            final AuditDetail[] sortedDetails = details.toArray( new AuditDetail[details.size()] );
            Arrays.sort( sortedDetails );
            return sortedDetails;
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( AuditSelector.class.getName() );
}
