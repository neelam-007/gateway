// $ANTLR 2.7.4: "xparser.g" -> "XprParser.java"$

package com.l7tech.common.xml.tarari.util;

import antlr.*;
import antlr.collections.impl.BitSet;

import java.io.PrintWriter;
import java.util.HashMap;

public class XprParser extends antlr.LLkParser       implements XprParserTokenTypes
 {

    PrintWriter wp ;
    HashMap prefixTab ;
    StringBuffer sbuf ;
    String nsName ;
    String localName ;

    public void myReportError( String msg ) throws TokenStreamException {
        throw new TokenStreamException(msg);
    }

    public void setOutFile( PrintWriter w ) {
        wp = w ;
    }

protected XprParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public XprParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected XprParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public XprParser(TokenStream lexer) {
  this(lexer,1);
}

public XprParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final void mainModule() throws RecognitionException, TokenStreamException {
		
		
		prefixTab = new HashMap() ;
		
		
		try {      // for error handling
			prolog();
			pathList();
			
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void prolog() throws RecognitionException, TokenStreamException {
		
		
		
		
		try {      // for error handling
			{
			_loop4:
			do {
				if ((LA(1)==LITERAL_declare)) {
					match(LITERAL_declare);
					namespaceDecl();
					separator();
				}
				else {
					break _loop4;
				}
				
			} while (true);
			}
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void pathList() throws RecognitionException, TokenStreamException {
		
		
		
		
		try {      // for error handling
			{
			_loop11:
			do {
				if ((LA(1)==SLASH)) {
					pathExpr();
				}
				else {
					break _loop11;
				}
				
			} while (true);
			}
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void namespaceDecl() throws RecognitionException, TokenStreamException {
		
		
		String c1, c2 ;
		
		
		try {      // for error handling
			match(LITERAL_namespace);
			c1=ncName();
			match(EQ);
			c2=stringLiteral();
			
			if ( prefixTab.containsKey( c1 ) ) {
					throw new TokenStreamException("prefix redeclared. Fatal error") ;
			}
			prefixTab.put( c1, c2 ) ;
				 //System.out.println("Declared namespace " + c1 + "=" + c2 ) ; 
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void separator() throws RecognitionException, TokenStreamException {
		
		
		
		
		try {      // for error handling
			match(SEMI);
			{
			_loop7:
			do {
				if ((LA(1)==NL)) {
					match(NL);
				}
				else {
					break _loop7;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_0);
		}
	}
	
	public final String  ncName() throws RecognitionException, TokenStreamException {
		String n;
		
		Token  z = null;
		
		n = null ;
		
		
		try {      // for error handling
			z = LT(1);
			match(ID);
			
			n = z.getText() ;
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return n;
	}
	
	public final String  stringLiteral() throws RecognitionException, TokenStreamException {
		String n;
		
		Token  z = null;
		
		n = null ;
		
		
		try {      // for error handling
			z = LT(1);
			match(STRING);
			
					n = z.getText() ;
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return n;
	}
	
	public final void pathExpr() throws RecognitionException, TokenStreamException {
		
		
		sbuf = new StringBuffer() ;
		
		
		try {      // for error handling
			pathsteps();
			match(NL);
			
			wp.println( sbuf.toString() ) ;
			sbuf = new StringBuffer() ;
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void pathsteps() throws RecognitionException, TokenStreamException {
		
		
		
		
		try {      // for error handling
			{
			int _cnt15=0;
			_loop15:
			do {
				if ((LA(1)==SLASH)) {
					match(SLASH);
					stepExpr();
				}
				else {
					if ( _cnt15>=1 ) { break _loop15; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt15++;
			} while (true);
			}
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void stepExpr() throws RecognitionException, TokenStreamException {
		
		
		sbuf.append("/") ;
		
		
		try {      // for error handling
			axisStep();
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void axisStep() throws RecognitionException, TokenStreamException {
		
		
		String c1 ;
		int nsflag = 0 ;
		String existingPredicate = null ;
		
		
		try {      // for error handling
			forwardStep();
			{
			switch ( LA(1)) {
			case PRED:
			case IPRED:
			{
				existingPredicate=predicate();
				break;
			}
			case NL:
			case SLASH:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
			if (( localName != null) || ( nsName != null ) || (existingPredicate != null)) {
			sbuf.append("[") ;
					if ( localName != null) {
					    sbuf.append( "local-name() = \"" ) ;
					    sbuf.append( localName );
					    sbuf.append( "\" " );
					    if ( nsName != null )
					        sbuf.append(" and ");
					    localName = null;
					}
			if ( nsName != null ) {
			String uristr = (String) prefixTab.get( nsName ) ;
			if ( uristr == null ) {
			throw new TokenStreamException("Undefined prefix " + nsName +". fatal error") ;
			}
			sbuf.append( "namespace-uri() =\"" ) ;
			sbuf.append( uristr ) ;
			sbuf.append("\" " ) ;
			nsflag = 1 ;
			nsName = null ;
			}
			if ( existingPredicate != null ) {
			
			if ( nsflag == 1 ) {
			sbuf.append(" and (" ) ;
			}
			sbuf.append( existingPredicate ) ;
			if( nsflag == 1 ) {
			sbuf.append(")" ) ;
			}
			}
			sbuf.append("]") ;
			}
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void forwardStep() throws RecognitionException, TokenStreamException {
		
		
		
		
		try {      // for error handling
			abbrevForwardStep();
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final String  predicate() throws RecognitionException, TokenStreamException {
		String n;
		
		Token  z = null;
		Token  y = null;
		
		n = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case PRED:
			{
				z = LT(1);
				match(PRED);
				
				n = z.getText() ;
				
				break;
			}
			case IPRED:
			{
				y = LT(1);
				match(IPRED);
				
				n = "position() = "+y.getText() ;
				
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return n;
	}
	
	public final void abbrevForwardStep() throws RecognitionException, TokenStreamException {
		
		
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case AT:
			{
				match(AT);
				sbuf.append("@") ;
				break;
			}
			case SLASH:
			{
				match(SLASH);
				sbuf.append("/") ;
				break;
			}
			case LITERAL_text:
			case STAR:
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			nodeTest();
			
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final void nodeTest() throws RecognitionException, TokenStreamException {
		
		
		String n = null  ;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case STAR:
			case ID:
			{
				n=nameTest();
				break;
			}
			case LITERAL_text:
			{
				n=kindTest();
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
			sbuf.append( n ) ;
			
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final String  nameTest() throws RecognitionException, TokenStreamException {
		String n;
		
		Token  z = null;
		Token  y = null;
		Token  x = null;
		
		int nsflag = 0 ;
		String c1 = null ;
		String c2 = null ;
		n = null ;
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case STAR:
			{
				z = LT(1);
				match(STAR);
				{
				switch ( LA(1)) {
				case COL:
				{
					match(COL);
					nsflag = 1 ;
					{
					switch ( LA(1)) {
					case ID:
					{
						c1=ncName();
						break;
					}
					case STAR:
					{
						y = LT(1);
						match(STAR);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					break;
				}
				case NL:
				case SLASH:
				case PRED:
				case IPRED:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				
						if ( nsflag == 1 ) {
							localName = c1;
							if ( c1 != null ) {
								n = "*" ;
							} else {
								n = "*" ;
							}
						} else {
						    n  = "*" ;
						}
					
				break;
			}
			case ID:
			{
				c1=ncName();
				{
				switch ( LA(1)) {
				case COL:
				{
					match(COL);
					nsflag = 1 ;
					{
					switch ( LA(1)) {
					case ID:
					{
						c2=ncName();
						break;
					}
					case STAR:
					{
						x = LT(1);
						match(STAR);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					break;
				}
				case NL:
				case SLASH:
				case PRED:
				case IPRED:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				
						if ( nsflag == 1 ) {
							nsName = c1 ;
							if ( c2 != null ) {
								localName = c2;
								n = "*" ;
							} else {
								n = "*" ;
							}
						} else {
							n = c1  ;
						}
					
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return n;
	}
	
	public final String  kindTest() throws RecognitionException, TokenStreamException {
		String n;
		
		
		n = null ;
		
		
		try {      // for error handling
			match(LITERAL_text);
			match(LPAREN);
			match(RPAREN);
			
			n = "text()" ;
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return n;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"declare\"",
		"SEMI",
		"NL",
		"\"namespace\"",
		"EQ",
		"SLASH",
		"AT",
		"\"text\"",
		"LPAREN",
		"RPAREN",
		"STAR",
		"COL",
		"PRED",
		"IPRED",
		"STRING",
		"ID"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 530L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	
	}
