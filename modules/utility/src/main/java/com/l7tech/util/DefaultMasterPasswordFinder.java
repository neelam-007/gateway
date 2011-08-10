package com.l7tech.util;

import java.io.File;
import java.lang.reflect.Constructor;

/**
 * A MasterPasswordFinder that checks a system property to find an implementation to delegate to.
 * <p/>
 * Lookup of the delegate is uncached -- the delegate will be looked up, instantiated, and queried
 * every time someone calls {@link #findMasterPasswordBytes()}.
 */
public class DefaultMasterPasswordFinder implements MasterPasswordManager.MasterPasswordFinder {
    public static final String PROP_FINDER = "com.l7tech.util.masterPasswordFinder";
    public static final String PROP_FINDER_DEFAULT = ObfuscatedFileMasterPasswordFinder.class.getName();
    public static final String PROP_FINDER_KMP_DEFAULT = KeyStorePrivateKeyMasterPasswordFinder.class.getName();

    private final File ompFile;

    /**
     * Create a proxy MasterPasswordFinder that will look up an appropriate implementation dynamically.
     * <p/>
     * If the implementation has a constructor-from-File, it will be passed the specified File.
     *
     * @param ompFile the file containing nothing but the obfuscated master password (omp.dat)
     */
    public DefaultMasterPasswordFinder(File ompFile) {
        this.ompFile = ompFile;
    }

    @Override
    public byte[] findMasterPasswordBytes() {
        File kmpPropsFile = KeyStorePrivateKeyMasterPasswordFinder.findPropertiesFile(ompFile);
        String finderClassname = ConfigFactory.getProperty( PROP_FINDER, kmpPropsFile.exists() ? PROP_FINDER_KMP_DEFAULT : PROP_FINDER_DEFAULT );

        try {
            Constructor<MasterPasswordManager.MasterPasswordFinder> ctor =
                    ConstructorInvocation.findMatchingConstructor(getClass().getClassLoader(), finderClassname, MasterPasswordManager.MasterPasswordFinder.class, new Class[]{File.class});
            return ctor.newInstance(ompFile).findMasterPasswordBytes();
        } catch (ConstructorInvocation.NoMatchingPublicConstructorException e) {
            // FALLTHROUGH and check for no-arg constructor
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate master password finder with File arg: " + ExceptionUtils.getMessage(e), e);
        }

        try {
            Constructor<MasterPasswordManager.MasterPasswordFinder> ctor =
                    ConstructorInvocation.findMatchingConstructor(getClass().getClassLoader(), finderClassname, MasterPasswordManager.MasterPasswordFinder.class, new Class[0]);
            return ctor.newInstance().findMasterPasswordBytes();
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate master password finder: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
