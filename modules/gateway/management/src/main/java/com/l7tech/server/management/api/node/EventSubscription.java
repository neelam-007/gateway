/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.node;

import com.l7tech.server.management.config.monitoring.ComponentType;

/** @author alex */
public class EventSubscription {
    String subscriptionId;
    ComponentType componentType;
    String componentId;
    String eventId;
    long expiry;
}
