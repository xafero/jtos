package org.xafero.jtos.compat;

public class SwiftDefaults {

	public static String getDefault(String type) {

		switch (type) {

		case "Int64":
		case "Int32":
		case "Int":
		case "Int16":
		case "Int8":
			return "0";

		case "Bool":
			return "false";

		case "String":
			return '"' + "" + '"';

		case "Character":
			return '"' + " " + '"';

		default:
			break;
		}

		return null;
	}

}
