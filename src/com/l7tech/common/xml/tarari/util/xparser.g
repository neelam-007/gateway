
header {
package com.l7tech.common.xml.tarari.util;
}

options {
   language="Java";
}

{
   //  stuff in the parser class file outside the class
   // ...

    import java.io.PrintWriter ;
    import java.util.HashMap ;
}


class XprParser extends Parser;
options {
   buildAST = false ;      // don't build default AST
}
{
    PrintWriter wp ;
    HashMap prefixTab ;
    int inPred = 0;

    private static class PrefixedName {
        private String nsPrefix = null;
        private String localName = null;
	    private String pass = null; // passthrough unchanged
    }

    public void myReportError( String msg ) throws TokenStreamException {
        throw new TokenStreamException(msg);
    }

    public void setOutFile( PrintWriter w ) {
        wp = w ;
    }
}

// ... grammar rules

mainModule	
{
    prefixTab = new HashMap() ;
}
	:	prolog pathList
	{
	}
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

prolog    	
{
}
	:	("declare" namespaceDecl 
		separator)* 
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

separator	 //returns [PTNode *n]
{
}
	:	SEMI (NL)*
	;

namespaceDecl  
{
    String c1, c2 ;
}
	:	 "namespace" c1=ncName EQ c2=stringLiteral
	{
         if ( prefixTab.containsKey( c1 ) ) {
		throw new TokenStreamException("prefix redeclared. Fatal error") ;
         }
         prefixTab.put( c1, c2 ) ;
	 //System.out.println("Declared namespace " + c1 + "=" + c2 ) ; 
	}
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

pathList	
{
}
	:	(  pathExpr )*
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}


pathExpr	
{
    String s = "";
}
	:  s=pathsteps NL
	{
       wp.println( s ) ;
	}
	; 
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

pathsteps	returns [String s]
{
    s = "";
    String ss = "";
}
	:
	(  SLASH { s += "/"; } ss=stepExpr { s += ss; }
	)+
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}


stepExpr	 returns [String s]
{
    s = "";
    String ss = "";
}
	:	ss=axisStep { s += ss; }
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

axisStep	 returns [String s]
{
    String c1 ;
    int nsflag = 0 ;
    String existingPredicate = null ;
    PrefixedName pn = null;
    s = "";
}
	:	pn=forwardStep ( existingPredicate=predicate 
    )?
	{
	    StringBuffer sbuf = new StringBuffer();

	    if (pn != null && pn.pass != null) {
	        sbuf.append(pn.pass);
	        pn.pass = null;
	    }

	    String nsUri = null;
        if ( pn.nsPrefix != null ) {
            nsUri = (String) prefixTab.get( pn.nsPrefix ) ;

            if (nsUri == null) {
                sbuf.append(pn.nsPrefix);
                sbuf.append(":");
                sbuf.append(pn.localName == null ? "*" : pn.localName);
                if (existingPredicate != null) {
                    sbuf.append("[");
                    sbuf.append(existingPredicate);
                    sbuf.append("]");
                    existingPredicate = null;
                }
                pn = null;
            }
        }

	    if (( pn != null && pn.localName != null) || ( pn != null && pn.nsPrefix != null ))
                sbuf.append("*") ;
	    if (( pn != null && pn.localName != null) || ( pn != null && pn.nsPrefix != null ) || (existingPredicate != null)) {
                sbuf.append("[") ;
		    if ( pn.localName != null) {
    		    if (inPred > 0) throw new RecognitionException("Unable to express nested predicate");
    		    sbuf.append( "local-name() = \"" ) ;
    		    sbuf.append( pn.localName );
    		    sbuf.append( "\" " );
    		    if ( pn.nsPrefix != null )
    		        sbuf.append(" and ");
                nsflag = 1 ;
    		}
            if ( nsUri != null ) {
    		    if (inPred > 0) throw new RecognitionException("Unable to express nested predicate");
                        sbuf.append( "namespace-uri() =\"" ) ;
                        sbuf.append( nsUri ) ;
                        sbuf.append("\" " ) ;
                        nsflag = 1 ;
                }
                if ( existingPredicate != null ) {
                   if ( nsflag == 1 )
                       sbuf.append(" and (" ) ;
                   sbuf.append( existingPredicate ) ;
                   if( nsflag == 1 )
                       sbuf.append(")" ) ;
                }
                sbuf.append("]") ;
            }
	    s += sbuf.toString();
	}

	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

forwardStep	returns [PrefixedName ret]
{
	PrefixedName n = null;
	ret = null;
}
	: n=abbrevForwardStep
	{
	    ret = n;
	}
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

abbrevForwardStep	returns [PrefixedName ret]
{
	PrefixedName n = null;
	ret = new PrefixedName();
	ret.pass = "";
}
	:	( AT { ret.pass += "@";  } | SLASH { ret.pass += "/"; } )? n=nodeTest
	{
	    if (n != null) {
	        ret.localName = n.localName;
		ret.nsPrefix = n.nsPrefix;
		if (n.pass != null)
		    ret.pass += n.pass;
	    }
	}
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

