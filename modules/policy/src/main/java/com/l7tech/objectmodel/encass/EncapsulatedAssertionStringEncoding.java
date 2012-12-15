package com.l7tech.objectmodel.encass;

import com.l7tech.common.io.CertUtils;
import com.l7tech.policy.variable.DataType;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class that handles encoding argument parameter values from runtime types to/from string values.
 */
public class EncapsulatedAssertionStringEncoding {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionStringEncoding.class.getName());

    /**
     * Convert a value (assumed to be assignable to the primary representation class of the specified DataType) into a String
     * for storage within an encapsulated assertion bean's parameters map.
     *
     * @param dataType data type to encode
     * @param value value of type dataType.getValueClasses()[0]
     * @return an encoded string, or null if one could not be produced
     */
    public static String encodeToString(@NotNull DataType dataType, @Nullable Object value) {
        Functions.Unary<String, Object> encoder = STRING_ENCODERS.get(dataType);
        if (encoder != null)
            return encoder.call(value);
        return value == null ? null : value.toString();
    }

    /**
     * Convert a string value (assumed to have been produced by {@link #encodeToString}) into an instance of the
     * primary representation class of the corresponding DataType.
     *
     * @param dataType data type to decode
     * @param valueString encoded string, or null if value was null
     * @return a value of type dataType.getValueClasses()[0], or null if the value was null or could not be decoded
     */
    public static Object decodeFromString(DataType dataType, String valueString) {
        Functions.Unary<Object, String> decoder = STRING_DECODERS.get(dataType);
        if (decoder != null) {
            return decoder.call(valueString);
        }
        return valueString;
    }

    // Anything not listed will just call .toString on the value object
    private static final Map<DataType, Functions.Unary<String,Object>> STRING_ENCODERS = CollectionUtils.MapBuilder.<DataType, Functions.Unary<String,Object>>builder()
        .put(DataType.DATE_TIME, dateTimeEncoder())
        .put(DataType.CERTIFICATE, certEncoder())
        .put(DataType.BINARY, byteArrayEncoder())
        .unmodifiableMap();

    // Anything not listed will just return the string itself as the value object
    private static final Map<DataType, Functions.Unary<Object,String>> STRING_DECODERS = CollectionUtils.MapBuilder.<DataType, Functions.Unary<Object,String>>builder()
        .put(DataType.DATE_TIME, dateTimeDecoder())
        .put(DataType.CERTIFICATE, certDecoder())
        .put(DataType.BINARY, byteArrayDecoder())
        .put(DataType.BOOLEAN, booleanDecoder())
        .put(DataType.CERTIFICATE, certDecoder())
        .unmodifiableMap();

    private static Functions.Unary<String, Object> dateTimeEncoder() {
        return new Functions.Unary<String, Object>() {
            @Override
            public String call(Object value) {
                return value instanceof Date ? ISO8601Date.format((Date) value) : null;
            }
        };
    }

    private static Functions.Unary<String,Object> certEncoder() {
        return new Functions.Unary<String, Object>() {
            @Override
            public String call(Object value) {
                try {
                    return value instanceof X509Certificate ? CertUtils.encodeAsPEM((X509Certificate) value) : null;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to encode certificate property: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    return null;
                }
            }
        };
    }

    private static Functions.Unary<String,Object> byteArrayEncoder() {
        return new Functions.Unary<String, Object>() {
            @Override
            public String call(Object value) {
                // The binary value is stored in the param string as base64 (for compactness) even though it is edited in the GUI as a hex string (for ease of editing).
                return value instanceof byte[] ? HexUtils.encodeBase64((byte[]) value) : null;
            }
        };
    }

    private static Functions.Unary<Object, String> dateTimeDecoder() {
        return new Functions.Unary<Object, String>() {
            @Override
            public Object call(String valueString) {
                try {
                    return valueString == null || valueString.trim().length() < 1 ? null : ISO8601Date.parse(valueString);
                } catch (ParseException e) {
                    logger.log(Level.WARNING, "Date property not ISO 8601 string: " + valueString + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    return null;
                }
            }
        };
    }

    private static Functions.Unary<Object, String> certDecoder() {
        return new Functions.Unary<Object, String>() {
            @Override
            public Object call(String valueString) {
                try {
                    return valueString == null || valueString.trim().length() < 1 ? null : CertUtils.decodeFromPEM(valueString, false);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Certificate property not a valid PEM X.509 certificate: " + valueString + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    return null;
                }
            }
        };
    }

    private static Functions.Unary<Object, String> byteArrayDecoder() {
        return new Functions.Unary<Object, String>() {
            @Override
            public Object call(String valueString) {
                // The binary value is stored in the param string as base64 (for compactness) even though it is edited in the GUI as a hex string (for ease of editing).
                return valueString == null || valueString.trim().length() < 1 ? null : HexUtils.decodeBase64(valueString, true);
            }
        };
    }

    private static Functions.Unary<Object, String> booleanDecoder() {
        return new Functions.Unary<Object, String>() {
            @Override
            public Object call(String valueString) {
                return Boolean.valueOf(valueString);
            }
        };
    }
}
