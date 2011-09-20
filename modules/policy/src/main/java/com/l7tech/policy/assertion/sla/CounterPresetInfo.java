package com.l7tech.policy.assertion.sla;

import com.l7tech.util.HexUtils;

import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class was migrated from a previous static class PresetInfo in RateLimitAssertion.
 * Now it is shared by RateLimitAssertion and ThroughputQuota.
 */
public class CounterPresetInfo {
    public static final Pattern presetFinder = Pattern.compile("^PRESET\\(([a-fA-F0-9]{16})\\)(.*)$");
    public static final Pattern defaultCustomExprFinder = Pattern.compile("^([a-fA-F0-9]{8})-?(.*)$");

    public static String makeDefaultCustomExpr(String uuid, String expr) {
        return (uuid != null ? uuid : makeUuid()).substring(8) + (expr == null || expr.length() < 1 ? "" : "-" + expr);
    }

    public static boolean isDefaultCustomExpr(String rawExpr, Map<String, String> counterNameTypes) {
        Matcher matcher = defaultCustomExprFinder.matcher(rawExpr);
        if (!matcher.matches())
            return false;
        String expr = matcher.group(2);
        return counterNameTypes.containsValue(expr);
    }

    /**
     * Generate a new counter name corresponding to the default preset (clientid).
     * @return a counter name similar to "PRESET(deadbeefcafebabe)${request.clientid}".  Never null or empty.
     */
    public static String makeDefaultCounterName(String presetDefault, String presetCustom, Map<String, String> counterNameTypes) {
        return findRawCounterName(presetDefault, makeUuid(), null, presetCustom, counterNameTypes);
    }

    /**
     * See if the specified raw counter name happens to match any of the user friendly presets we provide.
     * If a counter key is matched, this updates uuidOut[0] as a side effect.
     *
     * @param rawCounterName  raw counter name ot look up, ie "foo bar blatz ${mumble}"
     * @param uuidOut  An optional single-element String array to receive the UUID.
     *                 Any UUID found will be copied into the first element of this array, if present.
     * @return the key in counterNameTypes corresponding to the given raw counter name, or null for presetCustom.
     * for example given "PRESET(deadbeefcafebabe)${request.clientid}" this will return "User or client IP";
     * when given        "PRESET(abcdefabcdefabcd)" this will return "Gateway node (global)"; and
     * when given        "RateLimit-${request.clientid}" this will return null.
     */
    public static String findCounterNameKey(String rawCounterName, String[] uuidOut, String presetCustom, Map<String, String> counterNameTypes) {
        String foundKey = null;
        String foundUuid = null;

        Matcher matcher = presetFinder.matcher(rawCounterName);
        if (matcher.matches()) {
            String expr = matcher.group(2);
            if ((foundKey = findKeyForValue(counterNameTypes, expr)) != null)
                foundUuid = matcher.group(1);
        } else {
            matcher = defaultCustomExprFinder.matcher(rawCounterName);
            if (matcher.matches()) {
                String expr = matcher.group(2);
                if ((foundKey = findKeyForValue(counterNameTypes, expr)) != null)
                    foundUuid = makeUuid().substring(8) + matcher.group(1);
            }
        }

        if (foundUuid != null && uuidOut != null && uuidOut.length > 0) uuidOut[0] = foundUuid;
        return presetCustom.equals(foundKey) ? null : foundKey;
    }

    public static <K, V> K findKeyForValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet())
            if (entry.getValue().equals(value))
                return entry.getKey();
        return null;
    }

    /**
     * Create the raw counter name to save in the assertion instance given the specified counterNameKey.
     *
     * @param counterNameKey a counter name key from counterNameTypes, or null to use presetCustom.
     * @param uuid  the UUID to use to create unique counters.  Should be an 8-digit hex string.  Mustn't be null.
     * @param customExpr the custom string to use if counterNameKey is CUSTOM or null or can't be found in the map.
     *                    May be empty.  May only be null if counterNameKey is in the map and isn't CUSTOM.
     * @return the raw counter name to store into the assertion.  Never null or otherwise invalid.
     */
    public static String findRawCounterName(String counterNameKey, String uuid, String customExpr, String presetCustom, Map<String, String> counterNameTypes) {
        // If it claims to be custom, but looks just like a generated example, use whatever preset it was generated from
        if (counterNameKey == null || presetCustom.equals(counterNameKey))
            if (isDefaultCustomExpr(customExpr, counterNameTypes))
                counterNameKey = findCounterNameKey(customExpr, null, presetCustom, counterNameTypes);

        String presetExpr = counterNameKey == null ? null : counterNameTypes.get(counterNameKey);
        if (presetExpr == null || presetCustom.equals(counterNameKey))
            return customExpr;
        return "PRESET(" + uuid + ")" + presetExpr;
    }

    public static String makeUuid() {
        Random random = new Random();
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return HexUtils.hexDump(bytes);
    }
}