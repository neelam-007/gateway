package com.l7tech.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Utility class to process command line arguments.
 */
public class Cli {

    //- PUBLIC

    /**
     * Process the given arguments for the specified target.
     *
     * @param target The target for the arguments (required)
     * @param args The arguments to process (required)
     * @param unprocessedArgs The map to receive unprocessed arguments (optional)
     */
    public static void process( final Object target,
                                final String[] args,
                                final Map<? super Integer, ? super String> unprocessedArgs ) throws CliException {
        if ( target == null ) throw new IllegalArgumentException( "target is required" );
        if ( args == null ) throw new IllegalArgumentException( "args is required" );

        final Map<Arg,Field> argMap = getArgumentFields( target );
        final Set<Arg> processedArgs = new HashSet<Arg>();

        // process arguments
        for ( int i=0; i < args.length; i++ ) {
            final String argText = args[i];
            final Arg arg = matchArg( argMap.keySet(), argText );

            if ( arg == null ) {
                if ( unprocessedArgs == null ) {
                    throw new CliException( argText, null, CliException.Reason.UNKNOWN );
                } else {
                    unprocessedArgs.put( i, argText );
                    continue;
                }
            }
            processedArgs.add( arg );

            String argValue = arg.value().isEmpty() ? null : arg.value();
            if ( argValue == null && i+1 < args.length ) {
                argValue = args[++i];
            }

            if ( argValue == null && arg.required() ) {
                throw new CliException( argText, null, CliException.Reason.MISSING );
            }

            final Field field = argMap.get( arg );
            final Object value = castValue(argText, argValue, field);
            try {
                field.set( target, value );
            } catch (IllegalAccessException e) {
                throw ExceptionUtils.wrap(e);
            }
        }

        // check for missing required arguments
        final Set<Arg> notProcessed = new HashSet<Arg>( argMap.keySet() );
        notProcessed.removeAll(processedArgs);
        for ( Arg arg : notProcessed ) {
            if ( arg.required() && arg.value().isEmpty() ) {
                throw new CliException( arg.name()[0], null, CliException.Reason.MISSING );     
            }
        }
    }

    /**
     * Output usage information to the given appender.
     *
     * @param target The target object (required)
     * @param out The appendable to output to (required)
     */
    public static void usage( final Object target, final Appendable out ) throws IOException {
        if ( target == null ) throw new IllegalArgumentException( "target is required" );
        if ( out == null ) throw new IllegalArgumentException( "out is required" );

        final Map<Arg,Field> argMap = getArgumentFields( target );
        final Set<Arg> argSet = argMap.keySet();
        final List<String> argNames = new ArrayList<String>();

        for ( final Arg arg : argSet ){
            argNames.add( arg.name()[0] );
        }
        Collections.sort( argNames, String.CASE_INSENSITIVE_ORDER );

        int commandWidth = 20;
        for ( final String argName : argNames  ) {
            final Arg arg = matchArg( argSet, argName );

            int width = INDENT_WIDTH + argName.length();
            if ( arg.value().isEmpty() ) {
                width += VALUE_TEXT.length();
            }

            commandWidth = Math.max( commandWidth, width );
        }

        final StringBuilder newlineBuffer = new StringBuilder( commandWidth + 5 );
        newlineBuffer.append('\n');
        for ( int i=0; i<commandWidth+3; i++ ) newlineBuffer.append( ' ' );
        final String newlineReplacement = newlineBuffer.toString();

        for ( final String argName : argNames ) {
            final Arg arg = matchArg( argSet, argName );

            int width = INDENT_WIDTH + argName.length();
            for ( int i=0; i<INDENT_WIDTH; i++) out.append( ' ' );
            out.append( argName );
            if ( arg.value().isEmpty() ) {
                width += VALUE_TEXT.length();
                out.append( " VALUE" );
            }

            for ( int i=width; i<commandWidth; i++) out.append( ' ' );

            out.append( " : " );
            out.append( arg.description().replace("\n", newlineReplacement) );
            out.append( '\n' );
        }
    }

    /**
     * Annotation for target fields
     */
    @Documented
    @Retention(value = RUNTIME)
    @Target(FIELD)
    public @interface Arg {
        /**
         * The name and aliases for the argument.
         */
        String[] name();

        /**
         * The description for the argument.
         */
        String description();

        /**
         * The value to use when the argument is present.
         */
        String value() default "";

        /**
         * Is the argument required.
         */
        boolean required() default true;
    }

    /**
     * Exception thrown for missing or invalid arguments.
     */
    @SuppressWarnings({"serial"})
    public static class CliException extends Exception {
        enum Reason { MISSING, INVALID, UNKNOWN }

        private final String arg;
        private final String value;
        private final Reason reason;

        public CliException( final String arg, final String value, final Reason reason ) {
            super( buildMessage( arg, value, reason ) );
            this.arg = arg;
            this.value = value;
            this.reason = reason;
        }

        public String getArg() {
            return arg;
        }

        public String getValue() {
            return value;
        }

        public Reason getReason() {
            return reason;
        }

        private static String buildMessage( final String arg, final String value, final Reason reason ) {
            String message = "Argument error";

            switch (reason) {
                case MISSING:
                    message = "Missing argument '"+arg+"'.";
                    break;
                case INVALID:
                    message = "Invalid value '"+value+"' for argument '"+arg+"'.";
                    break;
                case UNKNOWN:
                    message = "Argument not recognised '"+arg+"'.";
                    break;
            }

            return message;
        }
    }

    //- PRIVATE

    private static final int INDENT_WIDTH = 4;
    private static final String VALUE_TEXT = " VALUE";

    /**
     * Get the annotated fields for the target 
     */
    private static Map<Arg,Field> getArgumentFields( final Object target ) {
        final Map<Arg,Field> argMap = new HashMap<Arg,Field>();
        final Class<?> targetClass = target.getClass();
        final Field[] fields = targetClass.getDeclaredFields();

        for ( final Field field : fields ) {
            final Arg arg = field.getAnnotation(Arg.class);

            if ( arg != null ) {
                if ( arg.name().length==0 ) throw new IllegalStateException("Arg annotation missing name for field '"+field.getName()+"'."); 
                field.setAccessible(true);
                argMap.put( arg, field );
            }
        }

        return argMap;
    }

    /**
     * Find matching arg
     */
    private static Arg matchArg( final Iterable<Arg> args, final String argName ) {
        Arg match = null;

        for ( final Arg arg : args ) {
            if ( ArrayUtils.contains( arg.name(), argName ) ) {
                if ( match != null ) throw new IllegalStateException("Duplicated argument '"+argName+"'.");
                match = arg;
            }
        }

        return match;
    }

    /**
     * Cast the text value to the field type
     */
    private static Object castValue( final String argText, final String argValue, final Field field ) throws CliException {
        final Object value;

        final Class<?> type = field.getType();
        if ( type.isAssignableFrom( String.class ) ) {
            value = argValue;
        } else if ( type.isAssignableFrom( Boolean.class ) ||
                    type.isAssignableFrom( Boolean.TYPE ) ) {
            value = Boolean.valueOf(argValue);
        } else if ( type.isAssignableFrom( Integer.class ) ||
                    type.isAssignableFrom( Integer.TYPE ) ) {
            try {
                value = Integer.valueOf(argValue);
            } catch ( NumberFormatException nfe ) {
                throw new CliException( argText, argValue, CliException.Reason.INVALID );
            }
        } else if ( type.isAssignableFrom( File.class ) ) {
            value = new File(argValue);
        } else {
            throw new IllegalStateException( "Unsupported field type '"+type.getName()+"'." );
        }
        return value;
    }

}
