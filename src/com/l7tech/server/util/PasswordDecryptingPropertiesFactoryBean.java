package com.l7tech.server.util;

import org.springframework.beans.factory.config.PropertiesFactoryBean;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Extends PropertiesFactoryBean to support decrypting encrypted passwords.
 *
 * <p>Supports resolution of property values based on other properties.</p>
 */
public class PasswordDecryptingPropertiesFactoryBean extends PropertiesFactoryBean {

    //- PUBLIC

    public PasswordDecryptingPropertiesFactoryBean( final PropertiesDecryptor propertiesDecryptor ) {
        this.propertiesDecryptor = propertiesDecryptor;
        if (propertiesDecryptor == null) throw new IllegalArgumentException("A PropertiesDecryptor is required.");
    }

    /**
     *
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     *
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    //- PROTECTED

    @Override
    protected Properties mergeProperties() throws IOException {
        Properties props = super.mergeProperties();

        propertiesDecryptor.decryptEncryptedPasswords( props );

        expandProperties( props );
        
        for ( Map.Entry<Object,Object> entry : props.entrySet() ) {
            if ( propertiesDecryptor.isPasswordPropertyName( entry.getKey() ) ) {
                logger.log( Level.CONFIG, "Using property ''{0}'' = <HIDDEN>.", entry.getKey() );
            } else {
                logger.log( Level.CONFIG, "Using property ''{0}'' = ''{1}''.", new Object[]{entry.getKey(), entry.getValue()} );
            }
        }

        return props;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(PasswordDecryptingPropertiesFactoryBean.class.getName());
    private static final String DEFAULT_PLACEHOLDER_PREFIX = "${";
    private static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    private final PropertiesDecryptor propertiesDecryptor;
    private String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;
    private String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

    /**
     *
     */
    @SuppressWarnings({"unchecked"})
    private void expandProperties( final Properties properties ) {
        for ( String name : Collections.list((Enumeration<String>)properties.propertyNames()) ) {
            String value = properties.getProperty(name);
            String updatedValue = parseStringValue( value , properties, new HashSet<String>() );
            properties.setProperty( name, updatedValue );
        }
    }

	/**
     * See {@link org.springframework.beans.factory.config.PropertyPlaceholderConfigurer} for inspiration.
	 */
	private String parseStringValue( final String strVal, final Properties props, final Set<String> visitedPlaceholders ) {
		StringBuffer buf = new StringBuffer(strVal);

		int startIndex = strVal.indexOf(this.placeholderPrefix);
		while (startIndex != -1) {
			int endIndex = findPlaceholderEndIndex(buf, startIndex);
			if (endIndex != -1) {
				String placeholder = buf.substring(startIndex + this.placeholderPrefix.length(), endIndex);
				if (!visitedPlaceholders.add(placeholder)) {
                    // recursion, ignore this property value
                    break;
                }

				// Recursive invocation, parsing placeholders contained in the placeholder key.
				placeholder = parseStringValue(placeholder, props, visitedPlaceholders);

				// Now obtain the value for the fully resolved key...
				String propVal = props.getProperty(placeholder);
				if (propVal != null) {
					// Recursive invocation, parsing placeholders contained in the
					// previously resolved placeholder value.
					propVal = parseStringValue(propVal, props, visitedPlaceholders);
					buf.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
					startIndex = buf.indexOf(this.placeholderPrefix, startIndex + propVal.length());
				}
				else {
					// Proceed with unprocessed value.
                    startIndex = buf.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
				}

				visitedPlaceholders.remove(placeholder);
			}
			else {
				startIndex = -1;
			}
		}

		return buf.toString();
	}

    /**
     *
     */
    private int findPlaceholderEndIndex( final CharSequence buf, final int startIndex ) {
		int index = startIndex + this.placeholderPrefix.length();
		int withinNestedPlaceholder = 0;
		while (index < buf.length()) {
			if (substringMatch(buf, index, this.placeholderSuffix)) {
				if (withinNestedPlaceholder > 0) {
					withinNestedPlaceholder--;
					index = index + this.placeholderSuffix.length();
				}
				else {
					return index;
				}
			}
			else if (substringMatch(buf, index, this.placeholderPrefix)) {
				withinNestedPlaceholder++;
				index = index + this.placeholderPrefix.length();
			}
			else {
				index++;
			}
		}
		return -1;
	}

    /**
     *
     */
    public static boolean substringMatch( final CharSequence str, final int index, final CharSequence substring ) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }
}
