package org.xafero.jtos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.lang.model.type.NullType;

import org.apache.commons.text.TextStringBuilder;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.JavaUnit;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.xafero.jtos.api.SwiftUnit;
import org.xafero.jtos.compat.SwiftDefaults;
import org.xafero.jtos.compat.SwiftNaming;
import org.xafero.jtos.compat.SwiftOperator;
import org.xafero.jtos.compat.SwiftTyping;
import org.xafero.jtos.core.SwiftClass;
import org.xafero.jtos.core.SwiftField;
import org.xafero.jtos.core.SwiftMethod;
import org.xafero.jtos.core.SwiftNone;
import org.xafero.jtos.core.SwiftParam;
import org.xafero.jtos.tools.SwiftBuild;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.metamodel.TryStmtMetaModel;

public class App {

	public static void main(String[] args) throws Exception {
		if (args == null || args.length != 2) {
			System.out.println("jtos [input_dir] [output_dir]");
			return;
		}

		String rawPath = args[0];
		String outPath = args[1];

		File path = (new File(rawPath)).getAbsoluteFile();
		File out = (new File(outPath)).getAbsoluteFile();
		File srcOut = new File(out, "Sources");
		if (!srcOut.exists())
			srcOut.mkdir();

		File[] subDirs = path.listFiles(f -> f.isDirectory());
		String firstSubDir = subDirs[0].getName();

		SwiftBuild.writeManifest(out, firstSubDir);
		processDir(path, srcOut);

		System.out.println("Done.");
	}

	private static void processDir(File input, File output) {
		if (!output.exists())
			output.mkdir();

		System.out.println("Input path => " + input);
		System.out.println("Output path => " + output);

		for (File inFile : input.listFiles(f -> f.getName().endsWith(".java"))) {
			String outName = inFile.getName().replace(".java", ".swift");
			File outFile = new File(output, outName);
			try {
				processFile(inFile, outFile);
			} catch (Exception e) {
				System.out.println(" !!! " + inFile + " | " + outFile);
				e.printStackTrace();
			}
		}

		for (File dir : input.listFiles(d -> d.isDirectory())) {
			File outSubDir = new File(output, dir.getName());
			processDir(dir, outSubDir);
		}
	}

	private static void processFile(File inFile, File outFile) throws IOException {
		System.out.println(" * '" + inFile.getName() + "' --> " + outFile.getName());

		try (FileInputStream inStream = new FileInputStream(inFile)) {
			try (FileWriter outStream = new FileWriter(outFile)) {
				try (BufferedWriter outWriter = new BufferedWriter(outStream)) {
					JavaUnit unit = Roaster.parseUnit(inStream);
					List<JavaType<?>> types = unit.getTopLevelTypes();
					for (JavaType<?> type : types) {
						outWriter.newLine();
						Object swift = processType(type);
						outWriter.write(swift.toString());
						outWriter.newLine();
					}
				}
			}
		}
	}

	private static SwiftUnit processType(JavaType<?> type) {
		String targetName = type.getName();
		System.out.println("    # " + type.getClass().getSimpleName() + " '" + targetName + "'");

		if (type instanceof JavaClassSource) {
			JavaClassSource jcs = (JavaClassSource) type;
			SwiftClass cl = new SwiftClass(targetName);

			String superType = jcs.getSuperType();
			if (superType != null && !superType.equals("java.lang.Object")) {
				String[] supers = superType.split("\\.");
				cl.setSuperType(supers[supers.length - 1]);
			}

			for (MemberSource<JavaClassSource, ?> member : jcs.getMembers()) {
				String memberName = SwiftNaming.rewrite(member);

				if (member instanceof MethodSource) {
					MethodSource<?> mcs = (MethodSource<?>) member;
					processMethod(mcs, memberName, cl);
				} else if (member instanceof FieldSource) {
					FieldSource<?> fcs = (FieldSource<?>) member;
					processField(fcs, memberName, cl);
				}
			}

			return cl;
		}

		return new SwiftNone();
	}

