package com.ecosystem.simulation.rules.lang;

import com.ecosystem.simulation.rules.RuleParseException;
import com.ecosystem.simulation.rules.ast.CompareOp;
import com.ecosystem.simulation.rules.ast.ConditionNode;
import com.ecosystem.simulation.rules.ast.NumericNode;
import com.ecosystem.simulation.rules.ast.RuleAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Small hand-written recursive-descent parser for the rule condition/action grammar:
 *
 * <pre>
 * condition   ::= orExpr
 * orExpr      ::= andExpr ( "OR" andExpr )*
 * andExpr     ::= unary ( "AND" unary )*
 * unary       ::= "NOT" unary | comparison | "(" orExpr ")"
 * comparison  ::= numExpr compareOp numExpr
 * numExpr     ::= term ( ("+"|"-") term )*
 * term        ::= factor ( ("*"|"/") factor )*
 * factor      ::= NUMBER | IDENTIFIER | "(" numExpr ")"
 *
 * actionList  ::= action ( "," action )*
 * action      ::= IDENTIFIER ( mutationOp numExpr )?
 * mutationOp  ::= "=" | "+=" | "-=" | "*=" | "/="
 * </pre>
 *
 * <p>Precedence, low to high: {@code OR} &lt; {@code AND} &lt; {@code NOT} &lt;
 * comparison &lt; {@code + -} &lt; {@code * /} &lt; parentheses/literal.</p>
 *
 * <p>Division by a literal {@code 0} is rejected here, at parse time, because it is
 * statically detectable without any runtime state — division by a value that only
 * turns out to be zero at runtime (e.g. {@code energy / (age - age)}) is instead a
 * graceful runtime no-op, handled by {@link com.ecosystem.simulation.rules.Evaluator}.</p>
 */
public final class Parser {

    private final List<Token> tokens;
    private final int lineNumber;
    private int pos;

    private Parser(List<Token> tokens, int lineNumber) {
        this.tokens = tokens;
        this.lineNumber = lineNumber;
        this.pos = 0;
    }

    public static ConditionNode parseCondition(String expr, int lineNumber) throws RuleParseException {
        if (expr == null || expr.isBlank()) {
            throw new RuleParseException("Condition cannot be blank", lineNumber);
        }
        Parser p = new Parser(new Lexer(expr, lineNumber).tokenize(), lineNumber);
        ConditionNode node = p.orExpr();
        p.expect(TokenType.EOF, "Unexpected trailing text after condition");
        return node;
    }

    public static List<RuleAction> parseActions(String expr, int lineNumber) throws RuleParseException {
        if (expr == null || expr.isBlank()) {
            throw new RuleParseException("Action cannot be blank", lineNumber);
        }
        Parser p = new Parser(new Lexer(expr, lineNumber).tokenize(), lineNumber);
        List<RuleAction> actions = p.actionList();
        p.expect(TokenType.EOF, "Unexpected trailing text after action list");
        return actions;
    }

    // ── condition grammar ────────────────────────────────────────────────────

    private ConditionNode orExpr() throws RuleParseException {
        ConditionNode left = andExpr();
        while (check(TokenType.OR)) {
            advance();
            ConditionNode right = andExpr();
            left = new ConditionNode.Or(left, right);
        }
        return left;
    }

    private ConditionNode andExpr() throws RuleParseException {
        ConditionNode left = unary();
        while (check(TokenType.AND)) {
            advance();
            ConditionNode right = unary();
            left = new ConditionNode.And(left, right);
        }
        return left;
    }

    private ConditionNode unary() throws RuleParseException {
        if (check(TokenType.NOT)) {
            advance();
            return new ConditionNode.Not(unary());
        }
        if (check(TokenType.LPAREN)) {
            advance();
            ConditionNode inner = orExpr();
            expect(TokenType.RPAREN, "Expected ')' to close '('");
            return inner;
        }
        return comparison();
    }

    private ConditionNode comparison() throws RuleParseException {
        NumericNode left = numExpr();
        CompareOp op = compareOp();
        NumericNode right = numExpr();
        return new ConditionNode.Compare(left, op, right);
    }

