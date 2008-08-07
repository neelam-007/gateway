package com.l7tech.test;

import org.junit.internal.runners.TestClassRunner;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.Description;

/**
 *
 */
public class SystemPropertySwitchedRunner extends TestClassRunner {
    
    public SystemPropertySwitchedRunner( final Class<?> aClass ) throws InitializationError {
        super(aClass);
        try {
            this.filter(new Filter() {
                public boolean shouldRun(Description description) {
                    boolean run = true;

                    SystemPropertyPrerequisite prereq = description.getAnnotation(SystemPropertyPrerequisite.class);
                    if ( prereq != null ) {
                        if ( prereq.unless().length() > 0 && System.getProperty(prereq.unless())!=null ) {
                            run = false;
                        }

                        if ( prereq.require().length() > 0 && System.getProperty(prereq.require())==null ) {
                            run = false;
                        }
                    }

                    return run;
                }

                public String describe() {
                    return "System Property Filter";
                }
            });
        } catch(Exception e) {
            throw new InitializationError();
        }
    }
}
