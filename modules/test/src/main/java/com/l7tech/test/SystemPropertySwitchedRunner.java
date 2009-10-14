package com.l7tech.test;

import org.junit.runner.manipulation.Filter;
import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class SystemPropertySwitchedRunner extends BlockJUnit4ClassRunner {
    
    public SystemPropertySwitchedRunner( final Class<?> aClass ) throws InitializationError {
        super(aClass);
        try {
            this.filter(new Filter() {
                
                @Override
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

                @Override
                public String describe() {
                    return "System Property Filter";
                }
            });
        } catch(Exception e) {
            throw new InitializationError(Collections.<Throwable>singletonList(e));
        }
    }
}
