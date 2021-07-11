package org.xafero.jtos.core;

import org.apache.commons.text.TextStringBuilder;

public class SwiftParam {

	private String label;
	private String name;
	private String type;

	public SwiftParam() {
		this.label = "_";
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

	@Override
	public String toString() {
		String offset = "";
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(offset + label + " " + name + " : " + type);
		return bld.toString();
	}
}
