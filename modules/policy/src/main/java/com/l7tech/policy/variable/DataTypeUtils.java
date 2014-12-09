package com.l7tech.policy.variable;

import com.l7tech.message.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Utility static methods for working with {@link DataType}.  These are not part of the public Layer 7 API.
 * Some of these could be added as methods of DataType except for the need to minimize changes to the public API.
 * more than necessary.
 */
public class DataTypeUtils {

    /**
     * Find a DataType corresponding to the specified Java class, if there is one.
     * <p/>
     * This finds only exact matches, and will not accept subclasses, so (for example) a value of
     * java.util.Date will return DataType.DATE_TIME, but java.sql.Date will not (even though java.sql.Date
     * is a subtype of java.util.Date).
     * <p/>
     * The match will recognize the most-preferred representation type and may not recognize alternative types
     * (which may not be unique).
     * <p/>
     * One exception is DataType.MESSAGE, which is mapped from the Message class rather than from the Object class.
     * <p/>
     * Some DataType representation classes may not be recognized by this method.
     * At time of writing this includes BLOB and CLOB.
     *
     * @param value class to examine, eg BigInteger.class
     * @return a corresponding DataType, eg DataType.INTEGER, or null if there was no exact match.
     */
    @Nullable
    public static DataType getDataTypeForClass( @NotNull Class<?> value ) {
        final DataType dataType;

        if ( value == String.class ) {
            dataType = DataType.STRING;
        } else if ( value == BigInteger.class ) {
            dataType = DataType.INTEGER;
        } else if ( value == BigDecimal.class ) {
            dataType = DataType.DECIMAL;
        } else if ( value == Boolean.class ) {
            dataType = DataType.BOOLEAN;
        } else if ( value == Double.class ) {
            dataType = DataType.FLOAT;
        } else if ( value == Date.class ) {
            dataType = DataType.DATE_TIME;
        } else if ( value == Message.class ) {
            dataType = DataType.MESSAGE;
        } else if ( value == X509Certificate.class ) {
            dataType = DataType.CERTIFICATE;
        } else if ( value == Element.class ) {
            dataType = DataType.ELEMENT;
        } else {
            dataType = null;
        }

        return dataType;
    }

    private DataTypeUtils() {}
}
