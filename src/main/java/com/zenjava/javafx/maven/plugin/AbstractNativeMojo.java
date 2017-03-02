/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zenjava.javafx.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author puce
 */
public abstract class AbstractNativeMojo extends AbstractJfxToolsMojo {

    /**
     * Used as the 'id' of the application, and is used as the CFBundleDisplayName on Mac. See the official JavaFX
     * Packaging tools documentation for other information on this. Will be used as GUID on some installers too.
     *
     * @parameter
     */
    protected String identifier;
    /**
     * The vendor of the application (i.e. you). This is required for some of the installation bundles and it's
     * recommended just to set it from the get-go to avoid problems. This will default to the project.organization.name
     * element in you POM if you have one.
     *
     * @parameter property="project.organization.name"
     * @required
     */
    protected String vendor;

    /**
     * Properties passed to the Java Virtual Machine when the application is started (i.e. these properties are system
     * properties of the JVM bundled in the native distribution and used to run the application once installed).
     *
     * @parameter property="jfx.jvmProperties"
     */
    protected Map<String, String> jvmProperties;
    /**
     * JVM Flags to be passed into the JVM at invocation time. These are the arguments to the left of the main class
     * name when launching Java on the command line. For example:
     * <pre>
     *     &lt;jvmArgs&gt;
     *         &lt;jvmArg&gt;-Xmx8G&lt;/jvmArg&gt;
     *     &lt;/jvmArgs&gt;
     * </pre>
     *
     * @parameter property="jfx.jvmArgs"
     */
    protected List<String> jvmArgs;
    /**
     * Optional command line arguments passed to the application when it is started. These will be included in the
     * native bundle that is generated and will be accessible via the main(String[] args) method on the main class that
     * is launched at runtime.
     * <p>
     * These options are user overridable for the value part of the entry via user preferences. The key and the value
     * are concated without a joining character when invoking the JVM.
     *
     * @parameter property="jfx.userJvmArgs"
     */
    protected Map<String, String> userJvmArgs;
    /**
     * You can specify arguments that gonna be passed when calling your application.
     *
     * @parameter property="jfx.launcherArguments"
     */
    protected List<String> launcherArguments;
    /**
     * The release version as passed to the native installer. It would be nice to just use the project's version number
     * but this must be a fairly traditional version string (like '1.34.5') with only numeric characters and dot
     * separators, otherwise the JFX packaging tools bomb out. We default to 1.0 in case you can't be bothered to set
     * a version and don't really care.
     * Normally all non-number signs and dots are removed from the value, which can be disabled
     * by setting 'skipNativeVersionNumberSanitizing' to true.
     *
     * @parameter property="jfx.nativeReleaseVersion" default-value="1.0"
     */
    protected String nativeReleaseVersion;
    /**
     * Set this to true if you would like your application to have a shortcut on the users desktop (or platform
     * equivalent) when it is installed.
     *
     * @parameter property="jfx.needShortcut" default-value=false
     */
    protected boolean needShortcut;
    /**
     * Set this to true if you would like your application to have a link in the main system menu (or platform
     * equivalent) when it is installed.
     *
     * @parameter property="jfx.needMenu" default-value=false
     */
    protected boolean needMenu;
    /**
     * A list of bundler arguments. The particular keys and the meaning of their values are dependent on the bundler
     * that is reading the arguments. Any argument not recognized by a bundler is silently ignored, so that arguments
     * that are specific to a specific bundler (for example, a Mac OS X Code signing key name) can be configured and
     * ignored by bundlers that don't use the particular argument.
     * <p>
     * To disable creating native bundles with JRE in it, just add "&lt;runtime /&gt;" to bundleArguments.
     * <p>
     * If there are bundle arguments that override other fields in the configuration, then it is an execution error.
     *
     * @parameter property="jfx.bundleArguments"
     */
    protected Map<String, String> bundleArguments;
    /**
     * The name of the JavaFX packaged executable to be built into the 'native/bundles' directory. By default this will
     * be the finalName as set in your project. Change this if you want something nicer. This also has effect on the
     * filename of icon-files, e.g. having 'NiceApp' as appName means you have to place that icon
     * at 'src/main/deploy/package/[os]/NiceApp.[icon-extension]' for having it picked up by the bundler.
     *
     * @parameter property="jfx.appName" default-value="${project.build.finalName}"
     */
    protected String appName;

