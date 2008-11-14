package com.l7tech.server.ems;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author steve
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NavigationPage {
    String section();
    int sectionIndex() default 0;
    String sectionPage() default "";
    String page();
    int pageIndex() default 0;
    String pageUrl() default "";         
}
