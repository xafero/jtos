package org.xafero.jtos.core;

import org.apache.commons.text.TextStringBuilder;
import org.xafero.jtos.api.SwiftMember;

public class SwiftField implements SwiftMember {

	private String name;
	private String type;
	private String initializer;
	private String getter;
	private boolean isStatic;
	private boolean isConstant;

	public SwiftField(String memberName) {
		this.name = memberName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getInitializer() {
		return initializer;
	}

	public void setInitializer(String initializer) {
		this.initializer = initializer;
	}

	public boolean isConstant() {
		return isConstant;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setConstant(boolean isConstant) {
		this.isConstant = isConstant;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public String getGetter() {
		return getter;
	}

	public void setGetter(String getter) {
		this.getter = getter;
	}

	@Override
	public String toString() {
		String offset = '\t' + "";
		TextStringBuilder bld = new TextStringBuilder();
		String kind = isConstant ? "let" : "var";
		if (isStatic) {
			kind = "static " + kind;
		}
		bld.append(offset + kind + " " + name + " : " + type);
		if (initializer != null) {
			bld.append(" = " + initializer);
		}
		if (getter != null) {
			bld.append(" { get { return " + getter + " } }");
		}
		bld.appendln("");
		return bld.toString();
	}
}
