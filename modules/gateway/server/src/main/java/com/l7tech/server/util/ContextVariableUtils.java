package com.l7tech.server.util;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.*;
import java.util.regex.Pattern;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Functions.flatmap;

/**
 * Utility class for common processing of context variables values.
 *
 */
public class ContextVariableUtils {
    // Limit streams read into memory to 2mb by default.
    private static final int DEFAULT_MAX_STREAM_BYTES =
            SyspropUtil.getInteger( "com.l7tech.server.util.ContextVariableUtils.convertToBinary.stream.maxBytes", 2 * 1024 * 1024 );

    /**
     * Get all Strings referenced from the expression. This expression may be made up of variable and non variable
     * string references.
     *
     * The splitPattern is applied to both the expression string and also any resolved variables (single or multivalued)
     * which contain strings. In the case of multi valued the splitPattern is applied to each item.
     *
     * @param expression The expression to extract strings from. This should be delimited as expected by the splitPattern
     * and may contain strings and variable references.
     * @param serverVariables Map of available variables.
     * @param auditor The auditor to audit to.
     * @param splitPattern The Pattern to apply to all resolved variable values (including multi valued individual items)
     * in order to obtain the entire list of runtime values for the expression.
     * @param callback called if a value resolved from expression is not a string.
     * @return The list of Strings extracted.
     */
    public static List<String> getAllResolvedStrings(@NotNull final String expression,
                                                     @NotNull final Map<String, Object> serverVariables,
                                                     @NotNull final Audit auditor,
                                                     @NotNull final Pattern splitPattern,
                                                     @Nullable final Functions.UnaryVoid<Object> callback) {

        return flatmap(list(splitPattern.split(expression)), new Functions.UnaryThrows<Iterable<String>, String, RuntimeException>() {
            @Override
            public Iterable<String> call(String token) throws RuntimeException {
                List<Object> listToProcess = (!Syntax.validateStringOnlyReferencesVariables(token)) ?
                        // if the expression is not a single variable reference, then it's an expression
                        new ArrayList<Object>(Arrays.asList(ExpandVariables.process(token, serverVariables, auditor))):
                        // otherwise it's a single variable reference
                        ExpandVariables.processNoFormat(token, serverVariables, auditor);

                return ContextVariableUtils.getStringsFromList(
                        listToProcess,
                        splitPattern,
                        callback);
            }
        });
    }

    /**
     * Get all Strings from the List of possible objects. Expected to be the output of
     * {@link com.l7tech.server.policy.variable.ExpandVariables#processNoFormat(String, java.util.Map, com.l7tech.gateway.common.audit.Audit, boolean)}
     *
     * Each String found in the list will have the split pattern applied to it if not null.
     *
     * @param objectList list to extract strings from
     * @param splitPattern pattern to split found strings on. If null no splitting of stings is done.
     * @param notStringCallback if not null, callback will be invoked with any non string value found
     * @return list of all found strings
     */
    public static List<String> getStringsFromList(@NotNull final List<Object> objectList,
                                                  @Nullable final Pattern splitPattern,
                                                  @Nullable final Functions.UnaryVoid<Object> notStringCallback) {
        return flatmap(objectList, new Functions.UnaryThrows<Iterable<String>, Object, RuntimeException>() {
            @Override
            public Iterable<String> call(Object val) throws RuntimeException {
                if (val instanceof String) {
                    String customVal = (String) val;
                    if (splitPattern != null) {
                        final String[] authMethods = splitPattern.split(customVal);
                        return Functions.grep(Arrays.asList(authMethods), TextUtils.isNotEmpty());
                    } else {
                        return Functions.grep(Arrays.asList(customVal), TextUtils.isNotEmpty());
                    }
                } else {
                    if (notStringCallback != null) {
                        notStringCallback.call(val);
                    }
                    return Collections.emptyList();
                }
            }
        });
    }

    /**
     * Exception thrown if a context variable value cannot be converted to binary by {@link #convertContextVariableValueToByteArray }.
     */
    public static class NoBinaryRepresentationException extends Exception {
        public NoBinaryRepresentationException() {
        }

