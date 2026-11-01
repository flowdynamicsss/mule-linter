package com.mule.linter.rules;

/** Flags {@code <set-variable>} whose name is just the default "Set Variable" (any letter case). */
public class SetVariableDocNameRule extends DefaultDocNameRule {

    public SetVariableDocNameRule() {
        super("set-variable-default-name",
                "http://www.mulesoft.org/schema/mule/core",
                "set-variable",
                "Set Variable",
                false,
                "The Set Variable component has the default name 'Set Variable'; give it a name that says what it sets.");
    }
}
