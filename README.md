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
| `choice-missing-description` | Every `choice` has a `doc:description` |

All of them are reported as warnings for now.

## Report format

```json
{
  "issuesFound": 2,
  "byType": {
    "choice-missing-description": 1,
    "variable-naming-convention": 1
  },
  "issues": [
    {
      "code": "variable-naming-convention",
      "severity": "warning",
      "message": "Variable name 'order_id' is not camelCase.",
      "file": "partner-papi-v1.xml",
      "identifier": "3f1c2a9e-0000-0000-0000-000000000000"
    }
  ]
}
```

The `identifier` helps to find the place in the file. It is usually a `doc:id`
or a flow name, and it can be empty.

## Adding a new rule

1. Create a class in `com.mule.linter.rules` that implements `LinterRule`.
2. Return a list of `ValidationIssue` from `apply(...)`.
3. Add the rule to the `RULES` list in `MuleLinter`.

Rules that work on the whole project instead of a single XML file (like
`ProjectNamingRule`) are called directly from `MuleLinter.main`.

## Notes

- A file that can't be parsed is skipped with a message in the console, and the
  rest of the project is still checked.
- XML files are parsed with DOCTYPE and external entities turned off, so it is
  safe to run on projects from unknown sources.
