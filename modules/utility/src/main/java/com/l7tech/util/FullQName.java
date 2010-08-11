package com.l7tech.util;

import java.io.Serializable;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing QName in one of the formats NAME, PREFIX:NAME, {URI}NAME, or {URI}PREFIX:NAME.
 */
public class FullQName implements Serializable {
    private static final long serialVersionUID = 2984398572304958427L;

    // Regex matching a QName pattern in one of the formats NAME, PREFIX:NAME, {URI}NAME, or {URI}PREFIX:NAME
    private static final Pattern EXPAT_QNAME_PATTERN = Pattern.compile("^(?:\\{([^}]*)\\})?(?:([^:]+):)?(.+?)$");
    private String nsUri;
    private String prefix;
    private String local;

    public FullQName() {
    }

    public FullQName(String encodedString) throws ParseException {
        FullQName parsed = valueOf(encodedString);
        this.nsUri = parsed.getNsUri();
        this.prefix = parsed.getPrefix();
        this.local = parsed.getLocal();
    }

    public FullQName(String nsUri, String prefix, String local) {
        this.nsUri = nsUri;
        this.prefix = prefix;
        this.local = local;
    }

    public String getNsUri() {
        return nsUri;
    }

    public void setNsUri(String nsUri) {
        this.nsUri = nsUri;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getFullName() {
        return prefix == null
                ? local
                : prefix + ":" + local;
    }

    public static FullQName valueOf(String text) throws ParseException {
        // Check namespace URI, if any
        Matcher matcher = EXPAT_QNAME_PATTERN.matcher(text);
        if (!matcher.matches())
            throw new ParseException("Attribute name is formatted incorrectly.", 0);

        String nsUri = matcher.group(1);
        String prefix = matcher.group(2);
        String local = matcher.group(3);

        return new FullQName(nsUri, prefix, local);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (nsUri != null && nsUri.length() > 0) {
            sb.append('{').append(nsUri).append('}');
        }

        if (prefix != null && prefix.length() > 0) {
            sb.append(prefix).append(':');
        }

        sb.append(local);

        return sb.toString();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FullQName fullQName = (FullQName) o;

        if (local != null ? !local.equals(fullQName.local) : fullQName.local != null) return false;
        if (nsUri != null ? !nsUri.equals(fullQName.nsUri) : fullQName.nsUri != null) return false;
        if (prefix != null ? !prefix.equals(fullQName.prefix) : fullQName.prefix != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = nsUri != null ? nsUri.hashCode() : 0;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (local != null ? local.hashCode() : 0);
        return result;
    }
}
