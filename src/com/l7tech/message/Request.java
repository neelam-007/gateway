/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.io.InputStream;

/**
 * @author alex
 */
public interface Request extends Message {
    InputStream getRequestStream();
}
