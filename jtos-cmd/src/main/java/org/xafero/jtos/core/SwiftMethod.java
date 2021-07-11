package org.xafero.jtos.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.TextStringBuilder;
import org.xafero.jtos.api.SwiftMember;

public class SwiftMethod implements SwiftMember {

	private String name;
	private String returnType;
	private List<SwiftParam> params;
	private boolean isConstructor;
	private List<String> lines;
	private List<String> preLines;
	private boolean isOverride;

	public SwiftMethod(String name) {
		this.name = name;
		this.params = new ArrayList<SwiftParam>();
		this.lines = new ArrayList<String>();
		this.preLines = new ArrayList<String>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	public void setConstructor(boolean isConstructor) {
		this.isConstructor = isConstructor;
	}

	public List<String> getLines() {
		return lines;
	}

	public boolean isOverride() {
		return isOverride;
	}

	public void setOverride(boolean isOverride) {
		this.isOverride = isOverride;
	}

	@Override
	public String toString() {
		String offset = '\t' + "";
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(offset);
		if (isOverride)
			bld.append("override ");
		if (isConstructor)
			bld.append("init(");
		else
			bld.append("func " + name + "(");
		boolean isFirst = true;
		for (SwiftParam param : params) {
			if (isFirst)
				isFirst = false;
			else
				bld.append(", ");
			bld.append(param.toString());
		}
		bld.append(")");
		if (returnType != null)
			bld.append(" -> " + this.returnType);
		bld.appendln(" ");
		bld.appendln(offset + "{");
		if (this.lines.isEmpty()) {
			bld.appendln(offset + offset + "abort() // TODO Implement me!");
		} else {
			if (!preLines.isEmpty()) {
				for (String line : preLines) {
					bld.appendln(offset + offset + line.trim());
				}
				bld.appendln("");
			}
		}
		for (String line : lines) {
			bld.appendln(offset + offset + line.trim());
		}
		bld.appendln(offset + "}");
		return bld.toString();
	}

	public void add(SwiftParam parm) {
		params.add(parm);
	}

	public void addLine(String line) {
		lines.add(line);
	}

	public void addPreLine(String line) {
		preLines.add(line);
	}
}
