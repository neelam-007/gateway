package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * This will strictly convert a boolean parameter. Jersey by default is lax in it's boolean conversion. Meaning setting
 * a parameter to anything other then 'true' will set it to false. This implementation requires the parameter value to
 * either be 'true' or 'false' anything else will thrown an exception
 */
@Provider
public class StrictBooleanParamProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type type, Annotation[] annotations) {
        //This is only applicable to Boolean parameters
        if (Boolean.class.equals(rawType)) {
            //noinspection unchecked
            return (ParamConverter<T>) new ParamConverter<Boolean>() {
                @Override
                public Boolean fromString(String s) {
                    switch (s) {
                        case "true":
                            return true;
                        case "false":
                            return false;
                        default:
                            throw new InvalidArgumentException("Invalid boolean value, must be either 'true' or 'false'");
                    }
                }

                @Override
                public String toString(Boolean t) {
                    return t.toString();
                }
            };
        }
        return null;
    }
}
