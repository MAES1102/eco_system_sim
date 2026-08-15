package com.ecosystem.simulation.rules.lang;

/**
 * Lexical categories produced by {@link Lexer} and consumed by {@link Parser}.
 */
public enum TokenType {
    NUMBER, IDENTIFIER,
    AND, OR, NOT,
    LPAREN, RPAREN, COMMA,
    LT, LE, GT, GE, EQ, NE,
    PLUS, MINUS, STAR, SLASH,
    ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN,
    EOF
}
