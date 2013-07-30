package com.l7tech.server.ems;

import com.l7tech.gateway.common.licensing.FeatureSetExpander;

import java.util.*;
import java.util.logging.Logger;

/**
 * Master list of Feature Sets for the Enterprise Service Manager, hard-baked into the code so it will be obfuscated.
 *
 * @noinspection JavaDoc,StaticMethodNamingConvention,OverloadedMethodsWithSameNumberOfParameters,OverloadedVarargsMethod,OverlyCoupledClass
 */
public class EsmFeatureSets {
    private static final Logger logger = Logger.getLogger(EsmFeatureSets.class.getName());

    /** All pre-configured FeatureSets. */
    private static final Map<String, EsmFeatureSet> sets = new LinkedHashMap<String, EsmFeatureSet>();

    /** Only the root-level FeatureSets. */
    private static final Map<String, EsmFeatureSet> rootSets = new LinkedHashMap<String, EsmFeatureSet>();

    /** Only the root-level Product Profile feature sets. */
    private static final Map<String, EsmFeatureSet> profileSets = new LinkedHashMap<String, EsmFeatureSet>();

    /** The ultimate Product Profile that enables every possible feature. */
    public static final EsmFeatureSet PROFILE_ALL;

    // Constants for service names
    public static final String SERVICE_ADMIN = "service:Admin";

    public static final String FEATURE_BACKUPRESTORE = "feature:BackupRestore";
    public static final String FEATURE_POLICY_MIGRATION = "feature:PolicyMigration";
    public static final String FEATURE_REPORTING = "feature:Reporting";

    static {
        // Declare all baked-in feature sets

        //
        // Declare "twig" feature sets
        // (feature sets that don't include other feature sets, totally useless on their own, but not
        //  a "leaf" feature set like a single assertion or servlet)
        // Naming convention: set:all:lowercase
        //
        EsmFeatureSet admin =
        fsr("set:admin", "All admin APIs, over all admin API transports",
            "Everything that used to be enabled by the catchall Feature.ADMIN",
            misc(SERVICE_ADMIN, "All admin APIs, over all admin API transports", null));

        //
        // Declare "product profile" feature sets
        // (feature sets built out of "building block" feature sets, and which each constitutes a useable,
        //  complete product in its own right.)
        // Naming convention:   set:Profile:ProfileName
        //
        PROFILE_ALL =
        fsp("set:Profile:EnterpriseServiceManager", "Enterprise Service Manager",
            "All features enabled.",
            fs(admin),
            feat(FEATURE_BACKUPRESTORE, "Gateway Backup and Restore."),
            feat(FEATURE_POLICY_MIGRATION, "Policy Migration."),
            feat(FEATURE_REPORTING, "Reporting."));
    }

    /** @return All registered FeatureSets, including product profiles, building blocks, and twig and leaf features. */
    public static Map<String, EsmFeatureSet> getAllFeatureSets() {
        return Collections.unmodifiableMap(sets);
    }


    /** @return all root-level FeatureSets, including all product profiles, building blocks, and twig features. */
    public static Map<String, EsmFeatureSet> getRootFeatureSets() {
        return Collections.unmodifiableMap(rootSets);
    }


    /** @return all Product Profile FeatureSets. */
    public static Map<String, EsmFeatureSet> getProductProfiles() {
        return Collections.unmodifiableMap(profileSets);
    }


    /** @return the product profile that has all features enabled. */
    public static EsmFeatureSet getBestProductProfile() {
        return PROFILE_ALL;
    }

    /** @return the FeatureSetExpander to use when parsing License files. */
    public static FeatureSetExpander getFeatureSetExpander() {
        return new FeatureSetExpander() {
            @Override
            public Set<String> getAllEnabledFeatures(Set<String> inputSet) {
                Set<String> ret = new HashSet<String>(inputSet);

                for (String topName : inputSet) {
                    EsmFeatureSet fs = sets.get(topName);
                    if (fs == null) {
                        logger.fine("Ignoring unrecognized feature set name: " + topName);
                        continue;
                    }
                    fs.collectAllFeatureNames(ret);
                }

                return ret;
            }
        };
    }

    /** Find already-registered EsmFeatureSet by EsmFeatureSet.  (Basically just asserts that a featureset is registered already.) */
    private static EsmFeatureSet fs(EsmFeatureSet fs)  throws IllegalArgumentException {
        return fs(fs.name);
    }

    /** Find already-registered EsmFeatureSet by name. */
    private static EsmFeatureSet fs(String name) throws IllegalArgumentException {
        EsmFeatureSet got = sets.get(name);
        if (got == null || name == null) throw new IllegalArgumentException("Unknown feature set name: " + name);
        return got;
    }

    /** Create and register a new non-leaf EsmFeatureSet. */
    private static EsmFeatureSet fsr(String name, String desc, String note, EsmFeatureSet... deps) throws IllegalArgumentException {
        if (!name.startsWith("set:")) throw new IllegalArgumentException("Non-leaf feature set name must start with \"set:\": " + name);
        EsmFeatureSet newset = new EsmFeatureSet(name, desc, note, deps);
        EsmFeatureSet old = sets.put(name, newset);
        if (old != null) throw new IllegalArgumentException("Duplicate feature set name: " + name);
        rootSets.put(name, newset);
        return newset;
    }

    /** Create and register a new root-level "product profile" EsmFeatureSet. */
    private static EsmFeatureSet fsp(String name, String desc, String note, EsmFeatureSet... deps) throws IllegalArgumentException {
        EsmFeatureSet got = fsr(name, desc, note, deps);
        profileSets.put(name, got);
        return got;
    }

    private static EsmFeatureSet getOrMakeFeatureSet(String name, String desc) {
        EsmFeatureSet got = sets.get(name);
        if (got != null) {
            if (!desc.equals(got.desc)) throw new IllegalArgumentException("Already have different feature set named: " + name);
            return got;
        }

        got = new EsmFeatureSet(name, desc);
        sets.put(name, got);
        return got;
    }

    /** Create (and register, if new) a new miscellaneous EsmFeatureSet with the specified name and description. */
    private static EsmFeatureSet misc(String name, String desc, String note) {
        EsmFeatureSet got = sets.get(name);
        if (got != null) {
            if (!desc.equals(got.desc))
                throw new IllegalArgumentException("Feature set name already in use with different description: " + name);
            return got;
        }

        got = new EsmFeatureSet(name, desc, note, null);
        sets.put(name, got);
        return got;
    }

    private static EsmFeatureSet feat(String name, String desc) {
        if (!name.startsWith("feature:"))
            throw new IllegalArgumentException("Preferred feature name for feature must start with \"feature:\": " + name);
        return getOrMakeFeatureSet(name, desc);
    }
}