nodeTest	returns [PrefixedName ret]
{
	PrefixedName n = null;
	ret = null;
}
	:	( n = nameTest
	| n = kindTest
        )
     {
	    ret = n;
     }
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

kindTest	 returns [PrefixedName ret]
{
    ret = new PrefixedName();
}
	:	"text" LPAREN RPAREN
	{
	    ret.localName = "text()" ;
	}
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

nameTest	 returns [PrefixedName ret]
{
    int nsflag = 0 ;
    String c1 = null ;
    String c2 = null ;
    ret = new PrefixedName();
}
	:	z:STAR (COL { nsflag = 1 ; } (c1=ncName| y:STAR))?
	{
		if ( nsflag == 1 ) {
			ret.localName = c1;
		} else {
		   ret.pass = "*";
		}
	}
	|  c1=ncName (COL { nsflag = 1 ; } (c2=ncName | x:STAR))?
	{
        if ( nsflag == 1 ) {
		    ret.nsPrefix = c1 ;
			if ( c2 != null ) {
				ret.localName = c2;
			}
		} else {
			ret.pass = c1;
		}
	} 
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

predicate	 returns [String n]
{
  n = null ;
  String ss = null;
}
	:  SQLEFT { inPred++; if (inPred > 1) throw new RecognitionException("Nested predicates not supported"); } (ss=simplepredicate
        {   
            n = ss ;
        }
        |   y:INTEGER
        {
           n = "position() = "+y.getText() ;
        }
	|   ss=stepExpr
	{
	   n = ss;
	}
	) SQRIGHT { inPred--; }
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

simplepredicate     returns [String n]
{
    n = "" ;
    String ss = null ;
}
    : ( ss=simplepredicatenoint {n+=ss;} )+
      ( ss=simplepredicateint {n+=ss;} )*
    ;

simplepredicateint  returns [String n]
{
    n = "" ;
}
    : ( a:ID {n+=a.getText();}
      | b:DASH {n+=b.getText();}
      | c:COL {n+=c.getText();}
      | d:EQ {n+=d.getText();}
      | e:STRING {n+='"' + e.getText() + '"';}
      | f:PARENLEFT {n+=f.getText();}
      | g:PARENRIGHT {n+=g.getText();}
      | h:INTEGER {n+=h.getText();}
      )+
    ;

simplepredicatenoint    returns [String n]
{
    n = "";
}
    : ( a:ID {n+=a.getText();}
      | b:DASH {n+=b.getText();}
      | c:COL {n+=c.getText();}
      | d:EQ {n+=d.getText();}
      | e:STRING {n+='"' + e.getText() + '"';}
      | f:PARENLEFT {n+=f.getText();}
      | g:PARENRIGHT {n+=g.getText();}
      )+
    ;

stringLiteral	 returns [String n]
{
  n = null ;
}
	:  z:STRING
	{
		n = z.getText() ;
	} 
	;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}



ncName	 returns [String n]
{
   n = null ;
}
	:  z:ID
	{
            n = z.getText() ;
	}
;
	exception
	catch [ RecognitionException ex ] {
		myReportError( ex.toString() ) ;
	}

{
   // global stuff in the lexer file
}


class XprLexer extends  Lexer ;
options {
    importVocab=XprParser ;
    charVocabulary='\0' .. '\377' ;
    k = 3 ;
}

{
  // additional methods in lexer class

}

// lexer token defs

WS     :       
	  (	' '
        |       '\t'
      )+
      {
        _ttype = Token.SKIP ;
      } 
    ;

NL     :
         (  '\n' { newline() ; }
        |       '\r' '\n' { newline() ;} 
          )+
        ;
        

SQLEFT  :  '['
        ;

SQRIGHT :  ']'
        ;

PARENLEFT : '('
          ;

PARENRIGHT : ')'
           ;

SEMI    :   ';'
        ;

SLASH   : '/'
        ;

AT     : '@'
	;

EQ  : '='
    ;

DASH : '-'
     ;

ASSIGN  : ':' '='
	;

COL   : ':'
      ;

STRING : '"'!  (ESC | ~('\\' | '"')  )* '"'!
	;

protected
ESC    : '\\' ( '"' )? 
	;

protected
DIGIT
        :       '0'..'9'
        ;

INTEGER     :       (DIGIT)+
        ;

ID
options {
	testLiterals = true ;
}
: IDSTART ( IDV )?
        ;


STAR    : '*'
	;

protected 
IDSTART  : (  'a'..'z' | 'A' .. 'Z' )
         ;

protected
IDV      : ( 'a'..'z' | 'A'..'Z' | '_' | '0' .. '9' )+
         ;

protected
CHAR    : '\0' .. '\377'
	;
