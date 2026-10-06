package com.mule.linter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mule.linter.model.ValidationIssue;
import com.mule.linter.rules.ApikitNamingConsistencyRule;
import com.mule.linter.rules.ChoiceRule;
import com.mule.linter.rules.ApiListenerPathRule;
import com.mule.linter.rules.JavaClassNamingRule;
import com.mule.linter.rules.LinterRule;
import com.mule.linter.rules.ProjectNamingRule;
import com.mule.linter.rules.VariableNamingRule;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
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
            new VariableNamingRule(),
            new ChoiceRule()
        );

    public static void main(String[] args) throws IOException {
        File projectRoot = new File(args.length > 0 ? args[0] : ".").getCanonicalFile();
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
                System.out.println("Error parsing file " + path.getFileName() + ": " + e.getMessage());
            }
        }

        Path reportPath = args.length > 1 ? Path.of(args[1]) : getDefaultReportPath();
        writeReport(allIssues, reportPath);
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

        try (FileWriter writer = new FileWriter(reportPath.toFile())) {
            writer.write(json);
        }

        System.out.println("Execution finished! Results written to " + reportPath.toAbsolutePath());
        System.out.println(json);
    }
}
