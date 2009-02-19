package com.l7tech.server.ems.util;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

/**
 * Typed extension of property column.
 */
public class TypedPropertyColumn extends PropertyColumn {

    //- PUBLIC

    public TypedPropertyColumn( final IModel displayModel,
                                final String propertyExpression,
                                final Class columnClass,
                                final boolean escapePropertyValue ) {
        this( displayModel, propertyExpression, columnClass, escapePropertyValue, false );
    }

    public TypedPropertyColumn( final IModel displayModel,
                                final String propertyExpression,
                                final Class columnClass,
                                final boolean escapePropertyValue,
                                final boolean multiline ) {
        super( displayModel, propertyExpression );
        this.columnClass = columnClass;
        this.escapePropertyValue = escapePropertyValue;
        this.multiline = multiline;
    }

    public TypedPropertyColumn( final IModel displayModel,
                                final String sortProperty,
                                final String propertyExpression,
                                final Class columnClass,
                                final boolean escapePropertyValue ) {
        this( displayModel, sortProperty, propertyExpression, columnClass, escapePropertyValue, false );
    }

    public TypedPropertyColumn( final IModel displayModel,
                                final String sortProperty,
                                final String propertyExpression,
                                final Class columnClass,
                                final boolean escapePropertyValue,
                                final boolean multiline ) {
        super( displayModel, sortProperty, propertyExpression );
        this.columnClass = columnClass;
        this.escapePropertyValue = escapePropertyValue;
        this.multiline = multiline;
    }

    public Class getColumnClass() {
        return this.columnClass;
    }

    public boolean isEscapePropertyValue() {
        return this.escapePropertyValue;
    }

    public void setEscapePropertyValue( final boolean escapePropertyValue ) {
        this.escapePropertyValue = escapePropertyValue;          
    }

    //- PRIVATE

    private final Class columnClass;
    private final boolean multiline;
    private boolean escapePropertyValue;

    public boolean isMultiline() {
        return multiline;
    }
}
