package com.mule.linter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mule.linter.model.ValidationIssue;
import com.mule.linter.rules.ApikitNamingConsistencyRule;
import com.mule.linter.rules.ApiListenerPathRule;
import com.mule.linter.rules.ChoiceRule;
import com.mule.linter.rules.ConfigSecretRule;
import com.mule.linter.rules.JavaClassNamingRule;
import com.mule.linter.rules.LinterRule;
import com.mule.linter.rules.ProjectNamingRule;
import com.mule.linter.rules.VariableNamingRule;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Main entry point & orchestrator: walks src/main/mule and runs every registered rule. */
public class MuleLinter {

    private static final List<LinterRule> RULES = List.of(
            new ApikitNamingConsistencyRule(),
            new ApiListenerPathRule(),
            new ConfigSecretRule(),
            new VariableNamingRule(),
            new ChoiceRule()
    );

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

        File projectRoot = new File(args[0]).getCanonicalFile();
        File projectDir = new File(projectRoot, "src/main/mule");

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.out.println("Error: The 'src/main/mule' directory does not exist at: " + projectDir.getAbsolutePath());
            return;
        }

        List<Path> xmlFiles;
        try (Stream<Path> paths = Files.walk(projectDir.toPath())) {
            xmlFiles = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        }

        DocumentBuilder documentBuilder = createSecureDocumentBuilder();
        List<ValidationIssue> allIssues = new ArrayList<>(new ProjectNamingRule().apply(projectRoot));
        allIssues.addAll(new JavaClassNamingRule().apply(projectRoot));

        for (Path path : xmlFiles) {
            try {
                Document document = documentBuilder.parse(path.toFile());
                document.getDocumentElement().normalize();
                RuleContext context = new RuleContext(projectDir, path.toFile());

                for (LinterRule rule : RULES) {
                    allIssues.addAll(rule.apply(document, context));
                }
            } catch (SAXException | IOException e) {
                String line = e instanceof SAXParseException
                        ? " at line " + ((SAXParseException) e).getLineNumber()
                        : "";
                System.out.println("Error parsing XML file " + path.getFileName() + line + ". Details omitted.");
            }
        }

        Path reportPath = args.length > 1 ? Path.of(args[1]) : getDefaultReportPath();
        writeReport(allIssues, reportPath);
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

    private static Path getDefaultReportPath() throws IOException {
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

    private static DocumentBuilder createSecureDocumentBuilder() throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Harden against XXE/billion-laughs since we parse files from arbitrary project checkouts.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("Unable to initialize XML parser", e);
        }
    }

    private static void writeReport(List<ValidationIssue> issues, Path reportPath) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("issues", issues);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

        Path parent = reportPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(reportPath, json, StandardCharsets.UTF_8);

        System.out.println("Execution finished! Results written to " + reportPath.toAbsolutePath());
        System.out.println(json);
    }
}
