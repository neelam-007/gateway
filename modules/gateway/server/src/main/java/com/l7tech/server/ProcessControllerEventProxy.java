/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import org.springframework.context.ApplicationListener;

/**
 * Listens for SSG events and forwards interesting ones to the PC via the
 * {@link com.l7tech.server.management.api.node.ProcessControllerApi}.
 *
 * @author alex
 */
public interface ProcessControllerEventProxy extends ApplicationListener {
}
