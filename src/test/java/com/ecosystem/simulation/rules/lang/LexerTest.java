package com.ecosystem.simulation.rules.lang;

import com.ecosystem.simulation.rules.RuleParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LexerTest {

    @Test
    void tokenizesNumberIdentifierAndComparator() throws RuleParseException {
        List<Token> tokens = new Lexer("energy < 10", 1).tokenize();
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("energy", tokens.get(0).text());
        assertEquals(TokenType.LT, tokens.get(1).type());
        assertEquals(TokenType.NUMBER, tokens.get(2).type());
        assertEquals(10.0, tokens.get(2).numberValue());
        assertEquals(TokenType.EOF, tokens.get(3).type());
    }

    @Test
    void tokenizesNamespacedIdentifier() throws RuleParseException {
        List<Token> tokens = new Lexer("env.temperature", 1).tokenize();
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("env.temperature", tokens.get(0).text());
    }

    @Test
    void tokenizesKeywordsAndOperators() throws RuleParseException {
        List<Token> tokens = new Lexer("NOT (a AND b) OR c", 1).tokenize();
        assertEquals(TokenType.NOT, tokens.get(0).type());
        assertEquals(TokenType.LPAREN, tokens.get(1).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).type());
        assertEquals(TokenType.AND, tokens.get(3).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(4).type());
        assertEquals(TokenType.RPAREN, tokens.get(5).type());
        assertEquals(TokenType.OR, tokens.get(6).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(7).type());
    }

    @Test
    void tokenizesCompoundAssignmentOperators() throws RuleParseException {
        // energy(0) +=(1) 5(2) ,(3) speed(4) *=(5) 1.1(6) EOF(7)
        List<Token> tokens = new Lexer("energy += 5, speed *= 1.1", 1).tokenize();
        assertEquals(TokenType.PLUS_ASSIGN, tokens.get(1).type());
        assertEquals(TokenType.COMMA, tokens.get(3).type());
        assertEquals(TokenType.STAR_ASSIGN, tokens.get(5).type());
    }

    @Test
    void tokenizesDecimalNumber() throws RuleParseException {
        List<Token> tokens = new Lexer("0.8", 1).tokenize();
        assertEquals(0.8, tokens.get(0).numberValue(), 1e-9);
    }

    @Test
    void distinguishesEqualsFromAssignment() throws RuleParseException {
        List<Token> tokens = new Lexer("energy == 10", 1).tokenize();
        assertEquals(TokenType.EQ, tokens.get(1).type());

        tokens = new Lexer("energy = 10", 1).tokenize();
        assertEquals(TokenType.ASSIGN, tokens.get(1).type());
    }

    @Test
    void illegalCharacter_throwsWithPosition() {
        RuleParseException ex = assertThrows(RuleParseException.class, () -> new Lexer("energy $ 10", 3).tokenize());
        assertEquals(3, ex.getLineNumber());
        assertEquals(7, ex.getColumn());
    }

    @Test
    void loneBang_throwsHelpfulMessage() {
        RuleParseException ex = assertThrows(RuleParseException.class, () -> new Lexer("energy ! 10", 1).tokenize());
        assertEquals("Unexpected character '!' (did you mean '!='?) (line 1, column 7)", ex.getMessage());
    }
}
