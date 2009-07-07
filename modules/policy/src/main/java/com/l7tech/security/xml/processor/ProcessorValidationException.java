package com.l7tech.security.xml.processor;

/**
 * Extension of ProcessorException for validation errors.
 *
 * <p>This exception is for an error (or something unacceptable) in the message
 * being processed.</p>
 *
 * <p>It is expected that validation errors occur for well known reasons,
 * so would likely not require the associated stack trace to be recorded.</p>
 */
public class ProcessorValidationException extends ProcessorException {

    //- PUBLIC

    /**
     * Create an exception with the given message.
     *
     * @param message The exception message (required).
     */
    public ProcessorValidationException( final String message ) {
        super( notNull(message) );
    }

    //- PRIVATE

    private static String notNull( final String text ) {
        if ( text == null ) throw new IllegalArgumentException("Message must not be null."); 
        return text;
    }
}
