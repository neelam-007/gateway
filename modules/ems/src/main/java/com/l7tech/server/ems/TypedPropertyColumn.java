package com.l7tech.server.ems;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

/**
 * Typed extension of property column.
 */
public class TypedPropertyColumn extends PropertyColumn {

    //- PUBLIC

    public TypedPropertyColumn( final IModel displayModel,
                                final String propertyExpression,
                                final Class columnClass ) {
        super( displayModel, propertyExpression );
        this.columnClass = columnClass;
    }

    public TypedPropertyColumn( final IModel displayModel,
                                final String sortProperty,
                                final String propertyExpression,
                                final Class columnClass ) {
        super( displayModel, sortProperty, propertyExpression );
        this.columnClass = columnClass;
    }

    public Class getColumnClass() {
        return columnClass;
    }

    //- PRIVATE

    private final Class columnClass;
}
