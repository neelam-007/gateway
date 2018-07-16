package com.l7tech.external.assertions.swagger;

import com.l7tech.util.ExceptionUtils;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Placeholder for swagger utility methods
 */
public final class SwaggerUtil {

    private static final Logger logger = Logger.getLogger(SwaggerUtil.class.getName());

    /**
     *  Default constructor at private access level
     */
    private SwaggerUtil() { }

    /**
     * Parses Swagger JSON string
     * @param doc Swagger JSON string
     * @return Swagger object for the specified Swagger JSON string
     */
    public static final Swagger parseSwaggerJson(String doc) {
        SwaggerParser parser = new SwaggerParser();
        List<AuthorizationValue> authorizationValues = new ArrayList<>();

        authorizationValues.add(new AuthorizationValue());
        try {
            return parser.parse(doc, authorizationValues);
        } catch (Throwable ex) {
            // Note: With swagger-parser 1.0.26, NoClassDefFoundError was found with non-json documents.
            // This breaking change is because of not including the [org/yaml/snakeyaml] dependency.
            // For backward compatibility, mask the exception and return the null swagger object.
            logger.log(Level.WARNING, String.format("Invalid or Unsupported Swagger Document [%s]", ex.getMessage()),
                    ExceptionUtils.getMessageWithCause(ex));
        }

        return null;
    }

}
