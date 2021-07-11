package org.xafero.jtos.core;

import java.util.ArrayList;

import org.apache.commons.text.TextStringBuilder;
import org.xafero.jtos.api.SwiftMember;
import org.xafero.jtos.api.SwiftUnit;

public class SwiftClass implements SwiftUnit {

	private String name;
	private ArrayList<SwiftMember> members;
	private ArrayList<String> imports;
	private String superType;

	public SwiftClass(String name) {
		this.name = name;
		this.members = new ArrayList<SwiftMember>();
		this.imports = new ArrayList<String>();
		this.imports.add("Foundation");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<String> getImports() {
		return imports;
	}

	@Override
	public String toString() {
		TextStringBuilder bld = new TextStringBuilder();
		for (String imprt : imports) {
			bld.appendln("import " + imprt);
		}
		bld.appendln("");
		bld.append("class " + name);
		if (superType != null) {
			bld.append(" : " + superType);
		}
		bld.appendln("");
		bld.appendln("{");
		for (SwiftMember member : members) {
			String text = member.toString();
			bld.appendln(text);
		}
		bld.appendln("}");
		return bld.toString();
	}

	public void add(SwiftMember member) {
		members.add(member);
	}

	public String getSuperType() {
		return superType;
	}

	public void setSuperType(String superType) {
		this.superType = superType;
	}
}
