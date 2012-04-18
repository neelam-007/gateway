package com.l7tech.ntlm.util;

 import java.io.IOException;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class EncodingDecodingException extends IOException
{
  public EncodingDecodingException(String message)
  {
    super(message);
  }

  public EncodingDecodingException(String message, Throwable cause) {
    super(message, cause);
  }
}
