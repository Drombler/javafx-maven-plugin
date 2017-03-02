/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zenjava.javafx.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal bundle-native
 * @requiresProject false
 */
public class BundleNativeMojo extends AbstractMojo {

    /**
     * @parameter property = "jfx.appZipArtifact"
     * @required
     */
    private String appZipArtifact;

    /**
     * @parameter property = "jfx.deployZipArtifact"
     * @required
     */
    private String deployZipArtifact;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
//        try {
//            unpackZips();
//        jfxAppOutputDir = new File(jfxAppOutputDir, "foo");
//        executeNative();
//        } catch (IOException ex) {
//            throw new MojoExecutionException("foo", ex);
//        }
    }

//    private void unpackZips() throws MojoFailureException, MojoExecutionException, IOException {
//        Path appDirPath = Files.createTempDirectory("jfx-bundle-native-app");
//        UnpackMojo unpackMojo = new UnpackMojo();
//        unpackMojo.setOutputDirectory(appDirPath.toFile());
//        unpackMojo.setArtifact(appZipArtifact);

//        unpackMojo.setArtifactItems(Arrays.asList(new ArtifactItem(new Ar)));
//        unpackMojo.setFactory(factory);

//        ReflectMojo reflectCopyDependenciesMojo = new ReflectMojo(unpackMojo, UnpackMojo.class);
//        reflectCopyDependenciesMojo.setField("repositoryFactory", artifactRepositoryFactory);
//        reflectCopyDependenciesMojo.setField("repositoryLayouts", artifactRepositoryLayouts);
//        reflectCopyDependenciesMojo.setField("installer", artifactInstaller);
//        ReflectMojo reflectAbstractFromConfigurationMojo = new ReflectMojo(unpackMojo,
//                AbstractFromConfigurationMojo.class);
//        reflectAbstractFromConfigurationMojo.setField("outputDirectory", bundleDirPath.toFile());
//        reflectAbstractFromConfigurationMojo.setField("useRepositoryLayout", false);
//        reflectAbstractFromConfigurationMojo.setField("copyPom", false);
//
//
//        ReflectMojo reflectAbstractDependencyMojo = new ReflectMojo(unpackMojo,
//                AbstractDependencyMojo.class);
//        reflectAbstractDependencyMojo.setField("project", project);
//        reflectAbstractDependencyMojo.setField("factory", artifactFactory);
//        unpackMojo.execute();
//    }

}
