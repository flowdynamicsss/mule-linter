# Mule XML Linter

A small command-line tool that checks a Mule 4 project against our team conventions
and writes the problems it finds to a JSON report.

It looks at the XML files in `src/main/mule`, plus the project folder name and the
Java files in `src/main/java`. Nothing is changed in your project, it only reads.

## Requirements

- Java 11 or newer
- Maven (only if you want to build it yourself)

## Build

```bash
mvn clean package
```

This creates a runnable JAR with all dependencies included:

```
target/mule-xml-linter-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

## Usage

```bash
java -jar mule-xml-linter-1.0.0-SNAPSHOT-jar-with-dependencies.jar <project-root> [report-path]
```

- `<project-root>` - the Mule project folder, the one that contains `src/main/mule`
- `[report-path]` - where to save the JSON report (optional). By default it is
  saved as `report.json` next to the JAR.

Examples:

```bash
# lint the project in the current folder (e.g. you are inside partner-papi-v1)
java -jar mule-xml-linter-1.0.0-SNAPSHOT-jar-with-dependencies.jar .

# lint another project and choose where the report goes
java -jar mule-xml-linter-1.0.0-SNAPSHOT-jar-with-dependencies.jar C:\projects\partner-papi-v1 C:\reports\partner-papi-v1.json
```

The report is also printed in the console when the run finishes.

## What it checks

| Rule code | What it looks for |
|-----------|-------------------|
| `project-naming-convention` | Project folder name is kebab-case and has an API layer in it (`sapi`, `papi` or `xapi`), e.g. `partner-papi-v1` |
| `java-class-naming-convention` | Java files in `src/main/java` are named in PascalCase |
| `apikit-naming-mismatch` | The name of an `apikit:config` matches the RAML file it points to |
| `api-listener-path` | Flows with an APIkit router have an HTTP listener on `/api/*` |
| `config-hardcoded-secret` | Passwords, tokens, keys and similar values are not written directly in config, but use `${secure::...}` |
| `variable-naming-convention` | Variables created with `set-variable` are camelCase |
| `choice-default-name` | A `choice` has a name (`doc:name`) and it is not the default `Choice` |
| `set-variable-default-name` | A `set-variable` has a name (`doc:name`) and it is not the default `Set Variable` |
| `transform-default-name` | A Transform Message has a name (`doc:name`) and it is not the default `Transform Message` |

All of them are reported as warnings for now.

A few things that are good to know about the name rules (`set-variable-default-name`,
`transform-default-name` and `choice-default-name`):

- The name is compared as a whole and the letter case is ignored, so `set variable`
  is flagged, but `Set Variable Order` is fine.
- A missing name is reported too, the same as the default one.
- The `ee:set-variable` elements inside a Transform Message are not checked.
- The rules don't follow `flow-ref`. Every file is checked on its own, so a flow in
  another file is checked when that file is processed.

## Report format

```json
{
  "issuesFound": 2,
  "byType": {
    "project-naming-convention": 0,
    "java-class-naming-convention": 0,
    "apikit-naming-mismatch": 0,
    "api-listener-path": 0,
    "config-hardcoded-secret": 0,
    "variable-naming-convention": 1,
    "choice-default-name": 1,
    "set-variable-default-name": 0,
    "transform-default-name": 0
  },
  "issues": [
    {
      "code": "variable-naming-convention",
      "severity": "warning",
      "message": "Variable 'order_id' is not camelCase.",
      "file": "partner-papi-v1.xml",
      "identifier": "3f1c2a9e-0000-0000-0000-000000000000",
      "line": 16
    }
  ]
}
```

`byType` lists every rule, with `0` for the ones that found nothing.

To find the place in the file, use `line` and `identifier`:

- `line` is the line where the element starts. If a tag is written on several lines,
  it is the line where the tag ends. Project-level rules don't have a line.
- `identifier` is the `doc:id` of the element, or of its closest parent. Studio doesn't
  add a `doc:id` to every element, so when there is none it falls back to the name of
  the flow, and for global elements to their own name. It can still be empty.

## How the code is organized

You don't need to know Java to find your way around. The code is in
`src/main/java/com/mule/linter`, and the idea is simple:

1. `MuleLinter.java` is where the program starts. It runs the checks and writes the report.
2. Every check is one small file in the `rules` folder.
3. `rules/Rules.java` is the list of all the checks. It is the only place to change
   when you add or remove a check.
4. A check looks at one XML file (or at the project) and says what is wrong. Each problem
   goes to the report with a rule code, a message, the file and the line.

| File | What it is for |
|------|----------------|
| `MuleLinter.java` | Starts the program and runs the checks |
| `ReportWriter.java` | Writes the JSON report |
| `rules/Rules.java` | The list of all checks |
| `rules/*Rule.java` | The checks, one per file. The three name checks (Choice, Set Variable, Transform Message) share `DefaultNameRule.java` |
| `rules/XmlRule.java`, `rules/ProjectRule.java` | The two kinds of checks: for one XML file, or for the whole project |
| `XmlParser.java` | Reads an XML file and remembers the line of every element |
| `Xml.java` | Small helpers: find elements, get the identifier and the line of an element |
| `RuleContext.java` | The file that is being checked. A rule uses it to create a warning |
| `Namespaces.java` | The XML namespaces of Mule (`core`, `http`, ...), so they are written in one place |
| `model/ValidationIssue.java` | One problem in the report |

If you only want to change a message, a name or a path (like `/api/*`), open the rule
file and change the text in quotes. You don't need to touch anything else.

## Adding a new rule

For a check on XML files, add one file to `src/main/java/com/mule/linter/rules` and one line to
`Rules.java`. This one reports every `logger` that has no message:

```java
package com.mule.linter.rules;

import com.mule.linter.Namespaces;
import com.mule.linter.RuleContext;
import com.mule.linter.Xml;
import com.mule.linter.model.ValidationIssue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/** Checks that every logger has a message. */
public class LoggerMessageRule implements XmlRule {

    @Override
    public String code() {
        return "logger-no-message";   // this is how the rule is named in the report
    }

    @Override
    public List<ValidationIssue> check(Document xml, RuleContext file) {
        List<ValidationIssue> issues = new ArrayList<>();

        for (Element logger : Xml.find(xml, Namespaces.CORE, "logger")) {   // every <logger> in the file
            if (logger.getAttribute("message").isEmpty()) {
                issues.add(file.warning(code(), "Logger has no message.", logger));
            }
        }

        return issues;
    }
}
```

Then add `new LoggerMessageRule()` to the `XML` list in `Rules.java`. The rule is now part of
the run, and it shows up in `byType` in the report by itself.

- If the new namespace is not in `Namespaces.java` yet, add it there.
- A check that only looks at the name (`doc:name`) of one more element doesn't need a new file.
  Add another `DefaultNameRule` line to the `XML` list.
- A check for the whole project (like the folder name) implements `ProjectRule` instead and goes
  in the `PROJECT` list.

## Notes

- A file that can't be parsed is skipped with a message in the console, and the
  rest of the project is still checked.
- XML files are parsed with DOCTYPE and external entities turned off, so it is
  safe to run on projects from unknown sources.
