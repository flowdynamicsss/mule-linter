package com.mule.linter;

import com.mule.linter.model.ValidationIssue;
import com.mule.linter.rules.ProjectRule;
import com.mule.linter.rules.Rules;
import com.mule.linter.rules.XmlRule;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Starts the linter. Give it a Mule project folder, it runs all the checks and writes a JSON report.
 * <p>
 * Some checks look at the whole project (the folder name, the Java files) and run once.
 * The others look at every XML file in src/main/mule. The list of checks is in Rules.java.
 */
public class MuleLinter {

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            printUsage();
            return;
        }
        if (args.length > 2 || args[0].startsWith("-")) {
            System.out.println("Invalid arguments.");
            printUsage();
            return;
        }

        File projectFolder = new File(args[0]).getCanonicalFile();
        File muleFolder = new File(projectFolder, "src/main/mule");
        if (!muleFolder.isDirectory()) {
            System.out.println("Error: The 'src/main/mule' directory does not exist at: " + muleFolder.getAbsolutePath());
            return;
        }

        List<ValidationIssue> issues = new ArrayList<>();
        issues.addAll(checkProject(projectFolder));
        issues.addAll(checkXmlFiles(muleFolder));

        Path reportPath = args.length > 1 ? Path.of(args[1]) : defaultReportPath();
        ReportWriter.write(issues, reportPath);
    }

    private static List<ValidationIssue> checkProject(File projectFolder) throws IOException {
        List<ValidationIssue> issues = new ArrayList<>();
        for (ProjectRule rule : Rules.PROJECT) {
            issues.addAll(rule.check(projectFolder));
        }
        return issues;
    }

    private static List<ValidationIssue> checkXmlFiles(File muleFolder) throws IOException {
        List<ValidationIssue> issues = new ArrayList<>();
        XmlParser parser = new XmlParser();

        for (Path path : findXmlFiles(muleFolder)) {
            try {
                Document xml = parser.parse(path.toFile());
                RuleContext file = new RuleContext(muleFolder, path.toFile());
                for (XmlRule rule : Rules.XML) {
                    issues.addAll(rule.check(xml, file));
                }
            } catch (SAXException | IOException e) {
                // Skip the broken file and go on with the next one
                String line = e instanceof SAXParseException
                        ? " at line " + ((SAXParseException) e).getLineNumber()
                        : "";
                System.out.println("Error parsing XML file " + path.getFileName() + line + ". Details omitted.");
            }
        }

        return issues;
    }

    private static List<Path> findXmlFiles(File folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar mule-xml-linter.jar <project-root> [report-path]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  <project-root>  Mule project directory containing src/main/mule");
        System.out.println("  [report-path]   Optional JSON report path; defaults to report.json beside the linter JAR");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar mule-xml-linter.jar .");
        System.out.println("  java -jar mule-xml-linter.jar C:\\projects\\orders C:\\reports\\orders.json");
        System.out.println("  java -jar mule-xml-linter.jar --help");
    }

    /** By default the report is saved next to the JAR. */
    private static Path defaultReportPath() throws IOException {
        try {
            Path codeLocation = Path.of(MuleLinter.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path outputDirectory = Files.isDirectory(codeLocation)
                    ? codeLocation
                    : codeLocation.getParent();
            if ("classes".equals(outputDirectory.getFileName().toString())) {
                outputDirectory = outputDirectory.getParent();
            }
            return outputDirectory.resolve("report.json");
        } catch (URISyntaxException e) {
            throw new IOException("Unable to determine the linter output directory", e);
        }
    }
}
