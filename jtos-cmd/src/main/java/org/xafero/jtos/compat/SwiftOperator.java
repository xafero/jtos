package org.xafero.jtos.compat;

import com.github.javaparser.ast.expr.BinaryExpr.Operator;

public class SwiftOperator {

	public static String getSymbol(Operator operator) {
		switch (operator) {
		case AND:
			return "&&";
		case BINARY_AND:
			return "&";
		case BINARY_OR:
			return "|";
		case DIVIDE:
			return "/";
		case EQUALS:
			return "==";
		case GREATER:
			return ">";
		case GREATER_EQUALS:
			return ">=";
		case LEFT_SHIFT:
			return "<<";
		case LESS:
			return "<";
		case LESS_EQUALS:
			return "<=";
		case MINUS:
			return "-";
		case MULTIPLY:
			return "*";
		case NOT_EQUALS:
			return "!=";
		case OR:
			return "||";
		case PLUS:
			return "+";
		case REMAINDER:
			return "%";
		case SIGNED_RIGHT_SHIFT:
			return ">>";
		case UNSIGNED_RIGHT_SHIFT:
			return ">>";
		case XOR:
			return "||";
		default:
			break;
		}
		throw new RuntimeException(operator.toString());
	}

	public static String getSymbol(com.github.javaparser.ast.expr.UnaryExpr.Operator operator) {
		switch (operator) {
		case BITWISE_COMPLEMENT:
			return "~";
		case LOGICAL_COMPLEMENT:
			return "!";
		case MINUS:
			return "-";
		case PLUS:
			return "+";
		case POSTFIX_DECREMENT:
			return " -= 1";
		case POSTFIX_INCREMENT:
			return " += 1";
		case PREFIX_DECREMENT:
			break;
		case PREFIX_INCREMENT:
			break;
		default:
			break;
		}
		throw new RuntimeException(operator.toString());
	}

	public static String getSymbol(com.github.javaparser.ast.expr.AssignExpr.Operator operator) {
		switch (operator) {
		case ASSIGN:
			return "=";
		case BINARY_AND:
			return "&=";
		case BINARY_OR:
			return "|=";
		case DIVIDE:
			return "/=";
		case LEFT_SHIFT:
			return "<<=";
		case MINUS:
			return "-=";
		case MULTIPLY:
			return "*=";
		case PLUS:
			return "+=";
		case REMAINDER:
			return "%=";
		case SIGNED_RIGHT_SHIFT:
			return ">>=";
		case UNSIGNED_RIGHT_SHIFT:
			return ">>=";
		case XOR:
			break;
		default:
			break;
		}
		throw new RuntimeException(operator.toString());
	}
}