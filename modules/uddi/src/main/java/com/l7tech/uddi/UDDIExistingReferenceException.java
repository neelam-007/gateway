package com.l7tech.uddi;

/**
 * UDDI Exception thrown when an existing reference would be overwritten.
 *
 * @author Steve Jones
*/
public class UDDIExistingReferenceException extends UDDIException {

    //- PUBLIC

    public UDDIExistingReferenceException(String key) {
        this.key = key;
    }

    public String getKeyValue() {
        return key;
    };

    //- PRIVATE

    private final String key;
    
}
