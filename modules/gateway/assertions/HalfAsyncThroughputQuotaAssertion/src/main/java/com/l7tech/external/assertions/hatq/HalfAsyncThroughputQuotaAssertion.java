package com.l7tech.external.assertions.hatq;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.sla.CounterPresetInfo;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.util.TextUtils;

import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
public class HalfAsyncThroughputQuotaAssertion extends ThroughputQuota {
    protected static final Logger logger = Logger.getLogger(HalfAsyncThroughputQuotaAssertion.class.getName());

    //
    // Metadata
    //
    private static final String META_INITIALIZED = HalfAsyncThroughputQuotaAssertion.class.getName() + ".metadataInitialized";

    private final static String baseName = "(Half-Async) Apply Throughput Quota";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ThroughputQuota>(){
        @Override
        public String getAssertionName( final ThroughputQuota assertion, final boolean decorate) {
            if(!decorate) return baseName;

            final StringBuilder buffer = new StringBuilder( baseName );
            final String readableCounterName = getReadableCounterName(assertion.getCounterName());
            if (assertion.getCounterStrategy() == ThroughputQuota.DECREMENT) {
                buffer.append(": Decrement counter ").append(readableCounterName);
            } else {
                buffer.append(": ").append(readableCounterName).append(": ").append(assertion.getQuota()).append(" per ").append(timeUnitStr(assertion.getTimeUnit()));
            }
            return buffer.toString();
        }
    };

    /**
     * Generate a user-experienced readable counter name to be displayed in UI.
     * @param rawCounterName: the raw counter name with a format PRESET(<uuid>)-${<context variable>}, if the raw counter name is well-formatted.
     * @return a readable counter name in a format, "<8-digit of uuid>-${<context variable>}.
     */
    static String getReadableCounterName(final String rawCounterName) {
        final String[] uuidOut = new String[]{null};
        final String quotaOption = CounterPresetInfo.findCounterNameKey(rawCounterName, uuidOut, PRESET_CUSTOM, COUNTER_NAME_TYPES);

        String readableCounterName = quotaOption == null?
            rawCounterName : CounterPresetInfo.makeDefaultCustomExpr(uuidOut[0], COUNTER_NAME_TYPES.get(quotaOption));

        if (readableCounterName == null) readableCounterName = rawCounterName;
        if (readableCounterName.length() > 128) readableCounterName = TextUtils.truncateStringAtEnd(readableCounterName, 128);

        return readableCounterName;
    }

    private static String timeUnitStr(int timeUnit) {
        switch (timeUnit) {
            case ThroughputQuota.PER_SECOND: return "second";
            case ThroughputQuota.PER_MINUTE: return "minute";
            case ThroughputQuota.PER_HOUR: return "hour";
            case ThroughputQuota.PER_DAY: return "day";
            case ThroughputQuota.PER_MONTH: return "month";
            default: return "something";
        }
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(PALETTE_FOLDERS, new String[]{"misc"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Limit the number of service requests permitted within a predetermined time period.  There may be a delay before overages are detected.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/policy16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.ThroughputQuotaPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "(Half-Async) Throughput Quota Properties");

        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.hatq.server.ServerHalfAsyncThroughputQuotaAssertion");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
