package mithal;

import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class Tokenizer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("و", TokenType.AND);
        keywords.put("أو", TokenType.OR);
        keywords.put("إذ", TokenType.IF);
        keywords.put("وإلا", TokenType.ELSE);
        keywords.put("حق", TokenType.TRUE);
        keywords.put("باطل", TokenType.FALSE);
        keywords.put("لكل", TokenType.FOR);
        keywords.put("طالما", TokenType.WHILE);
        keywords.put("دالة", TokenType.FUN);
        keywords.put("ارجع", TokenType.RETURN);
        keywords.put("صافر", TokenType.NIL);
        keywords.put("قل", TokenType.PRINT);
        keywords.put("الأصل", TokenType.SUPER);
        keywords.put("فرعي", TokenType.THIS);
        keywords.put("متغ", TokenType.VAR);
    }

    Tokenizer(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while(!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case '،': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case '؛': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;

            case '!': 
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); 
                break;
            case '=': 
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); 
                break;
            case '<': 
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); 
                break;
            case '>': 
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); 
                break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line/file.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            case ' ':
            case '\r': 
            case '\t':
                break;

            case '\n':
                line++;
                break;

            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Mithal.error(line, "Unexpected character.");
                }
                break; 
        }
    }

    private boolean isAtEnd() {
        return current >= source.length();  
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true; 
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean isDigit(char c) {
        return isEasternArabicNumeral(c) || isWesternArabicNumeral(c);
    }

    private boolean isEasternArabicNumeral(char c) {
        return Pattern.matches("[١٢٣٤٥٦٧٨٩٠]", String.valueOf(c));
    }

    private boolean isWesternArabicNumeral(char c) {
        return Pattern.matches("[0-9]", String.valueOf(c));
    }

    private boolean isAlpha(char c) {
        // TODO: Write proper regex
        // English characters are supported momentarily for debugging until the language is complete
        return Pattern.matches("[ًٌَُّْدجحخهعغفقثصضطنتالبيسشظزوةىلارؤءئ]", String.valueOf(c)) ||
                (c >= 'a') && (c <= 'z') ||
                (c >= 'A') && (c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isDigit(c) || isAlpha(c);
    }

    // TODO: Disallow numbers that mix between Eastern and Western Arabic numerals
    private void number() {
        while(isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while(!isDigit(peek())) advance();
        }

        String literal = source.substring(start, current);
        if (isEasternArabicNumeral(literal.charAt(0))) {
            StringBuilder sb = new StringBuilder();
            for (char c : literal.toCharArray()) {
                if (isEasternArabicNumeral(c)) {
                    sb.append(Character.getNumericValue(c));
                } else {
                    sb.append(c);
                }
            }
            literal = sb.toString();
        }
        addToken(TokenType.NUMBER, Double.parseDouble(literal));
    }

    private void string() {
        while(peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Mithal.error(line, "سلسلة بتراء"); // Arabic for "Prematurely terminated string"
            return;
        }

        advance();

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private void identifier() {
        while(isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }
}