    /**
     * When you need to add additional files to the base-folder of all bundlers (additional non-overriding files like
     * images, licenses or separated modules for encryption etc.) you can specify the source-folder here. All files
     * will be copied recursively. Please make sure to inform yourself about the details of the used bundler.
     *
     * @parameter property="jfx.additionalBundlerResources"
     */
    protected File additionalBundlerResources;
    /**
     * Since Java version 1.8.0 Update 40 the native launcher for linux was changed and includes a bug
     * while searching for the generated configfile. This results in wrong ouput like this:
     * <pre>
     * client-1.1 No main class specified
     * client-1.1 Failed to launch JVM
     * </pre>
     * <p>
     * Scenario (which would work on windows):
     * <p>
     * <ul>
     * <li>generated launcher: i-am.working.1.2.0-SNAPSHOT</li>
     * <li>launcher-algorithm extracts the "extension" (a concept not known in linux-space for executables) and now searches for i-am.working.1.2.cfg</li>
     * </ul>
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
     *
     * @parameter property="jfx.skipNativeLauncherWorkaround124" default-value=false
     */
    protected boolean skipNativeLauncherWorkaround124;
    /**
     * @parameter property="jfx.secondaryLaunchers"
     */
    protected List<NativeLauncher> secondaryLaunchers;
    /**
     * Since Java version 1.8.0 Update 60 the native launcher configuration for windows was changed
     * and includes a bug: the file-format before was "property-file", now it's "INI-file" per default,
     * but the runtime-configuration isn't honored like in property-files.
     * This workaround enforces the property-file-format.
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
     * @parameter property="jfx.skipNativeLauncherWorkaround167" default-value=false
     */
    protected boolean skipNativeLauncherWorkaround167;
    /**
     * It is possible to create file associations when using native installers. When specified,
     * all file associations are bound to the main native launcher. There is no support for bunding
     * them to second launchers.
     * <p>
     * For more informatione, please see official information source: https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/javafx_ant_task_reference.html#CIAIDHBJ
     *
     * @parameter property="jfx.fileAssociations"
     */
    protected List<FileAssociation> fileAssociations;
    /**
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was presented and includes
     * a bug while generating relative file-references when building on windows.
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @parameter property="jfx.skipJNLPRessourcePathWorkaround182"
     */
    protected boolean skipJNLPRessourcePathWorkaround182;
    /**
     * The location of the keystore. If not set, this will default to src/main/deploy/kesytore.jks which is usually fine
     * to use for most cases.
     *
     * @parameter property="jfx.keyStore" default-value="src/main/deploy/keystore.jks"
     */
    protected File keyStore;
    /**
     * The alias to use when accessing the keystore. This will default to "myalias".
     *
     * @parameter property="jfx.keyStoreAlias" default-value="myalias"
     */
    protected String keyStoreAlias;
    /**
     * The password to use when accessing the keystore. This will default to "password".
     *
     * @parameter property="jfx.keyStorePassword" default-value="password"
     */
    protected String keyStorePassword;
    /**
     * The password to use when accessing the key within the keystore. If not set, this will default to
     * keyStorePassword.
     *
     * @parameter property="jfx.keyPassword"
     */
    protected String keyPassword;
    /**
     * The type of KeyStore being used. This defaults to "jks", which is the normal one.
     *
     * @parameter property="jfx.keyStoreType" default-value="jks"
     */
    protected String keyStoreType;
    /**
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was introduced,
     * but lacks the ability to sign jar-files by passing some flag. We are signing the files in the
     * case of having "jnlp" as bundler. The MOJO with the goal "build-web" was deprecated in favor
     * of that new bundler (mostly because the old one does not fit the bundler-list strategy).
     * <p>
     * Change this to "true" when you don't want signing jar-files.
     *
     * @parameter property="jfx.skipSigningJarFilesJNLP185" default-value=false
     */
    protected boolean skipSigningJarFilesJNLP185;
    /**
     * After signing is done, the sizes inside generated JNLP-files still point to unsigned jar-file sizes,
     * so we have to fix these sizes to be correct. This sizes-fix even lacks in the old web-MOJO.
     * <p>
     * Change this to "true" when you don't want to recalculate sizes of jar-files.
     *
     * @parameter property="jfx.skipSizeRecalculationForJNLP185" default-value=false
     */
    protected boolean skipSizeRecalculationForJNLP185;
    /**
     * JavaFX introduced a new way for signing jar-files, which was called "BLOB signing".
     * <p>
     * The tool "jarsigner" is not able to verify that signature and webstart doesn't
     * accept that format either. For not having to call jarsigner yourself, set this to "true"
     * for having your jar-files getting signed when generating JNLP-files.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/190
     *
     * @parameter property="jfx.noBlobSigning" default-value=false
     */
    protected boolean noBlobSigning;
    /**
     * As it is possible to extend existing bundlers, you don't have to use your private
     * version of the javafx-maven-plugin. Just provide a list with the java-classes you
     * want to use, declare them as compile-depencendies and run `mvn jfx:native`
     * or by using maven lifecycle.
     * You have to implement the Bundler-interface (@see com.oracle.tools.packager.Bundler).
     *
     * @parameter property="jfx.customBundlers"
     */
    protected List<String> customBundlers;
    /**
     * Same problem as workaround for bug 124 for native launchers, but this time regarding
     * created native installers, where the workaround didn't apply.
     * <p>
     * Change this to "true" when you don't want this workaround.
     * <p>
     * Requires skipNativeLauncherWorkaround124 to be false.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
     *
     * @parameter property="jfx.skipNativeLauncherWorkaround205" default-value=false
     */
    protected boolean skipNativeLauncherWorkaround205;
    /**
     * @parameter property="jfx.skipMacBundlerWorkaround" default-value=false
     */
    protected boolean skipMacBundlerWorkaround = false;
    /**
     * Per default his plugin does not break the build if any bundler is failing. If you want
     * to fail the build and not just print a warning, please set this to true.
     *
     * @parameter property="jfx.failOnError" default-value=false
     */
    protected boolean failOnError = false;
    /**
     * @parameter property="jfx.onlyCustomBundlers" default-value=false
     */
    protected boolean onlyCustomBundlers = false;
    /**
     * @parameter property="jfx.skipJNLP" default-value=false
     */
    protected boolean skipJNLP = false;
    /**
     * @parameter property="jfx.skipNativeVersionNumberSanitizing" default-value=false
     */
    protected boolean skipNativeVersionNumberSanitizing = false;
    /**
     * Since it it possible to sign created jar-files using jarsigner, it might be required to
     * add some special parameters for calling it (like -tsa and -tsacert). Just add them to this
     * list to have them being applied.
     *
     * @parameter property="jfx.additionalJarsignerParameters"
     */
    protected List<String> additionalJarsignerParameters = new ArrayList<>();
    /**
     * Set this to true, to not scan for the specified main class inside the generated/copied jar-files.
     * <p>
     * Check only works for the main launcher, any secondary launchers are not checked.
     *
     * @parameter property="jfx.skipMainClassScanning"
     */
    protected boolean skipMainClassScanning = false;


}
