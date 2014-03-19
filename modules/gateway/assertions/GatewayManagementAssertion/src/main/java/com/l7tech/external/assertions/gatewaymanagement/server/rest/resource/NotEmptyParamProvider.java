package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.util.Functions;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.internal.inject.ExtractorException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.Arrays;

/**
 * This implements the {@link com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.NotEmpty} parameter
 * annotation provider
 */
@Provider
public class NotEmptyParamProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type type, Annotation[] annotations) {
        //find the @NotEmpty annotation
        final NotEmpty notEmptyParam = (NotEmpty) Functions.grepFirst(Arrays.asList(annotations), new Functions.Unary<Boolean, Annotation>() {
            @Override
            public Boolean call(Annotation annotation) {
                return annotation instanceof NotEmpty;
            }
        });
        //return null if the @NotEmpty annotation cannot be found
        if (notEmptyParam == null) {
            return null;
        } else {
            //find the value of method on the parameter object
            final Method valueOf = AccessController.doPrivileged(ReflectionHelper.getValueOfStringMethodPA(rawType));
            //Parameters using the @NotEmpty annotation must have a static valueOf(String) method. If one cannot be found throw an error
            if (valueOf == null) {
                throw new IllegalStateException("Parameters annotated with @NotEmpty must have a valueOf(String) method");
            } else {
                return new ParamConverter<T>() {

                    @Override
                    public T fromString(String value) {
                        // if the value is an empty string throw an exception
                        if (value.isEmpty()) {
                            throw new InvalidArgumentException("Value cannot be empty");
                        }
                        try {
                            return rawType.cast(valueOf.invoke(null, value));
                        } catch (InvocationTargetException ex) {
                            //This is thrown if there was a problem calling the parameters valueOf method.
                            Throwable cause = ex.getCause();
                            if (cause instanceof WebApplicationException) {
                                throw (WebApplicationException) cause;
                            } else {
                                throw new ExtractorException(cause);
                            }
                        } catch (Exception ex) {
                            throw new ProcessingException(ex);
                        }
                    }

                    @Override
                    public String toString(T value) throws IllegalArgumentException {
                        return value.toString();
                    }
                };
            }
        }
    }
}

