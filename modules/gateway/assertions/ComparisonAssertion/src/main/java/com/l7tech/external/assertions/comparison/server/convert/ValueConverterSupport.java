package com.l7tech.external.assertions.comparison.server.convert;

import com.l7tech.policy.variable.DataType;

/**
 * Support class for ValueConverter implementation.
 */
abstract class ValueConverterSupport<RT> implements ValueConverter<RT> {

    //- PUBLIC

    @Override
    public DataType getDataType() {
        return dataType;
    }

    //- PACKAGE

    ValueConverterSupport( final DataType dataType ) {
        this.dataType = dataType;    
    }

    //- PRIVATE

    private final DataType dataType;
}
