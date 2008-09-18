package com.l7tech.server.util;

import org.springframework.aop.Pointcut;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.core.Ordered;

import java.lang.annotation.Annotation;
import java.util.Collection;


/**
 * General purpose annotation based pointcut.
 */
public class AnnotationPointcut implements Ordered, Pointcut {

    //- PUBLIC

    /**
     * Create a new annotation pointcut that matches any of the given annotations.
     *
     * @param classAnnotations The class annotations to match
     * @param methodAnnotations The method annotations to match
     * @param checkInherited True to check inherited class annotations (not method)
     * @param order The order for this pointcut
     */
    public AnnotationPointcut( final Collection<Class<? extends Annotation>> classAnnotations,
                               final Collection<Class<? extends Annotation>> methodAnnotations,
                               final boolean checkInherited,
                               final int order ) {
        ComposablePointcut pc = new ComposablePointcut();

        // Add all class filters
        for ( Class<? extends Annotation> annotation : classAnnotations ) {
            ClassFilter filter = new AnnotationClassFilter( annotation, checkInherited );
            pc.intersection( filter );
        }

        // Add all method matchers     
        for ( Class<? extends Annotation> annotation : methodAnnotations ) {
            MethodMatcher matcher = new AnnotationMethodMatcher( annotation );
            pc.intersection( matcher );
        }

        this.composed = pc;
        this.order = order;
    }

    public ClassFilter getClassFilter() {
        return composed.getClassFilter();
    }

    public MethodMatcher getMethodMatcher() {
        return composed.getMethodMatcher();
    }

    public int getOrder() {
        return order;
    }

    //- PRIVATE

    private final ComposablePointcut composed;
    private final int order;

}
