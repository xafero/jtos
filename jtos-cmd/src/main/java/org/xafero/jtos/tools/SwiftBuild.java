package org.xafero.jtos.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SwiftBuild {

	public static void writeManifest(File outDir, String packName) throws IOException {
		File outFile = new File(outDir, "Package.swift");
		System.out.println("Root manifest => " + outFile);

		try (FileWriter writer = new FileWriter(outFile)) {
			try (BufferedWriter outWriter = new BufferedWriter(writer)) {
				outWriter.write("// swift-tools-version:5.3");
				outWriter.newLine();
				outWriter.newLine();

				outWriter.write("import PackageDescription");
				outWriter.newLine();
				outWriter.newLine();

				outWriter.write("let package = Package(");
				outWriter.newLine();
				outWriter.write("    name: \"" + packName + "\",");
				outWriter.newLine();
				outWriter.write("    dependencies: [");
				outWriter.newLine();
				outWriter.write("    ],");
				outWriter.newLine();
				outWriter.write("    targets: [");
				outWriter.newLine();
				outWriter.write("        .target(");
				outWriter.newLine();
				outWriter.write("            name: \"" + packName + "\",");
				outWriter.newLine();
				outWriter.write("            dependencies: [])");
				outWriter.newLine();
				outWriter.write("    ]");
				outWriter.newLine();
				outWriter.write(")");
				outWriter.newLine();
			}
		}
	}
}