	private static void processField(FieldSource<?> fcs, String memberName, SwiftClass cl) {
		String[] internalBits = fcs.getInternal().toString().split("=");
		String internalName = internalBits[0].split("\\[")[0];
		if (!internalName.equalsIgnoreCase(memberName)) {
			memberName = internalName;
		}

		SwiftField ft = new SwiftField(memberName);
		System.out.println("       # found field '" + memberName + "'");

		Type<?> fieldType = fcs.getType();
		String fieldSwType = SwiftTyping.rewrite(fieldType);
		ft.setType(fieldSwType);

		if (fcs.isFinal()) {
			ft.setConstant(true);
		}
		if (fcs.isStatic()) {
			ft.setStatic(true);
		}

		String rawIniter = fcs.getLiteralInitializer();
		if (rawIniter == null) {
			String defIniter = SwiftDefaults.getDefault(fieldSwType);
			if (defIniter != null)
				ft.setInitializer(defIniter);
		} else {
			String initer = rawIniter.trim().replaceAll("\\(byte\\)", " ");
			if (initer.equalsIgnoreCase("false") || initer.equalsIgnoreCase("true") || initer.startsWith("\"")) {
				ft.setInitializer(initer);
			} else if (Character.isDigit(initer.charAt(0))
					|| (initer.startsWith("-") && Character.isDigit(initer.charAt(1)))) {
				ft.setInitializer(initer);
			} else if (initer.charAt(0) == '{' && Character.isDigit(initer.charAt(1))) {
				ft.setInitializer("[" + initer.replace('{', ' ').replace('}', ' ') + "]");
			} else if (initer.charAt(0) == '{' && (initer.charAt(1) == 'f' || initer.charAt(1) == 't')) {
				ft.setInitializer("[" + initer.replace('{', ' ').replace('}', ' ') + "]");
			} else if (initer.charAt(0) == '{' && initer.charAt(1) == 'n' && initer.charAt(2) == 'u') {
				ft.setInitializer("[" + initer.replaceAll("null", "nil").replace('{', ' ').replace('}', ' ') + "]");
				ft.setType(buildNullableType(fieldSwType));
			} else if (initer.charAt(0) == '{' && initer.charAt(1) == '{') {
				ft.setInitializer(initer.replace('{', '[').replace('}', ']'));
			} else if (initer.startsWith("new String[")) {
				ft.setInitializer(getArrayInit(getArrayCount(initer), "String", null));
			} else if (initer.startsWith("new char[")) {
				ft.setInitializer(getArrayInit(getArrayCount(initer), "Character", null));
			} else if (initer.startsWith("new short[")) {
				ft.setInitializer(getArrayInit(getArrayCount(initer), "Int16", null));
			} else if (initer.startsWith("new ") && initer.endsWith("]")) {
				ft.setInitializer(getArrayInit(getArrayCount(initer), null, strip(fieldSwType) + "()"));
			} else if (initer.startsWith("{new ")) {
				ft.setInitializer("[" + initer.replaceAll("new ", " ").replace('{', ' ').replace('}', ' ') + "]");
			} else if (initer.startsWith("{")) {
				String someInitTxt = stripClause(initer);
				if (someInitTxt.contains("null")) {
					someInitTxt = someInitTxt.replaceAll("null", "nil");
					ft.setType(buildNullableType(fieldSwType));
				}
				ft.setInitializer("[ " + someInitTxt + " ]");
			} else {
				ft.setGetter(initer);
			}
		}

		cl.add(ft);
	}

	private static String buildNullableType(String fieldSwType) {
		String oldType = fieldSwType;
		String nullType = oldType.substring(0, oldType.length() - 1) + "?]";
		return nullType;
	}

	private static String stripClause(String text) {
		return text.replace('{', ' ').replace('}', ' ').trim();
	}

	private static String strip(String type) {
		return type.replace('[', ' ').replace(']', ' ').trim();
	}

	private static String getArrayInit(int arrayCount, String type, String override) {
		String defVal = override == null ? SwiftDefaults.getDefault(type) : override;
		StringBuilder bld = new StringBuilder();
		bld.append("[");
		for (int i = 0; i < arrayCount; i++) {
			if (i != 0)
				bld.append(", ");
			bld.append(defVal);
		}
		bld.append("]");
		return bld.toString();
	}

	private static int getArrayCount(String initer) {
		String txt = initer.split("\\[")[1].replace(']', ' ').trim();
		return Integer.parseInt(txt);
	}

