/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.io.OutputStream;

/**
 * @author alex
 */
public interface Response extends Message {
    OutputStream getResponseStream();
}
