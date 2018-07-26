package com.l7tech.common.io;

/**
 * Dedicated RuntimeException for (currently) impossible scenarios of a DocumentBuilder being unavailable
 */
class DocumentBuilderAvailabilityException extends RuntimeException {
    DocumentBuilderAvailabilityException(String message) {
        super(message);
    }

    DocumentBuilderAvailabilityException(Exception e) {
        super(e);
    }

    DocumentBuilderAvailabilityException(String message, Exception e) {
        super(message, e);
    }
}
