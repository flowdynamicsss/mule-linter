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
        String baseDir = args.length > 0 ? args[0] : ".";
        File projectDir = new File(baseDir, "src/main/mule");

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
        File projectRoot = projectDir.getParentFile().getParentFile().getParentFile();
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

        writeReport(allIssues);
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

    private static void writeReport(List<ValidationIssue> issues) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("issues", issues);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

        try (FileWriter writer = new FileWriter("report.json")) {
            writer.write(json);
        }

        System.out.println("Execution finished! Results written to report.json");
        System.out.println(json);
    }
}
