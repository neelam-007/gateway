package com.l7tech.security.prov.luna;

import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.safenetinc.luna.LunaSlotManager;
import com.safenetinc.luna.provider.LunaProvider;

import java.security.*;
import java.util.concurrent.Callable;

/**
 * Version of Luna provider driver that works with Luna version 5.0.0 (and possibly other post-5.0.0 versions).
 */
public class Luna5JceProviderEngine extends JceProvider {
    private static final Provider JCE_PROVIDER = new LunaProvider();
    private final Provider PBE_PROVIDER;

    public Luna5JceProviderEngine() {
        logIntoPartition();
        Security.insertProviderAt(JCE_PROVIDER, 1);
        PBE_PROVIDER = Security.getProvider("SunJCE"); // Can't use Luna providers for this; they advertise an impl but it is not compatible
    }

    private static void logIntoPartition()  {
        char[] pin = lookupClientPassword();
        int slotNum = SyspropUtil.getInteger("com.l7tech.lunaSlotNum", -1);
        if (slotNum >= 1) {
            if (LunaSlotManager.getInstance().isLoggedIn())
                LunaSlotManager.getInstance().logout(slotNum);
            LunaSlotManager.getInstance().login(slotNum, new String(pin));
        } else {
            LunaSlotManager.getInstance().login(new String(pin));
        }
        LunaSlotManager.getInstance().setSecretKeysExtractable(true);
    }

    /** @return the client password, ie "///6-6KWT-SCMH-N3FE". */
    private static char[] lookupClientPassword() {
        char[] pin;
        String pinFinderClassName = SyspropUtil.getString("com.l7tech.lunaPinFinder", DefaultLunaPinFinder.class.getName());
        try {
            Callable pinFinder = (Callable) Class.forName(pinFinderClassName).newInstance();
            pin = (char[]) pinFinder.call();
        } catch (Exception e) {
            throw new RuntimeException("Unable to look up client password for Luna security provider (do you need to set either the com.l7tech.lunaPin or com.l7tech.lunaPinFinder system properties?): " + ExceptionUtils.getMessage(e), e);
        }
        return pin;
    }

    @Override
    public boolean isFips140ModeEnabled() {
        // TODO this method exists but its return value is not documented: LunaTokenManager.getInstance().GetCurrentFIPSSetting();
        return true;
    }

    @Override
    public Provider getBlockCipherProvider() {
        return JCE_PROVIDER;
    }

    @Override
    public String getDisplayName() {
        return JCE_PROVIDER.toString();
    }

    @Override
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        throw new UnsupportedOperationException("LunaJceProviderEngine is unable to create new Certificate Signing Request using Luna KeyPair: Unsupported operation");
    }

    @Override
    public Provider getProviderFor(String service) {
        // Must override PBE since the Luna impl is not compatible with the Sun impl and so can't decrypt the cluster shared key
        if (JceProvider.SERVICE_PBE_WITH_SHA1_AND_DESEDE.equalsIgnoreCase(service))
            return PBE_PROVIDER;
        return null;
    }
}
