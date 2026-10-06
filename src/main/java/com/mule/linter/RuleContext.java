package com.mule.linter;

import java.io.File;

/**
 * Shared metadata passed to every rule while it inspects a single Mule XML file.
 */
public class RuleContext {

    private final File projectDir;
    private final File currentFile;

    public RuleContext(File projectDir, File currentFile) {
        this.projectDir = projectDir;
        this.currentFile = currentFile;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    /** Path of the current file relative to the project dir, using forward slashes for consistent JSON output. */
    public String getRelativePath() {
        return projectDir.toPath().relativize(currentFile.toPath()).toString().replace('\\', '/');
    }
}
