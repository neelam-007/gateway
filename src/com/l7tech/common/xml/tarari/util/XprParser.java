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
		
		
		String s = "";
		
		
		try {      // for error handling
			s=pathsteps();
			match(NL);
			
			wp.println( s ) ;
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
	}
	
	public final String  pathsteps() throws RecognitionException, TokenStreamException {
		String s;
		
		
		s = "";
		String ss = "";
		
		
		try {      // for error handling
			{
			int _cnt15=0;
			_loop15:
			do {
				if ((LA(1)==SLASH)) {
					match(SLASH);
					s += "/";
					ss=stepExpr();
					s += ss;
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
		return s;
	}
	
	public final String  stepExpr() throws RecognitionException, TokenStreamException {
		String s;
		
		
		s = "";
		String ss = "";
		
		
		try {      // for error handling
			ss=axisStep();
			s += ss;
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return s;
	}
	
	public final String  axisStep() throws RecognitionException, TokenStreamException {
		String s;
		
		
		String c1 ;
		int nsflag = 0 ;
		String existingPredicate = null ;
		PrefixedName pn = null;
		s = "";
		
		
		try {      // for error handling
			pn=forwardStep();
			{
			switch ( LA(1)) {
			case SQLEFT:
			{
				existingPredicate=predicate();
				break;
			}
			case NL:
			case SLASH:
			case SQRIGHT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
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
					    if (inPred > 0) throw new RecognitionException("requires nested predicate");
					    sbuf.append( "local-name() = \"" ) ;
					    sbuf.append( pn.localName );
					    sbuf.append( "\" " );
					    if ( pn.nsPrefix != null )
					        sbuf.append(" and ");
			nsflag = 1 ;
					}
			if ( nsUri != null ) {
					    if (inPred > 0) throw new RecognitionException("requires nested predicate");
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
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return s;
	}
	
	public final PrefixedName  forwardStep() throws RecognitionException, TokenStreamException {
		PrefixedName ret;
		
		
			PrefixedName n = null;
			ret = null;
		
		
		try {      // for error handling
			n=abbrevForwardStep();
			
				    ret = n;
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return ret;
	}
	
	public final String  predicate() throws RecognitionException, TokenStreamException {
		String n;
		
		Token  y = null;
		
		n = null ;
		String ss = null;
		
		
		try {      // for error handling
			match(SQLEFT);
			inPred++; if (inPred > 1) throw new RecognitionException("Nested predicates not supported");
			{
			if ((_tokenSet_1.member(LA(1)))) {
				ss=simplepredicate();
				
				n = ss ;
				
			}
			else if ((LA(1)==INTEGER)) {
				y = LT(1);
				match(INTEGER);
				
				n = "position() = "+y.getText() ;
				
			}
			else if ((_tokenSet_2.member(LA(1)))) {
				ss=stepExpr();
				
					   n = ss;
					
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			match(SQRIGHT);
			inPred--;
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return n;
	}
	
	public final PrefixedName  abbrevForwardStep() throws RecognitionException, TokenStreamException {
		PrefixedName ret;
		
		
			PrefixedName n = null;
			ret = new PrefixedName();
			ret.pass = "";
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case AT:
			{
				match(AT);
				ret.pass += "@";
				break;
			}
			case SLASH:
			{
				match(SLASH);
				ret.pass += "/";
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
			n=nodeTest();
			
				    if (n != null) {
				        ret.localName = n.localName;
					ret.nsPrefix = n.nsPrefix;
					if (n.pass != null)
					    ret.pass += n.pass;
				    }
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return ret;
	}
	
	public final PrefixedName  nodeTest() throws RecognitionException, TokenStreamException {
		PrefixedName ret;
		
		
			PrefixedName n = null;
			ret = null;
		
		
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
			
				    ret = n;
			
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return ret;
	}
	
	public final PrefixedName  nameTest() throws RecognitionException, TokenStreamException {
		PrefixedName ret;
		
		Token  z = null;
		Token  y = null;
		Token  x = null;
		
		int nsflag = 0 ;
		String c1 = null ;
		String c2 = null ;
		ret = new PrefixedName();
		
		
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
				case SQLEFT:
				case SQRIGHT:
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
							ret.localName = c1;
						} else {
						   ret.pass = "*";
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
				case SQLEFT:
				case SQRIGHT:
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
						    ret.nsPrefix = c1 ;
							if ( c2 != null ) {
								ret.localName = c2;
							}
						} else {
							ret.pass = c1;
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
		return ret;
	}
	
	public final PrefixedName  kindTest() throws RecognitionException, TokenStreamException {
		PrefixedName ret;
		
		
		ret = new PrefixedName();
		
		
		try {      // for error handling
			match(LITERAL_text);
			match(LPAREN);
			match(RPAREN);
			
				    ret.localName = "text()" ;
				
		}
		catch ( RecognitionException ex ) {
			
					myReportError( ex.toString() ) ;
				
		}
		return ret;
	}
	
	public final String  simplepredicate() throws RecognitionException, TokenStreamException {
		String n;
		
		
		n = "" ;
		String ss = null ;
		
		
		try {      // for error handling
			{
			int _cnt34=0;
			_loop34:
			do {
				if ((_tokenSet_1.member(LA(1)))) {
					ss=simplepredicatenoint();
					n+=ss;
				}
				else {
					if ( _cnt34>=1 ) { break _loop34; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt34++;
			} while (true);
			}
			{
			_loop36:
			do {
				if ((_tokenSet_3.member(LA(1)))) {
					ss=simplepredicateint();
					n+=ss;
				}
				else {
					break _loop36;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_4);
		}
		return n;
	}
	
	public final String  simplepredicatenoint() throws RecognitionException, TokenStreamException {
		String n;
		
		Token  a = null;
		Token  b = null;
		Token  c = null;
		Token  d = null;
		Token  e = null;
		Token  f = null;
		Token  g = null;
		
		n = "";
		
		
		try {      // for error handling
			{
			int _cnt42=0;
			_loop42:
			do {
				if ((LA(1)==ID)) {
					a = LT(1);
					match(ID);
					n+=a.getText();
				}
				else if ((LA(1)==DASH)) {
					b = LT(1);
					match(DASH);
					n+=b.getText();
				}
				else if ((LA(1)==COL)) {
					c = LT(1);
					match(COL);
					n+=c.getText();
				}
				else if ((LA(1)==EQ)) {
					d = LT(1);
					match(EQ);
					n+=d.getText();
				}
				else if ((LA(1)==STRING)) {
					e = LT(1);
					match(STRING);
					n+='"' + e.getText() + '"';
				}
				else if ((LA(1)==PARENLEFT)) {
					f = LT(1);
					match(PARENLEFT);
					n+=f.getText();
				}
				else if ((LA(1)==PARENRIGHT)) {
					g = LT(1);
					match(PARENRIGHT);
					n+=g.getText();
				}
				else {
					if ( _cnt42>=1 ) { break _loop42; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt42++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
		return n;
	}
	
	public final String  simplepredicateint() throws RecognitionException, TokenStreamException {
		String n;
		
		Token  a = null;
		Token  b = null;
		Token  c = null;
		Token  d = null;
		Token  e = null;
		Token  f = null;
		Token  g = null;
		Token  h = null;
		
		n = "" ;
		
		
		try {      // for error handling
			{
			int _cnt39=0;
			_loop39:
			do {
				if ((LA(1)==ID)) {
					a = LT(1);
					match(ID);
					n+=a.getText();
				}
				else if ((LA(1)==DASH)) {
					b = LT(1);
					match(DASH);
					n+=b.getText();
				}
				else if ((LA(1)==COL)) {
					c = LT(1);
					match(COL);
					n+=c.getText();
				}
				else if ((LA(1)==EQ)) {
					d = LT(1);
					match(EQ);
					n+=d.getText();
				}
				else if ((LA(1)==STRING)) {
					e = LT(1);
					match(STRING);
					n+='"' + e.getText() + '"';
				}
				else if ((LA(1)==PARENLEFT)) {
					f = LT(1);
					match(PARENLEFT);
					n+=f.getText();
				}
				else if ((LA(1)==PARENRIGHT)) {
					g = LT(1);
					match(PARENRIGHT);
					n+=g.getText();
				}
				else if ((LA(1)==INTEGER)) {
					h = LT(1);
					match(INTEGER);
					n+=h.getText();
				}
				else {
					if ( _cnt39>=1 ) { break _loop39; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt39++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
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
		"SQLEFT",
		"INTEGER",
		"SQRIGHT",
		"ID",
		"DASH",
		"STRING",
		"PARENLEFT",
		"PARENRIGHT"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 530L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 16285952L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 544256L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 16417024L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 262144L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 16679168L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	
	}
