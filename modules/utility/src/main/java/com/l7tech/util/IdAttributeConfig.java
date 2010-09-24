package com.l7tech.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Holds an index of attribute QNames organized for quickly checking whether a given attribute should
 * be recognized as an ID attribute.
 *
 * @see IdAttributeConfig#makeIdAttributeConfig(java.util.Collection
 */
public class IdAttributeConfig {
    final Collection<FullQName> idAttrsInPreferenceOrder;

    IdAttributeConfig(Collection<FullQName> idAttrsInPreferenceOrder) {
        this.idAttrsInPreferenceOrder = idAttrsInPreferenceOrder;
    }

    /**
     * Creates an IdAttributeConfig that will recognize only the specified FullQName values as attributes.
     *
     * @param ids attributes to recognize as ID attributes.  Prefixes are ignored, except for the special value "local",
     *            which matches the namespace URI (if any) against the owning element rather than the attribute itself.
     *            Entries with no namespace URI are treated as instructions to recognize a local attribute.
     * @return an IdAttributeConfig that can be passed to @{link #getElementByIdMap}.
     */
    public static IdAttributeConfig makeIdAttributeConfig(Collection<FullQName> ids) {
        Collection<FullQName> idAttrsInPreferenceOrder = new ArrayList<FullQName>(ids);
        return new IdAttributeConfig(idAttrsInPreferenceOrder);
    }

    /**
     * Create an ID attribute config from the specified configuration string.
     *
     * @param idConfig a config formatted as a whitespace-separated sequence of qnames in the format
     *                 specified by {@link FullQName#valueOf(String)}.
     * @return a new IdAttributeConfig.  Never null.
     * @throws ParseException if the config string cannot be parsed.
     */
    public static IdAttributeConfig fromString(String idConfig) throws ParseException {
        if (idConfig.length() < 1)
            throw new ParseException("config string is empty", 0);
        String[] qnstrs = idConfig.split("\\s+");
        Collection<FullQName> qns = new ArrayList<FullQName>();
        for (String qnstr : qnstrs) {
            if (qnstr.length() < 1)
                throw new ParseException("config string contians empty qname", 0);
            qns.add(FullQName.valueOf(qnstr));
        }
        return new IdAttributeConfig(qns);
    }

}