	private static void processMethod(MethodSource<?> mcs, String memberName, SwiftClass cl) {
		SwiftMethod mt = new SwiftMethod(memberName);
		System.out.println("       # found method '" + memberName + "'");

		Type<?> retType = mcs.getReturnType();
		if (retType == null) {
			mt.setConstructor(true);
			if (cl.getSuperType() != null)
				mt.setOverride(true);
		} else {
			String swiftType = SwiftTyping.rewrite(retType);
			mt.setReturnType(swiftType);
		}

		List<String> varCopy = new ArrayList<String>();
		for (ParameterSource<?> param : mcs.getParameters()) {
			SwiftParam parm = new SwiftParam();
			String parmName = param.getName();
			String copyName = parmName + "Const";
			varCopy.add("var " + parmName + " = " + copyName);
			parm.setName(copyName);
			Type<?> parmType = param.getType();
			String parmSwType = SwiftTyping.rewrite(parmType);
			parm.setType(parmSwType);
			mt.add(parm);
		}

		if (varCopy.size() >= 1)
			for (String varLine : varCopy)
				mt.addPreLine(varLine);

		String methBody = "{ " + mcs.getBody().toString() + " }";
		try {
			BodyDeclaration<?> methDecl = StaticJavaParser.parseBodyDeclaration(methBody);
			BlockStmt block = (BlockStmt) methDecl.getChildNodes().get(0);
			TextStringBuilder translated = processBlock(4, block);
			mt.addLine(translated.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		cl.add(mt);
	}

	private static String createSpaces(int number) {
		StringBuilder bld = new StringBuilder();
		for (int i = 0; i < number; i++) {
			bld.append(" ");
		}
		return bld.toString();
	}

	private static TextStringBuilder processBlock(int offset, BlockStmt block) {
		TextStringBuilder bld = new TextStringBuilder();
		for (Node node : block.getChildNodes()) {
			bld.appendln(processNode(offset + 4, node));
		}
		return bld;
	}

	private static TextStringBuilder processNode(int offset, Node node) {
		if (node instanceof BlockStmt)
			return processBlock(offset, (BlockStmt) node);

		if (node instanceof ReturnStmt)
			return processReturn(offset, (ReturnStmt) node);

		if (node instanceof BreakStmt)
			return processBreak(offset, (BreakStmt) node);

		if (node instanceof ContinueStmt)
			return processContinue(offset, (ContinueStmt) node);

		if (node instanceof ExpressionStmt)
			return processExpressionSt(offset, (ExpressionStmt) node);

		if (node instanceof ForStmt)
			return processForStmt(offset, (ForStmt) node);

		if (node instanceof EmptyStmt)
			return processEmptyStmt(offset, (EmptyStmt) node);

		if (node instanceof WhileStmt)
			return processWhileStmt(offset, (WhileStmt) node);

		if (node instanceof DoStmt)
			return processDoStmt(offset, (DoStmt) node);

		if (node instanceof SwitchStmt)
			return processSwitchStmt(offset, (SwitchStmt) node);

		if (node instanceof TryStmt)
			return processTryStmt(offset, (TryStmt) node);

		if (node instanceof IfStmt)
			return processIfStat(offset, ((IfStmt) node));

		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(spaces + " // " + node.getClass().getSimpleName() + " ?! ");
		return bld;
	}

	private static TextStringBuilder processTryStmt(int offset, TryStmt node) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.appendln(processNode(offset, node.getTryBlock()));
		return bld;
	}

	private static TextStringBuilder processEmptyStmt(int offset, EmptyStmt node) {
		TextStringBuilder bld = new TextStringBuilder();
		return bld;
	}

	private static TextStringBuilder processDoStmt(int offset, DoStmt node) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		bld.appendln(spaces + "repeat {");
		bld.appendln(processNode(offset, node.getBody()));
		bld.appendln(spaces + "} while " + processExpression(offset, node.getCondition()));
		return bld;
	}

