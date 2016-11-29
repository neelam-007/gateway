package com.l7tech.console.policy;

import com.l7tech.policy.assertion.Assertion;
import static com.l7tech.util.ClassUtils.getArrayElementClassName;
import static com.l7tech.util.ClassUtils.isArrayClassName;
import com.l7tech.util.Functions.BinaryThrows;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class finder that uses the ConsoleAssertionRegistry.
 *
 * <p>This class can be used to support loading of classes from within a
 * modular assertion, it does not currently support loading of classes for
 * libraries packaged within a modular assertion.</p>
 */
public class ConsoleModularAssertionClassFinder implements BinaryThrows<Class,String,ClassNotFoundException,ClassNotFoundException> {

    //- PUBLIC

    public ConsoleModularAssertionClassFinder( @NotNull final ConsoleAssertionRegistry registry ) {
        this.registry = registry;
    }

    @Override
    public Class call( final String className, final ClassNotFoundException e ) throws ClassNotFoundException {
        if (logger.isLoggable( Level.FINE )) {
            logger.log( Level.FINE, "Loading class: " + className );
        }

        final boolean isArrayClass = isArrayClassName( className );
        String cleanClassName = className;
        if ( isArrayClass ) {
            cleanClassName = getArrayElementClassName( className );
        }

        final Option<String> moduleName = optional( registry.getModuleNameMatchingPackage( cleanClassName ) );
        if ( moduleName.isSome() ) {
            for ( final Assertion assertion : registry.getAssertions() ) {
                if ( moduleName.some().equals( registry.getModuleNameMatchingPackage( assertion.getClass().getName() ) ) ) {
                    if ( isArrayClass ) {
                        return Class.forName( className, true, assertion.getClass().getClassLoader() );
                    } else {
                        return assertion.getClass().getClassLoader().loadClass( className );
                    }
                }
            }
        }

        throw e;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ConsoleModularAssertionClassFinder.class.getName());

    private final ConsoleAssertionRegistry registry;
}
