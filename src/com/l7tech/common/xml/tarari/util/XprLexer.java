// $ANTLR 2.7.4: "xparser.g" -> "XprLexer.java"$

package com.l7tech.common.xml.tarari.util;

import antlr.*;
import antlr.collections.impl.BitSet;

import java.io.InputStream;
import java.io.Reader;
import java.util.Hashtable;

// global stuff in the lexer file

public class XprLexer extends antlr.CharScanner implements XprLexerTokenTypes, TokenStream
 {

    // additional methods in lexer class

    public XprLexer(InputStream in) {
        this(new ByteBuffer(in));
    }
    public XprLexer(Reader in) {
        this(new CharBuffer(in));
    }
    public XprLexer(InputBuffer ib) {
        this(new LexerSharedInputState(ib));
    }
    public XprLexer(LexerSharedInputState state) {
        super(state);
        caseSensitiveLiterals = true;
        setCaseSensitive(true);
        literals = new Hashtable();
        literals.put(new ANTLRHashString("text", this), new Integer(11));
        literals.put(new ANTLRHashString("declare", this), new Integer(4));
        literals.put(new ANTLRHashString("namespace", this), new Integer(7));
    }

    public Token nextToken() throws TokenStreamException {

        tryAgain:
	for (;;) {

        int _ttype = Token.INVALID_TYPE;
        resetText();
        try {   // for char stream error handling
            try {   // for lexical error handling
                switch ( LA(1)) {
                    case '\t':  case ' ':
                        {
                            mWS(true);
                            break;
                        }
                    case '\n':  case '\r':
                        {
                            mNL(true);
                            break;
                        }
                    case ';':
                        {
                            mSEMI(true);
                            break;
                        }
                    case '/':
                        {
                            mSLASH(true);
                            break;
                        }
                    case '0':  case '1':  case '2':  case '3':
                    case '4':  case '5':  case '6':  case '7':
                    case '8':  case '9':
                        {
                            mINTEGER(true);
                            break;
                        }
                    case '@':
                        {
                            mAT(true);
                            break;
                        }
                    case '=':
                        {
                            mEQ(true);
                            break;
                        }
                    case '"':
                        {
                            mSTRING(true);
                            break;
                        }
                    case 'A':  case 'B':  case 'C':  case 'D':
                    case 'E':  case 'F':  case 'G':  case 'H':
                    case 'I':  case 'J':  case 'K':  case 'L':
                    case 'M':  case 'N':  case 'O':  case 'P':
                    case 'Q':  case 'R':  case 'S':  case 'T':
                    case 'U':  case 'V':  case 'W':  case 'X':
                    case 'Y':  case 'Z':  case 'a':  case 'b':
                    case 'c':  case 'd':  case 'e':  case 'f':
                    case 'g':  case 'h':  case 'i':  case 'j':
                    case 'k':  case 'l':  case 'm':  case 'n':
                    case 'o':  case 'p':  case 'q':  case 'r':
                    case 's':  case 't':  case 'u':  case 'v':
                    case 'w':  case 'x':  case 'y':  case 'z':
                        {
                            mID(true);
                            break;
                        }
                    case '*':
                        {
                            mSTAR(true);
                            break;
                        }
                    default:
                        if ((LA(1)=='[') && ((LA(2) >= '0' && LA(2) <= '9')) && (_tokenSet_0.member(LA(3)))) {
                            mIPRED(true);
                        }
                        else if ((LA(1)=='[') && ((LA(2) >= '\u0000' && LA(2) <= '\u00ff')) && (true)) {
                            mPRED(true);
                        }
                        else if ((LA(1)==':') && (LA(2)=='=')) {
                            mASSIGN(true);
                        }
                        else if ((LA(1)==':') && (true)) {
                            mCOL(true);
                        }
                        else {
                            if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
                            else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
                        }
                }
                if ( _returnToken==null ) continue tryAgain; // found SKIP token
                _ttype = _returnToken.getType();
                _ttype = testLiteralsTable(_ttype);
                _returnToken.setType(_ttype);
                return _returnToken;
            }
            catch (RecognitionException e) {
                throw new TokenStreamRecognitionException(e);
            }
        }
        catch (CharStreamException cse) {
            if ( cse instanceof CharStreamIOException ) {
                throw new TokenStreamIOException(((CharStreamIOException)cse).io);
            }
            else {
                throw new TokenStreamException(cse.getMessage());
            }
        }
    }
    }

    public final void mWS(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = WS;


        {
            int _cnt35=0;
            _loop35:
		do {
            switch ( LA(1)) {
                case ' ':
                    {
                        match(' ');
                        break;
                    }
                case '\t':
                    {
                        match('\t');
                        break;
                    }
                default:
                    {
                        if ( _cnt35>=1 ) { break _loop35; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
                    }
            }
            _cnt35++;
        } while (true);
        }

        _ttype = Token.SKIP ;

        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mNL(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = NL;


        {
            int _cnt38=0;
            _loop38:
		do {
            switch ( LA(1)) {
                case '\n':
                    {
                        match('\n');
                        newline() ;
                        break;
                    }
                case '\r':
                    {
                        match('\r');
                        match('\n');
                        newline() ;
                        break;
                    }
                default:
                    {
                        if ( _cnt38>=1 ) { break _loop38; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
                    }
            }
            _cnt38++;
        } while (true);
        }
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mSEMI(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = SEMI;


        match(';');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mSLASH(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = SLASH;


        match('/');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mIPRED(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = IPRED;
        int _saveIndex;

        _saveIndex=text.length();
        match('[');
        text.setLength(_saveIndex);
        mINTEGER(false);
        _saveIndex=text.length();
        match(']');
        text.setLength(_saveIndex);
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mINTEGER(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = INTEGER;


        {
            int _cnt59=0;
            _loop59:
		do {
            if (((LA(1) >= '0' && LA(1) <= '9'))) {
                mDIGIT(false);
            }
            else {
                if ( _cnt59>=1 ) { break _loop59; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
            }

            _cnt59++;
        } while (true);
        }
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mPRED(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = PRED;
        int _saveIndex;

        _saveIndex=text.length();
        match('[');
        text.setLength(_saveIndex);
        {
            _loop45:
		do {
            if ((_tokenSet_1.member(LA(1)))) {
                {
                    match(_tokenSet_1);
                }
            }
            else {
                break _loop45;
            }

        } while (true);
        }
        _saveIndex=text.length();
        match(']');
        text.setLength(_saveIndex);
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mAT(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = AT;


        match('@');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mEQ(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = EQ;


        match('=');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mASSIGN(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = ASSIGN;


        match(':');
        match('=');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mCOL(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = COL;


        match(':');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mSTRING(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = STRING;
        int _saveIndex;

        _saveIndex=text.length();
        match('"');
        text.setLength(_saveIndex);
        {
            _loop53:
		do {
            if ((LA(1)=='\\')) {
                mESC(false);
            }
            else if ((_tokenSet_2.member(LA(1)))) {
                {
                    match(_tokenSet_2);
                }
            }
            else {
                break _loop53;
            }

        } while (true);
        }
        _saveIndex=text.length();
        match('"');
        text.setLength(_saveIndex);
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    protected final void mESC(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = ESC;


        match('\\');
        {
            if ((LA(1)=='"') && ((LA(2) >= '\u0000' && LA(2) <= '\u00ff')) && (true)) {
                match('"');
            }
            else if (((LA(1) >= '\u0000' && LA(1) <= '\u00ff')) && (true) && (true)) {
            }
            else {
                throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
            }

        }
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    protected final void mDIGIT(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = DIGIT;


        matchRange('0','9');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mID(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = ID;


        mIDSTART(false);
        {
            if ((_tokenSet_3.member(LA(1)))) {
                mIDV(false);
            }
            else {
            }

        }
        _ttype = testLiteralsTable(_ttype);
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    protected final void mIDSTART(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = IDSTART;


        {
            switch ( LA(1)) {
                case 'a':  case 'b':  case 'c':  case 'd':
                case 'e':  case 'f':  case 'g':  case 'h':
                case 'i':  case 'j':  case 'k':  case 'l':
                case 'm':  case 'n':  case 'o':  case 'p':
                case 'q':  case 'r':  case 's':  case 't':
                case 'u':  case 'v':  case 'w':  case 'x':
                case 'y':  case 'z':
                    {
                        matchRange('a','z');
                        break;
                    }
                case 'A':  case 'B':  case 'C':  case 'D':
                case 'E':  case 'F':  case 'G':  case 'H':
                case 'I':  case 'J':  case 'K':  case 'L':
                case 'M':  case 'N':  case 'O':  case 'P':
                case 'Q':  case 'R':  case 'S':  case 'T':
                case 'U':  case 'V':  case 'W':  case 'X':
                case 'Y':  case 'Z':
                    {
                        matchRange('A','Z');
                        break;
                    }
                default:
                    {
                        throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
                    }
            }
        }
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    protected final void mIDV(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = IDV;


        {
            int _cnt67=0;
            _loop67:
		do {
            switch ( LA(1)) {
                case 'a':  case 'b':  case 'c':  case 'd':
                case 'e':  case 'f':  case 'g':  case 'h':
                case 'i':  case 'j':  case 'k':  case 'l':
                case 'm':  case 'n':  case 'o':  case 'p':
                case 'q':  case 'r':  case 's':  case 't':
                case 'u':  case 'v':  case 'w':  case 'x':
                case 'y':  case 'z':
                    {
                        matchRange('a','z');
                        break;
                    }
                case 'A':  case 'B':  case 'C':  case 'D':
                case 'E':  case 'F':  case 'G':  case 'H':
                case 'I':  case 'J':  case 'K':  case 'L':
                case 'M':  case 'N':  case 'O':  case 'P':
                case 'Q':  case 'R':  case 'S':  case 'T':
                case 'U':  case 'V':  case 'W':  case 'X':
                case 'Y':  case 'Z':
                    {
                        matchRange('A','Z');
                        break;
                    }
                case '_':
                    {
                        match('_');
                        break;
                    }
                case '0':  case '1':  case '2':  case '3':
                case '4':  case '5':  case '6':  case '7':
                case '8':  case '9':
                    {
                        matchRange('0','9');
                        break;
                    }
                default:
                    {
                        if ( _cnt67>=1 ) { break _loop67; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
                    }
            }
            _cnt67++;
        } while (true);
        }
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    public final void mSTAR(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = STAR;


        match('*');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }

    protected final void mCHAR(boolean _createToken) throws RecognitionException, CharStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = CHAR;


        matchRange('\0','\377');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }


    private static final long[] mk_tokenSet_0() {
        long[] data = { 287948901175001088L, 536870912L, 0L, 0L, 0L};
        return data;
    }
    public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
    private static final long[] mk_tokenSet_1() {
        long[] data = new long[8];
        data[0]=-1L;
        data[1]=-536870913L;
        for (int i = 2; i<=3; i++) { data[i]=-1L; }
        return data;
    }
    public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
    private static final long[] mk_tokenSet_2() {
        long[] data = new long[8];
        data[0]=-17179869185L;
        data[1]=-268435457L;
        for (int i = 2; i<=3; i++) { data[i]=-1L; }
        return data;
    }
    public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
    private static final long[] mk_tokenSet_3() {
        long[] data = { 287948901175001088L, 576460745995190270L, 0L, 0L, 0L};
        return data;
    }
    public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());

}
