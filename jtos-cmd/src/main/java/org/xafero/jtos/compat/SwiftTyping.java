package org.xafero.jtos.compat;

import org.jboss.forge.roaster.model.Type;

public class SwiftTyping {

	public static String rewrite(Type<?> retType) {
		if (retType == null)
			return null;
		final String name = retType.getName();
		return rewrite(name);
	}

	public static String rewrite(com.github.javaparser.ast.type.Type retType) {
		if (retType == null)
			return null;
		final String name = retType.asString();
		return rewrite(name);
	}

	private static String rewrite(String name) {
		String newName = null;

		switch (name) {
		case "void":
			newName = "Void";
			break;
		case "char":
			newName = "Character";
			break;
		case "boolean":
			newName = "Bool";
			break;
		case "int":
			newName = "Int"; // "Int32";
			break;
		case "short":
			newName = "Int16";
			break;
		case "long":
			newName = "Int"; // "Int64";
			break;
		case "boolean[]":
			newName = "[Bool]";
			break;
		case "long[]":
			newName = "[Int]"; // "[Int64]";
			break;
		case "byte":
			newName = "UInt8";
			break;
		case "byte[]":
			newName = "[UInt8]";
			break;
		case "byte[][]":
			newName = "[[UInt8]]";
			break;
		case "byte[][][]":
			newName = "[[[UInt8]]]";
			break;
		case "short[]":
			newName = "[Int16]";
			break;
		case "char[]":
			newName = "[Character]";
			break;
		case "short[][]":
			newName = "[[Int16]]";
			break;
		case "int[]":
			newName = "[Int]"; // "[Int32]";
			break;
		case "String[]":
			newName = "[String]";
			break;
		case "String[][]":
			newName = "[[String]]";
			break;
		case "Object[][]":
			newName = "[[Object]]";
			break;
		default:
			if (name.endsWith("[]")) {
				newName = "[" + name.split("\\[")[0] + "]";
				break;
			}
			return name;
		}

		return newName;
	}
}
