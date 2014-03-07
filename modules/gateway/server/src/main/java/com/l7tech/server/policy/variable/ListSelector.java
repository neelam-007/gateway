package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;

import java.util.List;

public class ListSelector implements ExpandVariables.Selector<List> {


    @Override
    public Selection select(String contextName, List context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        int indexOffset = name.indexOf( '.' );

        String indexText;
        String remainingName;
        if ( indexOffset < 1 ) {
            indexText = name;
            remainingName = null;
        } else {
            indexText = name.substring( 0, indexOffset );
            remainingName = name.substring( indexOffset+1 );
        }

        try {
            int index = Integer.parseInt( indexText ) - 1; // selector is one based
            if ( index < 0 || index >= context.size() ) {
                String msg = handler.handleSubscriptOutOfRange( index, contextName, context.size() );
                if ( strict ) throw new IllegalArgumentException(msg);
            } else {
                return new Selection(context.get(index), process(remainingName));
            }
        } catch ( NumberFormatException nfe ) {
            // If the list selector cannot be parsed as a integer it will be parsed as another selector somewhere else,
            // thus we swallow the exception.
        }

        return null;
    }

    @Override
    public Class<List> getContextObjectClass() {
        return List.class;
    }

    private String process( final String name ) {
        String processedName = null;

        if ( name != null && !name.isEmpty() ) {
            if ( name.startsWith( "." )) {
                processedName = name.substring( 1 );
            } else {
                processedName = name;
            }
        }

        return processedName;
    }
}