	private static TextStringBuilder processContinue(int offset, ContinueStmt node) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(spaces + "continue");
		return bld;
	}

	private static TextStringBuilder processBreak(int offset, BreakStmt node) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(spaces + "break");
		return bld;
	}

	private static TextStringBuilder processWhileStmt(int offset, WhileStmt node) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		bld.appendln(spaces + "while " + processExpression(offset, node.getCondition()));
		bld.appendln(spaces + "{");
		bld.appendln(processNode(offset, node.getBody()));
		bld.appendln(spaces + "}");
		return bld;
	}

	private static TextStringBuilder processExpressionSt(int offset, ExpressionStmt node) {
		Expression coreExpr = node.getExpression();
		if (coreExpr.isAssignExpr()) {
			AssignExpr coreAssExp = (AssignExpr) coreExpr;
			Expression firstTarget = coreAssExp.getTarget();
			Expression valueAssExp = coreAssExp.getValue();
			com.github.javaparser.ast.expr.AssignExpr.Operator op = coreAssExp.getOperator();
			if (valueAssExp.isAssignExpr()) {
				AssignExpr core2AssExp = (AssignExpr) valueAssExp;
				Expression secondTarget = core2AssExp.getTarget();
				Expression secondValue = core2AssExp.getValue();
				TextStringBuilder bld = new TextStringBuilder();
				bld.append(processAssignExpr(offset, firstTarget, op, secondValue));
				bld.append(" ; ");
				bld.append(processAssignExpr(offset, secondTarget, op, secondValue));
				return bld;
			}
		}
		return processExpression(offset, coreExpr);
	}

	private static TextStringBuilder processForStmt(int offset, ForStmt node) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		for (Expression expr : node.getInitialization())
			bld.appendln(processExpression(offset, expr));
		bld.appendln(spaces + "while " + processExpression(offset, node.getCompare().orElse(null)));
		bld.appendln(spaces + "{");
		bld.appendln(processNode(offset + 4, node.getBody()));
		for (Expression expr : node.getUpdate())
			bld.appendln(processExpression(offset + 4, expr));
		bld.appendln(spaces + "}");
		return bld;
	}

	private static TextStringBuilder processSwitchStmt(int offset, SwitchStmt node) {
		boolean hasDefault = false;
		String spaces = createSpaces(offset);
		String caseSpaces = createSpaces(offset + 4);
		TextStringBuilder bld = new TextStringBuilder();
		String ifs = processExpression(offset, node.getSelector()).toString();
		bld.appendln(spaces + "switch " + ifs.stripLeading());
		bld.appendln(spaces + "{");
		List<Expression> emptyLabels = new ArrayList<Expression>();
		for (SwitchEntry entry : node.getEntries()) {
			if (entry.isEmpty()) {
				emptyLabels.addAll(entry.getLabels());
				continue;
			}
			if (entry.getLabels().isEmpty()) {
				hasDefault = true;
				bld.append(caseSpaces + "default");
			} else {
				bld.append(caseSpaces + "case ");
				emptyLabels.addAll(entry.getLabels());
				boolean isFirst = true;
				for (Expression label : emptyLabels) {
					if (isFirst)
						isFirst = false;
					else
						bld.append(", ");
					bld.append(processExpression(offset, label));
				}
				emptyLabels.clear();
			}
			bld.appendln(":");
			for (Statement stat : entry.getStatements())
				bld.appendln(processNode(offset + 4 + 4, stat));
		}
		if (!hasDefault) {
			bld.appendln(caseSpaces + "default:");
			bld.appendln(createSpaces(offset + 4 + 4) + "break");
		}
		bld.appendln(spaces + "}");
		return bld;
	}

	private static TextStringBuilder processIfStat(int offset, IfStmt ifStmt) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		String ifs = processExpression(offset, ifStmt.getCondition()).toString();
		bld.appendln(spaces + "if " + ifs.stripLeading());
		bld.appendln(spaces + "{");
		bld.appendln(processNode(offset + 4, ifStmt.getThenStmt()));
		bld.appendln(spaces + "}");
		Statement elseSt = ifStmt.getElseStmt().orElse(null);
		if (elseSt != null) {
			boolean isElseIf = elseSt instanceof IfStmt;
			if (isElseIf) {
				bld.append(spaces + "else ");
			} else {
				bld.appendln(spaces + "else ");
				bld.appendln(spaces + "{");
			}
			bld.appendln(processNode(offset + (isElseIf ? 0 : 4), elseSt));
			if (!isElseIf)
				bld.appendln(spaces + "}");
		}
		return bld;
	}

	private static TextStringBuilder processReturn(int offset, ReturnStmt node) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		Expression retValExpr = node.getExpression().orElse(null);
		if (retValExpr == null) {
			bld.append(spaces + "return");
		} else {
			String retVal = processExpression(offset, retValExpr).toString();
			bld.append(spaces + "return " + retVal.stripLeading());
		}
		return bld;
	}

	private static TextStringBuilder processExpression(int offset, Expression expression) {
		if (expression == null)
			return null;

		if (expression instanceof NameExpr)
			return processNameExpr(offset, (NameExpr) expression);

		if (expression instanceof FieldAccessExpr)
			return processFieldAcc(offset, (FieldAccessExpr) expression);

		if (expression instanceof VariableDeclarationExpr)
			return processVariableDecl(offset, (VariableDeclarationExpr) expression);

		if (expression instanceof ArrayAccessExpr)
			return processArrayAcc(offset, (ArrayAccessExpr) expression);

		if (expression instanceof UnaryExpr)
			return processUnaryExpr(offset, (UnaryExpr) expression);

		if (expression instanceof AssignExpr)
			return processAssignExpr(offset, (AssignExpr) expression);

		if (expression instanceof IntegerLiteralExpr)
			return processIntLiteralExpr(offset, (IntegerLiteralExpr) expression);

		if (expression instanceof LongLiteralExpr)
			return processLongLiteralExpr(offset, (LongLiteralExpr) expression);

		if (expression instanceof BooleanLiteralExpr)
			return processBoolLiteralExpr(offset, (BooleanLiteralExpr) expression);

		if (expression instanceof ObjectCreationExpr)
			return processCreationExpr(offset, (ObjectCreationExpr) expression);

		if (expression instanceof StringLiteralExpr)
			return processStrLiteralExpr(offset, (StringLiteralExpr) expression);

		if (expression instanceof MethodCallExpr)
			return processMethodCall(offset, (MethodCallExpr) expression);

		if (expression instanceof BinaryExpr)
			return processBinaryExpr(offset, (BinaryExpr) expression);

		if (expression instanceof ArrayCreationExpr)
			return processArrayCreate(offset, (ArrayCreationExpr) expression);

		if (expression instanceof EnclosedExpr)
			return processEnclosedExpr(offset, (EnclosedExpr) expression);

		if (expression instanceof NullLiteralExpr)
			return processNullLiteralExpr(offset, (NullLiteralExpr) expression);

		if (expression instanceof CharLiteralExpr)
			return processCharLiteralExpr(offset, (CharLiteralExpr) expression);

		if (expression instanceof CastExpr)
			return processCastExpr(offset, (CastExpr) expression);

		if (expression instanceof ConditionalExpr)
			return processConditionalExpr(offset, (ConditionalExpr) expression);

		if (expression instanceof ThisExpr)
			return processThisExpr(offset, (ThisExpr) expression);

		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(spaces + " // " + expression.getClass().getSimpleName() + " ?! ");
		return bld;
	}

	private static TextStringBuilder processArrayCreate(int offset, ArrayCreationExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		String type = SwiftTyping.rewrite(expression.getElementType());
		bld.append("Array<" + type + ">()");
		return bld;
	}

	private static TextStringBuilder processConditionalExpr(int offset, ConditionalExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(processExpression(offset, expression.getCondition()));
		bld.append(" ? ");
		bld.append(processExpression(offset, expression.getThenExpr()));
		bld.append(" : ");
		bld.append(processExpression(offset, expression.getElseExpr()));
		return bld;
	}

	private static TextStringBuilder processCreationExpr(int offset, ObjectCreationExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		ClassOrInterfaceType type = expression.getType();
		bld.append(type);
		bld.append("(");
		boolean isFirst = true;
		for (Expression expr : expression.getArguments()) {
			if (isFirst)
				isFirst = false;
			else
				bld.append(", ");
			bld.append(processExpression(offset, expr));
		}
		bld.append(")");
		return bld;
	}

	private static TextStringBuilder processLongLiteralExpr(int offset, LongLiteralExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(expression.getValue().replace('l', ' ').replace('L', ' ').trim());
		return bld;
	}

	private static TextStringBuilder processCharLiteralExpr(int offset, CharLiteralExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append('"' + expression.getValue() + '"');
		return bld;
	}

	private static TextStringBuilder processCastExpr(int offset, CastExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(processExpression(offset, expression.getExpression()));
		bld.append(" as! ");
		bld.append(SwiftTyping.rewrite(expression.getType()));
		return bld;
	}

	private static TextStringBuilder processNullLiteralExpr(int offset, NullLiteralExpr expression) {
		return new TextStringBuilder("nil");
	}

	private static TextStringBuilder processThisExpr(int offset, ThisExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append("self");
		return bld;
	}

	private static TextStringBuilder processEnclosedExpr(int offset, EnclosedExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append("( " + processExpression(offset, expression.getInner()) + " )");
		return bld;
	}

	private static TextStringBuilder processBoolLiteralExpr(int offset, BooleanLiteralExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(expression.getValue());
		return bld;
	}

	private static TextStringBuilder processAssignExpr(int offset, AssignExpr expression) {
		return processAssignExpr(offset, expression.getTarget(), expression.getOperator(), expression.getValue());
	}

	private static TextStringBuilder processAssignExpr(int offset, Expression target,
			com.github.javaparser.ast.expr.AssignExpr.Operator operator, Expression value) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		TextStringBuilder left = processExpression(offset, target);
		String op = SwiftOperator.getSymbol(operator);
		TextStringBuilder right = processExpression(offset, value);
		bld.append(spaces + left + " " + op + " " + right);
		return bld;
	}

	private static TextStringBuilder processStrLiteralExpr(int offset, StringLiteralExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append('"' + expression.getValue() + '"');
		return bld;
	}

	private static TextStringBuilder processBinaryExpr(int offset, BinaryExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		TextStringBuilder left = processExpression(offset, expression.getLeft());
		String op = SwiftOperator.getSymbol(expression.getOperator());
		TextStringBuilder right = processExpression(offset, expression.getRight());
		bld.append(left + " " + op + " " + right);
		return bld;
	}

	private static TextStringBuilder processMethodCall(int offset, MethodCallExpr expression) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		Expression scope = expression.getScope().orElse(null);
		bld.append(spaces);
		Object owner = processExpression(offset, scope);
		if (owner == null)
			owner = "self";
		bld.append(owner + ".");
		bld.append(expression.getName());
		bld.append("(");
		boolean first = true;
		for (Expression expr : expression.getArguments()) {
			if (first)
				first = false;
			else
				bld.append(", ");
			bld.append(processExpression(offset, expr));
		}
		bld.append(")");
		return bld;
	}

	private static TextStringBuilder processIntLiteralExpr(int offset, IntegerLiteralExpr expression) {
		TextStringBuilder bld = new TextStringBuilder();
		bld.append(expression.getValue());
		return bld;
	}

	private static TextStringBuilder processUnaryExpr(int offset, UnaryExpr expression) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		com.github.javaparser.ast.expr.UnaryExpr.Operator opp = expression.getOperator();
		String op = SwiftOperator.getSymbol(opp);
		TextStringBuilder content = processExpression(offset, expression.getExpression());
		if (opp == UnaryExpr.Operator.POSTFIX_DECREMENT || opp == UnaryExpr.Operator.POSTFIX_INCREMENT)
			bld.append(spaces + content + op);
		else
			bld.append(spaces + op + content);
		return bld;
	}

	private static TextStringBuilder processArrayAcc(int offset, ArrayAccessExpr expression) {
		TextStringBuilder name = processExpression(offset, expression.getName());
		TextStringBuilder index = processExpression(offset, expression.getIndex());
		return new TextStringBuilder(name + "[" + index + "]");
	}

	private static TextStringBuilder processFieldAcc(int offset, FieldAccessExpr expression) {
		Expression parent = expression.getScope();
		String name = expression.getName().asString();
		return new TextStringBuilder(processExpression(offset, parent) + "." + name);
	}

	private static TextStringBuilder processVariableDecl(int offset, VariableDeclarationExpr expression) {
		String spaces = createSpaces(offset);
		TextStringBuilder bld = new TextStringBuilder();
		for (VariableDeclarator var : expression.getVariables()) {
			TextStringBuilder value = processExpression(offset, var.getInitializer().orElse(null));
			String varType = SwiftTyping.rewrite(var.getType());
			String valueStr = value == null ? SwiftDefaults.getDefault(varType) : value.toString();
			bld.appendln(spaces + "var " + var.getNameAsString() + " : " + varType + " = " + valueStr);
		}
		return bld;
	}

	private static TextStringBuilder processNameExpr(int offset, NameExpr expression) {
		String name = expression.getName().asString();
		return new TextStringBuilder(name);
	}
}