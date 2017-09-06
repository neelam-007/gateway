package com.l7tech.json;

import com.fasterxml.jackson.core.JsonLocation;

/**
 * HiddenSourceJsonLocation extends JsonLocation to hide the source ref
 */
public class HiddenSourceJsonLocation extends JsonLocation {

    public HiddenSourceJsonLocation(JsonLocation location) {
        super(location.getSourceRef(), location.getByteOffset(), location.getCharOffset(), location.getLineNr(), location.getColumnNr());
    }

    /**
     * Provides the String representation of the JsonLocation without the SourceRef
     * @return string representation of the JsonLocation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(80);
        sb.append("[line: ");
        sb.append(getLineNr());
        sb.append(", column: ");
        sb.append(getColumnNr());
        sb.append(']');
        return sb.toString();
    }
}
