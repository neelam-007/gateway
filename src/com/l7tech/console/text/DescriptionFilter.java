
/*
 * $Header$
 */
package com.l7tech.console.text;


/**
 * <CODE>DescriptionFilter</CODE> is the implementation of
 * <CODE>Filter</CODE> accepting Strings that only contain
 * Letters of Digits, soon to include only lower ASCII values
 *
 * @author <a href="mailto: richard@l7tech.com">Richard Stride</a>
 */ 
public class DescriptionFilter implements FilterDocument.Filter {

  public DescriptionFilter() {}

  public boolean accept(String s) {
    int x = 0;
    while( x < s.length() ) {
      /*
       * The windows IPSEC client does not support Unicode, nor does it support ISO-8859-1
       * We must limit certain user input to ASCII, especially if that imput will be displayed 
       * in the windows IPSEC client
       *
       * Ideally we would use the following line to enable Western European character support 
       * if ( Character.UnicodeBlock.BASIC_LATIN != Character.UnicodeBlock.of(s.charAt(x)) ) 
       */
      if ( 127 < (int)s.charAt(x) ) 
        return false;
      x++;
    }
    return true;
  }

}
