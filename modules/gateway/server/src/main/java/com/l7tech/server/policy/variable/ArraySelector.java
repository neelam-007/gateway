package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;

/**
 * Selector for arrays.
 */
class ArraySelector implements ExpandVariables.Selector<Object[]> {

    //- PUBLIC

    @Override
    public Selection select( final String contextName,
                             final Object[] context,
                             final String name,
                             final Syntax.SyntaxErrorHandler handler,
                             final boolean strict ) {
        int indexOffset = name.indexOf( ']' );

        if ( indexOffset < 2 || !name.startsWith( "[" )) {
            String msg = handler.handleBadVariable( "Unable to process array subscript." );
            if ( strict ) throw new IllegalArgumentException(msg);
        } else {
            String indexText = name.substring( 1, indexOffset );
            String remainingName = name.substring( indexOffset+1 );

            try {
                int index = Integer.parseInt( indexText );
                if ( index < 0 || index >= context.length ) {
                    String msg = handler.handleSubscriptOutOfRange( index, contextName, context.length );
                    if ( strict ) throw new IllegalArgumentException(msg);
                } else {
                    return new Selection(context[index], process(remainingName));
                }
            } catch ( NumberFormatException nfe ) {
                String msg = handler.handleBadVariable( "Unable to process array subscript '"+indexText+"'." );
                if ( strict ) throw new IllegalArgumentException(msg);
            }
        }

        return null;
    }

    @Override
    public Class<Object[]> getContextObjectClass() {
        return Object[].class;
    }

    //- PRIVATE

    private String process( final String name ) {
        String processedName = null;

        if ( name != null && !name.isEmpty() ) {
            if ( name.startsWith( "." )) {
                processedName = name.substring( 1 );
            }
        }

        return processedName;
    }
}
