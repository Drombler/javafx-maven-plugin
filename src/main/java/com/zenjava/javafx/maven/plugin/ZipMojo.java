/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zenjava.javafx.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * @goal zip
 * @phase package
 */
public class ZipMojo extends AbstractJfxToolsMojo {
    private static final String[] DEFAULT_EXCLUDES = new String[]{};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents is being packaged into the APP-ZIP.
     *
     * @parameter
     */
    private String[] appIncludes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents is being packaged into the APP-ZIP.
     *
     * @parameter
     */
    private String[] appExcludes;

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents is being packaged into the APP-ZIP.
     *
     * @parameter
     */
    private String[] deployIncludes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents is being packaged into the APP-ZIP.
     *
     * @parameter
     */
    private String[] deployExcludes;

    /**
     * Directory containing the generated JAR.
     *
     * @parameter default-value = "${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter default-value="${project.basedir}"
     * @readonly
     */
    private File basedir;

    /**
     * Name of the generated JAR.
     *
     * @parameter default-value = "${project.build.finalName}"
     * @readonly
     */
    private String finalName;

    /**
     * The Jar archiver.
     *
     * @component role = "org.codehaus.plexus.archiver.Archiver" roleHint = "zip"
     */
    private ZipArchiver zipArchiver;

    /**
     * The {@link {MavenProject}.
     */
    /**
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * The {@link MavenSession}.
     *
     * @parameter default-value="${session}"
     * @readonly
     * @required
     */
    private MavenSession session;


    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Require the jar plugin to build a new JAR even if none of the contents appear to have changed. By default, this plugin looks to see if the output jar exists and inputs have not changed. If
     * these conditions are true, the plugin skips creation of the jar. This does not work when other plugins, like the maven-shade-plugin, are configured to post-process the jar. This plugin can not
     * detect the post-processing, and so leaves the post-processed jar in place. This can lead to failures when those plugins do not expect to find their own output as an input. Set this parameter to
     * <tt>true</tt> to avoid these problems by forcing this plugin to recreate the jar every time.<br/>
     * Starting with <b>3.0.0</b> the property has been renamed from <code>jar.forceCreation</code> to <code>maven.jar.forceCreation</code>.
     *
     * @parameter property = "jfx.forceCreation" default-value = "false"
     */
    private boolean forceCreation;

    /**
     * Generates the JAR.
     *
     * @throws MojoExecutionException in case of an error.
     */
    @Override
    public void execute() throws MojoExecutionException {
        createAndAttachZipFile("app-zip", "app-zip", jfxAppOutputDir.toPath(), appIncludes, appExcludes);
        zipArchiver.reset();
        createAndAttachZipFile("deploy-zip", "deploy-zip", basedir.toPath().resolve(deployDir), deployIncludes, deployExcludes);
    }

    private void createAndAttachZipFile(String type, String classifier, Path dirPath, String[] includes, String[] excludes) throws MojoExecutionException {
        Path zipPath = createArchive(classifier, dirPath, includes, excludes);
        projectHelper.attachArtifact(project, type, classifier, zipPath.toFile());
    }

    private Path createArchive(String classifier, Path dirPath, String[] includes, String[] excludes) throws MojoExecutionException {
        Path zipFilePath = getZipFile(outputDirectory.toPath(), finalName, classifier);

        zipArchiver.setDestFile(zipFilePath.toFile());
        zipArchiver.setForced(forceCreation);

        try {

            if (Files.exists(dirPath)) {
                zipArchiver.addDirectory(dirPath.toFile(), includes, excludes);
            } else {
                getLog().warn("JAR will be empty - no content was marked for inclusion!");
            }

            zipArchiver.createArchive();

            return zipFilePath;
        } catch (IOException | RuntimeException e) {
            // TODO: improve error handling
            throw new MojoExecutionException("Error assembling ZIP", e);
        }
    }

    private Path getZipFile(Path basedir, String resultFinalName, String classifier) {
        if (basedir == null) {
            throw new IllegalArgumentException("basedir is not allowed to be null");
        }
        if (resultFinalName == null) {
            throw new IllegalArgumentException("finalName is not allowed to be null");
        }

        String fileName = resultFinalName + "-" + classifier + ".zip";

        return basedir.resolve(fileName);
    }

    private String[] getIncludes(String[] includes) {
        if (includes != null && includes.length > 0) {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getExcludes(String[] excludes) {
        if (excludes != null && excludes.length > 0) {
            return excludes;
        }
        return DEFAULT_EXCLUDES;
    }
}
