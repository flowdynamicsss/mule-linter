package com.mule.linter.rules;

/**
 * Flags {@code <ee:transform>} that has no name or whose name is just the default
 * "Transform Message" (any letter case).
 * The {@code ee:set-variable} elements inside it are not checked, they have no display name.
 */
public class TransformDocNameRule extends DefaultDocNameRule {

    public TransformDocNameRule() {
        super("transform-default-name",
                "http://www.mulesoft.org/schema/mule/ee/core",
                "transform",
                "Transform Message",
                true,
                "The Transform Message component has no name or the default name 'Transform Message'; give it a name that says what it transforms.");
    }
}
