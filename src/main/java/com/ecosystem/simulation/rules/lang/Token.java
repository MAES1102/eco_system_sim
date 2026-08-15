package com.ecosystem.simulation.rules.lang;

/**
 * A single lexical token.
 *
 * <p>{@code position} is the character offset within the condition/action
 * clause being lexed, used to produce precise syntax-error messages.</p>
 *
 * @param type        lexical category
 * @param text        raw source text (identifier name, operator symbol, ...)
 * @param numberValue parsed numeric value; meaningful only when {@code type == NUMBER}
 * @param position    0-based character offset where this token starts
 */
public record Token(TokenType type, String text, double numberValue, int position) {

    public static Token of(TokenType type, String text, int position) {
        return new Token(type, text, 0.0, position);
    }

    public static Token number(double value, String text, int position) {
        return new Token(TokenType.NUMBER, text, value, position);
    }

    @Override
    public String toString() {
        return type == TokenType.NUMBER ? Double.toString(numberValue) : text;
    }
}
