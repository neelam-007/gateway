package com.l7tech.server.message;

import java.lang.annotation.*;

/**
 * This annotation documents which {@link com.l7tech.server.message.PolicyEnforcementContext PolicyEnforcementContext}
 * methods are related to routing metrics.
 * Methods marked with this annotation should know to forward calls to the parent PEC
 * when configured to behave like routing assertions.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RoutingMetricsRelated {
}