        public NoBinaryRepresentationException( String message ) {
            super( message );
        }

        public NoBinaryRepresentationException( String message, Throwable cause ) {
            super( message, cause );
        }

        public NoBinaryRepresentationException( Throwable cause ) {
            super( cause );
        }
    }

    /**
     * Figure out how to convert the specified context variable value into a binary array.
     * <p/>
     * Things currently converted to binary by this method:
     * <ul>
     *     <li>Byte arrays: returned as-is</li>
     *     <li>Message: entire message body stream (up to maxLength) is read into byte array and returned</li>
     *     <li>PartInfo: entire message part body stream (up to maxLength) read into byte array and returned</li>
     *     <li>java.security.cert.Certificate: returns encoded form of certificate (e.g. DER for an X.509 certificate)</li>
     *     <li>String or CharSequence: converted to bytes using specified encoding</li>
     * </ul>
     * <p/>
     * Examples of things that should <b>not</b> be converted to binary by this method:
     * <ul>
     *     <li>Arbitrary objects that implement Serializable: this is very Java-specific and not the kind of meaningful binary representation
     *     we want to expose to users.
     *     Also, there are objects like X509Certificate that are Serializable but whose meaningful binary representation differs.</li>
     *     <li>Automatic decoding of encodings such as Base-64 or URL encoding.  Such encodings must be explicitly decoded
     *     earlier in a policy, if this is desired.</li>
     * </ul>
     *
     * @param obj object to convert to binary.  If null, this method will throw NoBinaryRepresentationException.
     * @param maxLength maximum length of stream to read into memory, for value types that produce streams, or -1 to use default limit.
     *                  <b>NOTE:</b> A limit of zero is <em>valid</em> and will forbid non-empty streams from being read.
     * @param charset charset to use if a CharSequence must be encoded, or null to default to ISO-8859-1 (chosen
     *                because it is the most common character encoding that is binary-transparent).
     * @return a byte array containing a binary representation of the provided context variable value.  Never null.
     * @throws IOException if the input value cannot be read or encoded.
     * @throws NoSuchPartException if the input value is a Message or PartInfo whose body has been streamed away.
     * @throws NoBinaryRepresentationException if no unambiguous and meaningful binary representation can be found
     *                                         for the specified context variable value.
     */
    @NotNull
    public static byte[] convertContextVariableValueToByteArray( @Nullable Object obj, int maxLength, @Nullable Charset charset )
            throws IOException, NoSuchPartException, NoBinaryRepresentationException
    {
        if ( maxLength < 0 )
            maxLength = DEFAULT_MAX_STREAM_BYTES;

        if ( null == obj ) {
            throw new NoBinaryRepresentationException( "Unable to find binary representation for null variable" );
        } else if ( obj instanceof byte[] ) {
            return (byte[]) obj;
        } else if ( obj instanceof Message ) {
            Message message = (Message) obj;
            try ( InputStream stream = message.getMimeKnob().getEntireMessageBodyAsInputStream( false ) ) {
                return IOUtils.slurpStream( stream, maxLength );
            }
        } else if ( obj instanceof PartInfo ) {
            PartInfo partInfo = (PartInfo) obj;
            try ( InputStream stream = partInfo.getInputStream( false ) ) {
                return IOUtils.slurpStream( stream, maxLength);
            }
        } else if ( obj instanceof CharSequence ) {
            CharSequence charSequence = (CharSequence) obj;
            if ( null == charset )
                charset = Charsets.ISO8859;
            return charSequence.toString().getBytes( charset );
        } else if ( obj instanceof Certificate ) {
            Certificate certificate = (Certificate) obj;
            try {
                return certificate.getEncoded();
            } catch ( CertificateEncodingException e ) {
                throw new IOException( e );
            }
        } else {
            throw new NoBinaryRepresentationException( "Unable to find binary representation for variable of type " + obj.getClass() );
        }
    }

}
