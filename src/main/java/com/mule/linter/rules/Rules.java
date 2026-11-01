package com.mule.linter.rules;

import com.mule.linter.Namespaces;

import java.util.List;

/**
 * The list of all checks. This is the only place to change when you add or remove a check.
 */
public final class Rules {

    private Rules() {
    }

    /** Checks for the whole project. They run once. */
    public static final List<ProjectRule> PROJECT = List.of(
            new ProjectNamingRule(),
            new JavaClassNamingRule()
    );

    /** Checks for XML files. They run on every file in src/main/mule. */
    public static final List<XmlRule> XML = List.of(
            new ApikitNamingConsistencyRule(),
            new ApiListenerPathRule(),
            new ConfigSecretRule(),
            new VariableNamingRule(),
            new DefaultNameRule("choice-default-name", Namespaces.CORE, "choice", "Choice"),
            new DefaultNameRule("set-variable-default-name", Namespaces.CORE, "set-variable", "Set Variable"),
            new DefaultNameRule("transform-default-name", Namespaces.EE_CORE, "transform", "Transform Message")
    );
}
