package com.ecosystem.simulation.rules.lang;

import com.ecosystem.simulation.rules.RuleParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns a raw condition/action clause into a flat list of {@link Token}s.
 *
 * <p>Identifiers may contain a single {@code '.'} (e.g. {@code env.foodLevel},
 * {@code stat.predatorPopulation}) so that namespaced references are lexed as
 * one token without a dedicated DOT token or special-casing in the parser.</p>
 */
public class Lexer {

    private static final Map<String, TokenType> KEYWORDS = Map.of(
            "AND", TokenType.AND,
            "OR", TokenType.OR,
            "NOT", TokenType.NOT
    );

    private final String source;
    private int pos;
    private final int lineNumber;

    public Lexer(String source, int lineNumber) {
        this.source = source == null ? "" : source;
        this.pos = 0;
        this.lineNumber = lineNumber;
    }

    public List<Token> tokenize() throws RuleParseException {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (atEnd()) {
                tokens.add(Token.of(TokenType.EOF, "", pos));
                break;
            }
            int start = pos;
            char c = peek();

            if (Character.isDigit(c)) {
                tokens.add(number(start));
            } else if (Character.isLetter(c)) {
                tokens.add(identifierOrKeyword(start));
            } else {
                tokens.add(operator(start));
            }
        }
        return tokens;
    }

    private Token number(int start) {
        StringBuilder sb = new StringBuilder();
        boolean seenDot = false;
        while (!atEnd() && (Character.isDigit(peek()) || (peek() == '.' && !seenDot))) {
            if (peek() == '.') {
                seenDot = true;
            }
            sb.append(advance());
        }
        String text = sb.toString();
        return Token.number(Double.parseDouble(text), text, start);
    }

    private Token identifierOrKeyword(int start) {
        StringBuilder sb = new StringBuilder();
        while (!atEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '.')) {
            sb.append(advance());
        }
        String text = sb.toString();
        TokenType keyword = KEYWORDS.get(text);
        return Token.of(keyword != null ? keyword : TokenType.IDENTIFIER, text, start);
    }

    private Token operator(int start) throws RuleParseException {
        char c = advance();
        switch (c) {
            case '(': return Token.of(TokenType.LPAREN, "(", start);
            case ')': return Token.of(TokenType.RPAREN, ")", start);
            case ',': return Token.of(TokenType.COMMA, ",", start);
            case '+':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.PLUS_ASSIGN, "+=", start); }
                return Token.of(TokenType.PLUS, "+", start);
            case '-':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.MINUS_ASSIGN, "-=", start); }
                return Token.of(TokenType.MINUS, "-", start);
            case '*':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.STAR_ASSIGN, "*=", start); }
                return Token.of(TokenType.STAR, "*", start);
            case '/':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.SLASH_ASSIGN, "/=", start); }
                return Token.of(TokenType.SLASH, "/", start);
            case '<':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.LE, "<=", start); }
                return Token.of(TokenType.LT, "<", start);
            case '>':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.GE, ">=", start); }
                return Token.of(TokenType.GT, ">", start);
            case '=':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.EQ, "==", start); }
                return Token.of(TokenType.ASSIGN, "=", start);
            case '!':
                if (!atEnd() && peek() == '=') { advance(); return Token.of(TokenType.NE, "!=", start); }
                throw new RuleParseException("Unexpected character '!' (did you mean '!='?)", lineNumber, start);
            default:
                throw new RuleParseException("Unexpected character '" + c + "'", lineNumber, start);
        }
    }

    private void skipWhitespace() {
        while (!atEnd() && Character.isWhitespace(peek())) {
            pos++;
        }
    }

    private boolean atEnd() {
        return pos >= source.length();
    }

    private char peek() {
        return source.charAt(pos);
    }

    private char advance() {
        return source.charAt(pos++);
    }
}
