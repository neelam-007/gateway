package com.l7tech.server.identity.ldap;

/**
 * Utility class for construction of search filters.
 */
class LdapSearchFilter {

    //- PUBLIC

    /**
     * Begin an OR expression.
     *
     * <p>Subsequent expressions will become part of the OR.</p>
     */
    public void or() {
        beginExpression( "(|" );
    }

    /**
     * Begin an AND expression.
     *
     * <p>Subsequent expressions will become part of the AND.</p>
     */
    public void and() {
        beginExpression( "(&" );
    }

    /**
     * End the current expression.
     */
    public void end() {
        if ( expressionDepth > 0 ) {
            if ( expressionStart == filterBuilder.length()-2 ) {
                // empty expression, so remove it
                filterBuilder.delete( expressionStart, filterBuilder.length() );
            } else {
                filterBuilder.append( ")" );
            }

            expressionDepth--;
        }
    }

    /**
     * Add an expression to check for an objectclass
     *
     * @param objectClass The objectclass to check for
     */
    public void objectClass( final String objectClass ) {
        attrEquals( LdapIdentityProvider.OBJECTCLASS_ATTRIBUTE_NAME, objectClass );
    }

    /**
     * Add an expression to check for an attribute
     *
     * @param attrName The attribute to check for
     */
    public void attrPresent( final String attrName ) {
        attributeExpression(  attrName, "=", "*" );
    }

    /**
     * Add an expression to check an attribute value.
     *
     * @param attrName The attribute to test
     * @param attrValue The attribute value that is required
     */
    public void attrEquals( final String attrName, final String attrValue ) {
        attributeExpression(  attrName, "=", LdapUtils.filterEscape( attrValue ) );
    }

    /**
     * Add an expression to check an attribute value.
     *
     * <p>This will not escape the attribute value, so it can contain values that
     * are meaningful to LDAP.</p>
     *
     * @param attrName The attribute to test
     * @param attrValue The attribute value that is required
     */
    public void attrEqualsUnsafe( final String attrName, final String attrValue ) {
        attributeExpression(  attrName, "=", attrValue );
    }

    /**
     * Add an expression to the filter.
     *
     * <p>WARNING! This is not escaped, the caller must ensure the given expression is valid/safe.</p>
     *
     * @param expression The expression to add.
     */
    public void expression( final String expression ) {
        filterBuilder.append( "(" );
        filterBuilder.append( expression );
        filterBuilder.append( ")" );
    }

    /**
     * Build the search filter.
     *
     * @return The search filter.
     */
    public String buildFilter() {
        for (; expressionDepth > 0; expressionDepth-- ) {
            filterBuilder.append( ")" );
        }

        return filterBuilder.toString();
    }

    /**
     * A string representation of the current search filter text.
     *
     * @return The search filter.
     */
    @Override
    public String toString() {
        return filterBuilder.toString();
    }

    /**
     * Check if the filter is empty.
     *
     * <p>This class supports limited rollback of empty expressions, so if an "or" is added
     * but no other expression, isEmpty will be true (once "end" has been called).</p>
     *
     * @return true if empty.
     */
    public boolean isEmpty() {
        return filterBuilder.length() == 0;
    }

    //- PRIVATE

    private final StringBuilder filterBuilder = new StringBuilder( 200 );
    private int expressionDepth = 0;
    private int expressionStart = -1;

    private void beginExpression( final String text ) {
        expressionStart = filterBuilder.length();
        filterBuilder.append( text );
        expressionDepth++;
    }

    private void attributeExpression( final String attrName, final String op, final String attrValue ) {
        filterBuilder.append( "(" );
        filterBuilder.append( LdapUtils.filterEscape( attrName ) );
        filterBuilder.append( op );
        filterBuilder.append( attrValue );
        filterBuilder.append( ")" );
    }
}
