/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.security.prov.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.crypto.LunaTokenManager;
import com.chrysalisits.cryptox.LunaJCEProvider;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import java.security.*;
import java.util.concurrent.Callable;

/**
 * A set of JCE settings, translations, and installation behavior for the SafeNet HSM (formerly SafeNet Luna) network attached HSM.
 */
public class LunaJceProviderEngine extends JceProvider {
    private final Provider JCE_PROVIDER = new LunaJCEProvider();
    private final Provider JCA_PROVIDER = new LunaJCAProvider();
    private final Provider PBE_PROVIDER;
    
    public LunaJceProviderEngine() {
        Security.insertProviderAt(JCA_PROVIDER, 1);
        Security.insertProviderAt(JCE_PROVIDER, 1);
        PBE_PROVIDER = Security.getProvider("SunJCE"); // Can't use Luna providers for this; they advertise an impl but it is not compatible

        logIntoPartition();
    }

    private static void logIntoPartition()  {
        char[] pin = lookupClientPassword();
        int slotNum = SyspropUtil.getInteger("com.l7tech.lunaSlotNum", -1);
        if (slotNum >= 1) {
            LunaTokenManager.getInstance().Login(slotNum, new String(pin));
        } else {
            LunaTokenManager.getInstance().Login(new String(pin));
        }
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
    public String getDisplayName() {
        return JCE_PROVIDER.toString() + " / " + JCA_PROVIDER.toString();
    }

    @Override
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        throw new UnsupportedOperationException("LunaJceProviderEngine is unable to create new Certificate Signing Request using Luna KeyPair: Unsupported operation");
    }

    @Override
    public Provider getProviderFor(String service) {
        // Must override PBE since the Luna impl is not compatible with the Sun impl and so can't decrypt the cluster shared key
        if (JceProvider.SERVICE_PBE_WITH_SHA1_AND_DESEDE.equals(service))
            return PBE_PROVIDER;
        return null;
    }
}
