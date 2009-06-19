package com.l7tech.skunkworks.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.cryptox.LunaJCEProvider;
import org.junit.*;

/**
 * Lists services available from each of the two Luna security providers.
 */
public class LunaShowServices {
    @Test
    public void showJceServices() {
        TestLunaCommon.showServices(new LunaJCEProvider());
    }

    @Test
    public void showJcaService() {
        TestLunaCommon.showServices(new LunaJCAProvider());
    }
}
