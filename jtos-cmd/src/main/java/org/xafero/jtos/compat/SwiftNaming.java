package org.xafero.jtos.compat;

import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MemberSource;

public class SwiftNaming {

	public static String rewrite(MemberSource<JavaClassSource, ?> member) {
		if (member == null)
			return null;

		final String name = member.getName();
		String newName = null;

		switch (name) {
		case "init":
			newName = "´init´";
			break;
		default:
			return name;
		}

		return newName;
	}
}
