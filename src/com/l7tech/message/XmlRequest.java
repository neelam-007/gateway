/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tag interface defining an object that is both an XmlMessage and a Request.
 * @author alex
 * @version $Revision$
 */
public interface XmlRequest extends Request, XmlMessage {

}
