package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.*;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * This will validate {@link com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam}
 */
@Provider
public class ChoiceParamProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type type, Annotation[] annotations) {
        //get the @ChoiceParam annotation
        final ChoiceParam choiceParam = (ChoiceParam) Functions.grepFirst(Arrays.asList(annotations), new Functions.Unary<Boolean, Annotation>() {
            @Override
            public Boolean call(Annotation annotation) {
                return annotation instanceof ChoiceParam;
            }
        });
        //return null if there is no @ChoiceParam annotation
        if (choiceParam == null) {
            return null;
        } else {
            //Check to see if this is a string parameter. Throw an error if it is not
            if (!String.class.equals(rawType)) {
                throw new IllegalStateException("Arguments with the @ChoiceParam annotation must be of type String.");
            } else {
                //noinspection unchecked
                return (ParamConverter<T>) new ParamConverter<String>() {

                    @Override
                    public String fromString(String value) {
                        //check if the given value is in the choices list.
                        for (String choice : choiceParam.value()) {
                            if (choiceParam.caseSensitive() ? StringUtils.equals(choice, value) : StringUtils.equalsIgnoreCase(choice, value)) {
                                //valid choice so return it;
                                return value;
                            }
                        }
                        //The value is not in the choices list so throw an error.
                        throw new InvalidArgumentException("Invalid parameter value '" + value + "'. Expected one of: " + Arrays.asList(choiceParam.value()).toString());
                    }

                    @Override
                    public String toString(String value) throws IllegalArgumentException {
                        return value;
                    }
                };
            }
        }
    }
}
