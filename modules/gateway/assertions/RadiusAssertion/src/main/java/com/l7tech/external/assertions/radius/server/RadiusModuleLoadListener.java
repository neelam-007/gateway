package com.l7tech.external.assertions.radius.server;

import com.l7tech.server.policy.variable.ExpandVariables;
import net.jradius.packet.attribute.AttributeFactory;
import org.springframework.context.ApplicationContext;

public class RadiusModuleLoadListener {

    private static final RadiusSelector radiusSelector = new RadiusSelector();

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");
        //ExpandVariables.registerSelector(radiusSelector);

    }

    public static synchronized void onModuleUnloaded(final ApplicationContext context) {
        //ExpandVariables.unRegisterSelector(radiusSelector);
    }


}
