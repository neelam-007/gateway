package com.l7tech.external.assertions.bulkjdbcinsert.server.transformers;

import com.l7tech.external.assertions.bulkjdbcinsert.server.Transformer;

/**
 * Created by moiyu01 on 15-10-19.
 */
abstract class AbstractTransformer implements Transformer {
    public boolean isParameterValid(String param){
        return true;
    }
}
