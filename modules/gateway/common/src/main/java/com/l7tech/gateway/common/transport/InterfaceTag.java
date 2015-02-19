package com.l7tech.gateway.common.transport;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NameableEntity;
import com.l7tech.util.Charsets;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents zero or more named sets of IP address patterns.
 */
public class InterfaceTag implements Entity, NameableEntity {
    /** Conventional name of a property that might contain an interface tag set in string format. */
    public static final String PROPERTY_NAME = "interfaceTags";

    /** Pattern that matches a valid InterfaceTag name. */
    private static final Pattern NAME_PAT = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");

    /** Pattern that matches a single InterfaceTag in String format. */
    private static final Pattern SINGLE_PAT = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)\\(([0-9a-fA-F.,:/]*)\\)");

    private String name;
    private Set<String> ipPatterns; // TODO use a more appropriate class for this than String

    public InterfaceTag(String name, Set<String> ipPatterns) {
        if (name == null || ipPatterns == null) throw new NullPointerException();
        if (name.trim().length() < 1) throw new IllegalArgumentException("empty name");
        this.name = name;
        this.ipPatterns = new LinkedHashSet<String>(ipPatterns);
    }

    /**
     * Parse a single InterfaceTag out of string format.
     * This will fail unless the passed-in CharSequence contains exactly one InterfaceTag
     * with no preamble or unused trailing characters.
     *
     * @param in a String format InterfaceTag, ie "foo()" or "bar(255.10" or "blat(20)" or "mumble(127,30.33.55,24.33.8.0/24)",  Required.
     * @return the parsed tag.  Never null.
     * @throws ParseException if the input is not a valid interface tag.
     */
    public static InterfaceTag parseSingle(CharSequence in) throws ParseException {
        Matcher matcher = SINGLE_PAT.matcher(in);
        if (!matcher.matches())
            throw new ParseException("Invalid InterfaceTag format", 0);
        String name = matcher.group(1);
        String patString = matcher.group(2);
        String[] patArray = "".equals(patString) ? new String[0] : patString.split("\\,");
        for ( final String pattern : patArray ) {
            if ( !isValidPattern(pattern) ) 
                throw new ParseException("Invalid InterfaceTag format", 0);
        }
        return new InterfaceTag(name, new LinkedHashSet<String>(Arrays.asList(patArray)));
    }

    /**
     * Parse a semicolon-delimited list of InterfaceTags out of String format.
     * This will fail unless the input String contains exactly zero or more InterfaceTags
     * in String format with no extraneous leading or trailing characters.
     *
     * @param in zero or more String format InterfaceTags, ie "" or "foo();bar(127.0.0.1);mumble(127.0.0.1,30.33.55,24.33.8.0/24)".  Required.
     * @return
     * @throws ParseException
     */
    public static Set<InterfaceTag> parseMultiple(String in) throws ParseException {
        if(in.isEmpty()) {
            return Collections.emptySet();
        }
        String[] tagStrings = in.split("\\;");
        Set<InterfaceTag> ret = new LinkedHashSet<InterfaceTag>();
        for (String tagString : tagStrings)
            ret.add(parseSingle(tagString));
        return ret;
    }

    /**
     * returns the interface tags name as its id
     *
     * @return The name of the interface tag
     */
    @Override
    public String getId() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getIpPatterns() {
        return ipPatterns;
    }

    public void setIpPatterns(Set<String> ipPatterns) {
        this.ipPatterns = ipPatterns;
    }

    public String toString() {
        return name + "(" + TextUtils.join(",", ipPatterns) + ")";
    }

    public static String toString(Set<InterfaceTag> tags) {
        return TextUtils.join(";", tags).toString();
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PAT.matcher(name).matches();
    }

    public static boolean isValidPattern(String pattern) {
        return pattern != null && (InetAddressUtil.isValidIpv4Pattern(pattern) || InetAddressUtil.isValidIpv6Pattern(pattern) );
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InterfaceTag that = (InterfaceTag) o;

        if (!ipPatterns.equals(that.ipPatterns)) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 31 * result + ipPatterns.hashCode();
        return result;
    }

    /**
     * Returns the synthetic id for an interface tag, this is used in wsman and restman.
     *
     * @param interfaceTag The interface tag to get the synthetic id for
     * @return The synthetic id for the given interface tag
     */
    @NotNull
    public static String getSyntheticId(@NotNull final InterfaceTag interfaceTag) {
        return UUID.nameUUIDFromBytes(interfaceTag.getName().getBytes(Charsets.UTF8)).toString();
    }
}
