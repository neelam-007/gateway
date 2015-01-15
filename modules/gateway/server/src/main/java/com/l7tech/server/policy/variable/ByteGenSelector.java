package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ByteGen;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.HexUtils;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Selector for an object that implements ByteGen, which can generate a specified number of bytes.
 */
public class ByteGenSelector implements ExpandVariables.Selector<ByteGen> {
    static final String PROP_MAX_BYTES = "com.l7tech.server.policy.variable.ByteGen.maxBytes";
    static final int DEFAULT_MAX_BYTES = 10 * 1024 * 1024;

    private static final String SUFFIX_HEX = "hex";
    private static final String SUFFIX_BASE64 = "base64";
    private static final String SUFFIX_BINARY = "binary";
    private static final String SUFFIX_INTEGER = "integer";
    private static final String SUFFIX_UNSIGNED = "unsigned";

    // optional ( Digits followed by dot or end of input ) followed by ( zero or more word chars ) followed by end of input
    private static final Pattern PATTERN_NUMBER = Pattern.compile( "^(?:(\\d+)(?:\\.|$))?([a-zA-Z0-9\\_\\-]*)$" );

    @Override
    public Selection select( String contextName, ByteGen byteGen, String name, Syntax.SyntaxErrorHandler handler, boolean strict ) {
        Matcher matcher = PATTERN_NUMBER.matcher( name );
        if ( matcher.matches() ) {
            String numStr = matcher.group( 1 );
            String suffixStr = matcher.group( 2 );

            // Parse (optional) byte count number
            int num = 32;
            if ( numStr != null && numStr.length() > 0 ) {
                try {
                    num = Integer.parseInt( numStr );
                    int maxBytes = ConfigFactory.getIntProperty( PROP_MAX_BYTES, DEFAULT_MAX_BYTES );
                    if ( num < 1 ) {
                        String msg = handler.handleBadVariable(name + " for byte generator.  Invalid byte count; must be positive." );
                        if (strict)
                            throw new IllegalArgumentException(msg);
                        num = 1;
                    } else if ( num > maxBytes ) {
                        String msg = handler.handleBadVariable(name + " for byte generator.  Invalid byte count; must be less than " + maxBytes + "." );
                        if (strict)
                            throw new IllegalArgumentException(msg);
                        num = maxBytes;
                    }
                } catch ( NumberFormatException e) {
                    // Can't happen
                    String msg = handler.handleBadVariable(name + " for byte generator.  Invalid byte count; must be a valid number." );
                    if (strict)
                        throw new IllegalArgumentException(msg);
                }
            }

            byte[] bytes = new byte[ num ];
            byteGen.generateBytes( bytes, 0, num );

            if ( suffixStr.isEmpty() || SUFFIX_HEX.equalsIgnoreCase( suffixStr ) ) {
                return new Selection( HexUtils.hexDump( bytes ) );
            } else if ( SUFFIX_BASE64.equalsIgnoreCase( suffixStr ) ) {
                return new Selection( HexUtils.encodeBase64( bytes, true ) );
            } else if ( SUFFIX_BINARY.equalsIgnoreCase( suffixStr ) ) {
                return new Selection( bytes );
            } else if ( SUFFIX_INTEGER.equalsIgnoreCase( suffixStr ) ) {
                return new Selection( new BigInteger( bytes ) );
            } else if ( SUFFIX_UNSIGNED.equalsIgnoreCase( suffixStr ) ) {
                return new Selection( new BigInteger( 1, bytes ) );
            } // else FALLTHROUGH and fail
        }

        String msg = handler.handleBadVariable(name + " for byte generator. Unrecognized suffix; must be .hex, .b64 or .binary or .<num>.hex" );
        if (strict)
            throw new IllegalArgumentException(msg);
        return NOT_PRESENT;
    }

    @Override
    public Class<ByteGen> getContextObjectClass() {
        return ByteGen.class;
    }
}