    private CompareOp compareOp() throws RuleParseException {
        Token t = peek();
        CompareOp op = switch (t.type()) {
            case LT -> CompareOp.LT;
            case LE -> CompareOp.LE;
            case GT -> CompareOp.GT;
            case GE -> CompareOp.GE;
            case EQ -> CompareOp.EQ;
            case NE -> CompareOp.NE;
            default -> null;
        };
        if (op == null) {
            throw error("Expected a comparison operator (<, <=, >, >=, ==, !=) but found '" + t + "'", t);
        }
        advance();
        return op;
    }

    // ── numeric-expression grammar (shared by conditions and mutations) ─────

    private NumericNode numExpr() throws RuleParseException {
        NumericNode left = term();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            char op = check(TokenType.PLUS) ? '+' : '-';
            advance();
            NumericNode right = term();
            left = new NumericNode.BinaryOp(left, op, right);
        }
        return left;
    }

    private NumericNode term() throws RuleParseException {
        NumericNode left = factor();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            boolean isDivide = check(TokenType.SLASH);
            Token opToken = peek();
            advance();
            NumericNode right = factor();
            if (isDivide && right instanceof NumericNode.Literal lit && lit.value() == 0.0) {
                throw error("Division by literal zero is not allowed", opToken);
            }
            left = new NumericNode.BinaryOp(left, isDivide ? '/' : '*', right);
        }
        return left;
    }

    private NumericNode factor() throws RuleParseException {
        Token t = peek();
        if (t.type() == TokenType.MINUS) {
            // Unary minus, e.g. "energy = -999" or "env.temperature < -5".
            // Represented as (0 - factor) rather than a dedicated AST node, keeping
            // the numeric AST to exactly three shapes (Literal/Reference/BinaryOp).
            advance();
            NumericNode operand = factor();
            return new NumericNode.BinaryOp(new NumericNode.Literal(0), '-', operand);
        }
        if (t.type() == TokenType.NUMBER) {
            advance();
            return new NumericNode.Literal(t.numberValue());
        }
        if (t.type() == TokenType.IDENTIFIER) {
            advance();
            return new NumericNode.Reference(t.text());
        }
        if (t.type() == TokenType.LPAREN) {
            advance();
            NumericNode inner = numExpr();
            expect(TokenType.RPAREN, "Expected ')' to close '('");
            return inner;
        }
        throw error("Expected a number, attribute reference, unary '-', or '(' but found '" + t + "'", t);
    }

    // ── action-list grammar ──────────────────────────────────────────────────

    private List<RuleAction> actionList() throws RuleParseException {
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action());
        while (check(TokenType.COMMA)) {
            advance();
            actions.add(action());
        }
        return actions;
    }

    private RuleAction action() throws RuleParseException {
        Token nameToken = expect(TokenType.IDENTIFIER, "Expected an attribute or command name");
        char mutationOp = 0;
        switch (peek().type()) {
            case ASSIGN -> mutationOp = '=';
            case PLUS_ASSIGN -> mutationOp = '+';
            case MINUS_ASSIGN -> mutationOp = '-';
            case STAR_ASSIGN -> mutationOp = '*';
            case SLASH_ASSIGN -> mutationOp = '/';
            default -> mutationOp = 0;
        }
        if (mutationOp != 0) {
            advance();
            NumericNode value = numExpr();
            return new RuleAction.Mutation(nameToken.text(), mutationOp, value);
        }
        return new RuleAction.Command(nameToken.text());
    }

    // ── token-stream helpers ─────────────────────────────────────────────────

    private Token peek() {
        return tokens.get(pos);
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private void advance() {
        if (pos < tokens.size() - 1) {
            pos++;
        }
    }

    private Token expect(TokenType type, String message) throws RuleParseException {
        Token t = peek();
        if (t.type() != type) {
            throw error(message + " but found '" + t + "'", t);
        }
        advance();
        return t;
    }

    private RuleParseException error(String message, Token at) {
        return new RuleParseException(message, lineNumber, at.position());
    }
}
