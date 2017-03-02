/*
 * Copyright 2012 Daniel Zwolenski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zenjava.javafx.maven.plugin;

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.oracle.tools.packager.linux.LinuxDebBundler;
import com.oracle.tools.packager.linux.LinuxRpmBundler;
import com.oracle.tools.packager.windows.WinExeBundler;
import com.oracle.tools.packager.windows.WinMsiBundler;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.SignJarParams;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @goal build-native
 */
public class NativeMojo extends AbstractNativeMojo {

    /**
     *
     * The output directory that the native bundles are to be built into. This will be the base directory only as the JavaFX packaging tools use sub-directories that can't be customised. Generally
     * just have a rummage through the sub-directories until you find what you are looking for.
     * <p>
     * This defaults to 'target/jfx/native' and the interesting files are usually under 'bundles'.
     *
     * @parameter property="jfx.nativeOutputDir" default-value="${project.build.directory}/jfx/native"
     */
    protected File nativeOutputDir;

    /**
     * Specify the used bundler found by selected bundleType. May not be installed your OS and will fail in that case.
     *
     * <p>
     * By default this will be set to 'ALL', depending on your installed OS following values are possible for installers:
     * <p>
     * <ul>
     * <li>windows.app <i>(Creates only Windows Executable, does not bundle into Installer)</i></li>
     * <li>linux.app <i>(Creates only Linux Executable, does not bundle into Installer)</i></li>
     * <li>mac.app <i>(Creates only Mac Executable, does not bundle into Installer)</i></li>
     * <li>mac.appStore <i>(Creates a binary bundle ready for deployment into the Mac App Store)</i></li>
     * <li>exe <i>(Microsoft Windows EXE Installer, via InnoIDE)</i></li>
     * <li>msi <i>(Microsoft Windows MSI Installer, via WiX)</i></li>
     * <li>deb <i>(Linux Debian Bundle)</i></li>
     * <li>rpm <i>(Redhat Package Manager (RPM) bundler)</i></li>
     * <li>dmg <i>(Mac DMG Installer Bundle)</i></li>
     * <li>pkg <i>(Mac PKG Installer Bundle)</i></li>
     * </ul>
     *
     * <p>
     * For a full list of available bundlers on your system, call 'mvn jfx:list-bundlers' inside your project.
     *
     * @parameter property="jfx.bundler" default-value="ALL"
     */
    protected String bundler;

    /**
     * Will be set when having goal "build-native" within package-phase and calling "jfx:native" from CLI. Internal usage only.
     *
     * @parameter default-value=false
     */
    protected boolean jfxCallFromCLI;
    protected Workarounds workarounds = null;
    /**
     * When you need to add additional files to generated app-folder (e.g. README, license, third-party-tools, ...), you can specify the source-folder here. All files will be copied recursively.
     *
     * @parameter property="jfx.additionalAppResources"
     */
    protected File additionalAppResources;
    private static final String CFG_WORKAROUND_MARKER = "cfgWorkaroundMarker";
    private static final String CFG_WORKAROUND_DONE_MARKER = CFG_WORKAROUND_MARKER + ".done";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of Native Installers");
            return;
        }

        if( skip ){
            getLog().info("Skipping execution of NativeMojo MOJO.");
            return;
        }

        getLog().info("Building Native Installers");

        workarounds = new Workarounds(nativeOutputDir, getLog());

        try{
            Map<String, ? super Object> params = new HashMap<>();

            // make bundlers doing verbose output (might not always be as verbose as expected)
            params.put(StandardBundlerParam.VERBOSE.getID(), verbose);

            Optional.ofNullable(identifier).ifPresent(id -> {
                params.put(StandardBundlerParam.IDENTIFIER.getID(), id);
            });

            params.put(StandardBundlerParam.APP_NAME.getID(), appName);
            params.put(StandardBundlerParam.VERSION.getID(), nativeReleaseVersion);
            // replace that value
            if( !skipNativeVersionNumberSanitizing && nativeReleaseVersion != null ){
                params.put(StandardBundlerParam.VERSION.getID(), nativeReleaseVersion.replaceAll("[^\\d.]", ""));
            }
            params.put(StandardBundlerParam.VENDOR.getID(), vendor);
            params.put(StandardBundlerParam.SHORTCUT_HINT.getID(), needShortcut);
            params.put(StandardBundlerParam.MENU_HINT.getID(), needMenu);
            params.put(StandardBundlerParam.MAIN_CLASS.getID(), mainClass);

            Optional.ofNullable(jvmProperties).ifPresent(jvmProps -> {
                params.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
            });
            Optional.ofNullable(jvmArgs).ifPresent(jvmOptions -> {
                params.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
            });
            Optional.ofNullable(userJvmArgs).ifPresent(userJvmOptions -> {
                params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
            });
            Optional.ofNullable(launcherArguments).ifPresent(arguments -> {
                params.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
            });

            // bugfix for #83 (by copying additional resources to /target/jfx/app folder)
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/83
            Optional.ofNullable(additionalAppResources).filter(File::exists).ifPresent(appResources -> {
                try{
                    Path targetFolder = jfxAppOutputDir.toPath();
                    Path sourceFolder = appResources.toPath();
                    copyRecursive(sourceFolder, targetFolder);
                } catch(IOException e){
                    getLog().warn(e);
                }
            });

            // gather all files for our application bundle
            Set<File> resourceFiles = new HashSet<>();
            try{
                Files.walk(jfxAppOutputDir.toPath())
                        .map(p -> p.toFile())
                        .filter(File::isFile)
                        .filter(File::canRead)
                        .forEach(f -> {
                            getLog().info(String.format("Add %s file to application resources.", f));
                            resourceFiles.add(f);
                        });
            } catch(IOException e){
                getLog().warn(e);
            }
            params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(jfxAppOutputDir, resourceFiles));

            // check for misconfiguration
            Collection<String> duplicateKeys = new HashSet<>();
            Optional.ofNullable(bundleArguments).ifPresent(bArguments -> {
                duplicateKeys.addAll(params.keySet());
                duplicateKeys.retainAll(bArguments.keySet());
                params.putAll(bArguments);
            });

            if( !duplicateKeys.isEmpty() ){
                throw new MojoExecutionException("The following keys in <bundleArguments> duplicate other settings, please remove one or the other: " + duplicateKeys.toString());
            }

            if( !skipMainClassScanning ){
                boolean mainClassInsideResourceJarFile = resourceFiles.stream().filter(resourceFile -> resourceFile.toString().endsWith(".jar")).filter(resourceJarFile -> isClassInsideJarFile(mainClass, resourceJarFile)).findFirst().isPresent();
                if( !mainClassInsideResourceJarFile ){
                    // warn user about missing class-file
                    getLog().warn(String.format("Class with name %s was not found inside provided jar files!! JavaFX-application might not be working !!", mainClass));
                }
            }

            // check for secondary launcher misconfiguration (their appName requires to be different as this would overwrite primary launcher)
            Collection<String> launcherNames = new ArrayList<>();
            launcherNames.add(appName);
            final AtomicBoolean nullLauncherNameFound = new AtomicBoolean(false);
            // check "no launcher names" and gather all names
            Optional.ofNullable(secondaryLaunchers).filter(list -> !list.isEmpty()).ifPresent(launchers -> {
                getLog().info("Adding configuration for secondary native launcher");
                nullLauncherNameFound.set(launchers.stream().anyMatch(launcher -> launcher.getAppName() == null));
                if( !nullLauncherNameFound.get() ){
                    launcherNames.addAll(launchers.stream().map(launcher -> launcher.getAppName()).collect(Collectors.toList()));

                    // assume we have valid entry here
                    params.put(StandardBundlerParam.SECONDARY_LAUNCHERS.getID(), launchers.stream().map(launcher -> {
                        getLog().info("Adding secondary launcher: " + launcher.getAppName());
                        Map<String, Object> secondaryLauncher = new HashMap<>();
                        addToMapWhenNotNull(launcher.getAppName(), StandardBundlerParam.APP_NAME.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getMainClass(), StandardBundlerParam.MAIN_CLASS.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getJfxMainAppJarName(), StandardBundlerParam.MAIN_JAR.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getNativeReleaseVersion(), StandardBundlerParam.VERSION.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getVendor(), StandardBundlerParam.VENDOR.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getIdentifier(), StandardBundlerParam.IDENTIFIER.getID(), secondaryLauncher);

                        addToMapWhenNotNull(launcher.isNeedMenu(), StandardBundlerParam.MENU_HINT.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.isNeedShortcut(), StandardBundlerParam.SHORTCUT_HINT.getID(), secondaryLauncher);

                        // as we can set another JAR-file, this might be completly different
                        addToMapWhenNotNull(launcher.getClasspath(), StandardBundlerParam.CLASSPATH.getID(), secondaryLauncher);

                        Optional.ofNullable(launcher.getJvmArgs()).ifPresent(jvmOptions -> {
                            secondaryLauncher.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
                        });
                        Optional.ofNullable(launcher.getJvmProperties()).ifPresent(jvmProps -> {
                            secondaryLauncher.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
                        });
                        Optional.ofNullable(launcher.getUserJvmArgs()).ifPresent(userJvmOptions -> {
                            secondaryLauncher.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
                        });
                        Optional.ofNullable(launcher.getLauncherArguments()).ifPresent(arguments -> {
                            secondaryLauncher.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
                        });
                        return secondaryLauncher;
                    }).collect(Collectors.toList()));
                }
            });

            // check "no launcher names"
            if( nullLauncherNameFound.get() ){
                throw new MojoExecutionException("Not all secondary launchers have been configured properly.");
            }
            // check "duplicate launcher names"
            Set<String> duplicateLauncherNamesCheckSet = new HashSet<>();
            launcherNames.stream().forEach(launcherName -> duplicateLauncherNamesCheckSet.add(launcherName));
            if( duplicateLauncherNamesCheckSet.size() != launcherNames.size() ){
                throw new MojoExecutionException("Secondary launcher needs to have different name, please adjust appName inside your configuration.");
            }

            // check and prepare for file-associations (might not be present on all bundlers)
            Optional.ofNullable(fileAssociations).ifPresent(associations -> {
                final List<Map<String, ? super Object>> allAssociations = new ArrayList<>();
                associations.stream().forEach(association -> {
                    Map<String, ? super Object> settings = new HashMap<>();
                    settings.put(StandardBundlerParam.FA_DESCRIPTION.getID(), association.getDescription());
                    settings.put(StandardBundlerParam.FA_ICON.getID(), association.getIcon());
                    settings.put(StandardBundlerParam.FA_EXTENSIONS.getID(), association.getExtensions());
                    settings.put(StandardBundlerParam.FA_CONTENT_TYPE.getID(), association.getContentType());
                    allAssociations.add(settings);
                });
                params.put(StandardBundlerParam.FILE_ASSOCIATIONS.getID(), allAssociations);
            });

            // bugfix for "bundler not being able to produce native bundle without JRE on windows"
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
            if( workarounds.isWorkaroundForBug167Needed() ){
                if( !skipNativeLauncherWorkaround167 ){
                    workarounds.applyWorkaround167(params);
                } else {
                    getLog().info("Skipped workaround for native launcher regarding cfg-file-format.");
                }
            }

            Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?
            Collection<Bundler> loadedBundlers = bundlers.getBundlers();

            // makes it possible to kick out all default bundlers
            if( onlyCustomBundlers ){
                loadedBundlers.clear();
            }

            // don't allow to overwrite existing bundler IDs
            List<String> existingBundlerIds = loadedBundlers.stream().map(existingBundler -> existingBundler.getID()).collect(Collectors.toList());

            Optional.ofNullable(customBundlers).ifPresent(customBundlerList -> {
                customBundlerList.stream().map(customBundlerClassName -> {
                    try{
                        Class<?> customBundlerClass = Class.forName(customBundlerClassName);
                        Bundler newCustomBundler = (Bundler) customBundlerClass.newInstance();
                        // if already existing (or already registered), skip this instance
                        if( existingBundlerIds.contains(newCustomBundler.getID()) ){
                            return null;
                        }
                        return newCustomBundler;
                    } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex){
                        getLog().warn("There was an exception while creating a new instance of custom bundler: " + customBundlerClassName, ex);
                    }
                    return null;
                }).filter(customBundler -> customBundler != null).forEach(customBundler -> {
                    if( onlyCustomBundlers ){
                        loadedBundlers.add(customBundler);
                    } else {
                        bundlers.loadBundler(customBundler);
                    }
                });
            });

            boolean foundBundler = false;

            // the new feature for only using custom bundlers made it necessary to check for empty bundlers list
            if( loadedBundlers.isEmpty() ){
                throw new MojoExecutionException("There were no bundlers registered. Please make sure to add your custom bundlers as dependency to the plugin.");
            }

            for( Bundler b : bundlers.getBundlers() ){
                String currentRunningBundlerID = b.getID();
                // sometimes we need to run this bundler, so do special check
                if( !shouldBundlerRun(bundler, currentRunningBundlerID, params) ){
                    continue;
                }

                foundBundler = true;
                try{
                    if( workarounds.isWorkaroundForNativeMacBundlerNeeded(additionalBundlerResources) ){
                        if( !skipMacBundlerWorkaround ){
                            b = workarounds.applyWorkaroundForNativeMacBundler(b, currentRunningBundlerID, params, additionalBundlerResources);
                        } else {
                            getLog().info("Skipping replacement of the 'mac.app'-bundler. Please make sure you know what you are doing!");
                        }
                    }

                    Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);

                    if( b.validate(paramsToBundleWith) ){

                        doPrepareBeforeBundling(currentRunningBundlerID, paramsToBundleWith);

                        // "jnlp bundler doesn't produce jnlp file and doesn't log any error/warning"
                        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/42
                        // the new jnlp-bundler does not work like other bundlers, you have to provide some bundleArguments-entry :(
                        if( "jnlp".equals(currentRunningBundlerID) && !paramsToBundleWith.containsKey("jnlp.outfile") ){
                            // do fail if JNLP-bundler has to run
                            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/238
                            if( failOnError ){
                                throw new MojoExecutionException("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                            } else {
                                getLog().warn("You missed to specify some bundleArguments-entry, please set 'jnlp.outfile', e.g. using appName.");
                                continue;
                            }
                        }

                        // DO BUNDLE HERE ;) and don't get confused about all the other stuff
                        b.execute(paramsToBundleWith, nativeOutputDir);

                        applyWorkaroundsAfterBundling(currentRunningBundlerID, params);
                    }
                } catch(UnsupportedPlatformException e){
                    // quietly ignored
                } catch(ConfigException e){
                    if( failOnError ){
                        throw new MojoExecutionException("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                    } else {
                        getLog().info("Skipping '" + b.getName() + "' because of configuration error '" + e.getMessage() + "'\nAdvice to fix: " + e.getAdvice());
                    }

                }
            }
            if( !foundBundler ){
                if( failOnError ){
                    throw new MojoExecutionException("No bundler found for given id " + bundler + ". Please check your configuration.");
                }
                getLog().warn("No bundler found for given id " + bundler + ". Please check your configuration.");
            }
        } catch(RuntimeException e){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        } catch(PackagerException ex){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", ex);
        }
    }

    private void applyWorkaroundsAfterBundling(String currentRunningBundlerID, Map<String, ? super Object> params) throws PackagerException, MojoFailureException, MojoExecutionException {

        // Workaround for "Native package for Ubuntu doesn't work"
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
        // real bug: linux-launcher from oracle-jdk starting from 1.8.0u40 logic to determine .cfg-filename
        if( workarounds.isWorkaroundForBug124Needed() ){
            if( "linux.app".equals(currentRunningBundlerID) ){
                getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s).");
                if( !skipNativeLauncherWorkaround124 ){
                    workarounds.applyWorkaround124(appName, secondaryLaunchers);
                    // only apply workaround for issue 205 when having workaround for issue 124 active
                    if( Boolean.parseBoolean(String.valueOf(params.get(CFG_WORKAROUND_MARKER))) && !Boolean.parseBoolean((String) params.get(CFG_WORKAROUND_DONE_MARKER)) ){
                        getLog().info("Preparing workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s) inside native linux installers.");
                        workarounds.applyWorkaround205(appName, secondaryLaunchers, params);
                        params.put(CFG_WORKAROUND_DONE_MARKER, "true");
                    }
                } else {
                    getLog().info("Skipped workaround for native linux launcher(s).");
                }
            }
        }

        if( "jnlp".equals(currentRunningBundlerID) ){
            if( workarounds.isWorkaroundForBug182Needed() ){
                // Workaround for "JNLP-generation: path for dependency-lib on windows with backslash"
                // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/182
                getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding jar-path inside generated JNLP-files.");
                if( !skipJNLPRessourcePathWorkaround182 ){
                    workarounds.fixPathsInsideJNLPFiles();
                } else {
                    getLog().info("Skipped workaround for jar-paths jar-path inside generated JNLP-files.");
                }
            }

            // Do sign generated jar-files by calling the packager (this might change in the future,
            // hopefully when oracle reworked the process inside the JNLP-bundler)
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/185
            if( workarounds.isWorkaroundForBug185Needed(params) ){
                getLog().info("Signing jar-files referenced inside generated JNLP-files.");
                if( !skipSigningJarFilesJNLP185 ){
                    // JavaFX signing using BLOB method will get dropped on JDK 9: "blob signing is going away in JDK9. "
                    // https://bugs.openjdk.java.net/browse/JDK-8088866?focusedCommentId=13889898#comment-13889898
                    if( !noBlobSigning ){
                        getLog().info("Signing jar-files using BLOB method.");
                        signJarFilesUsingBlobSigning();
                    } else {
                        getLog().info("Signing jar-files using jarsigner.");
                        signJarFiles();
                    }
                    workarounds.applyWorkaround185(skipSizeRecalculationForJNLP185);
                } else {
                    getLog().info("Skipped signing jar-files referenced inside JNLP-files.");
                }
            }
        }
    }

    private void doPrepareBeforeBundling(String currentRunningBundlerID, Map<String, ? super Object> paramsToBundleWith) {
        // copy all files every time a bundler runs, because they might cleanup their folders,
        // but user might have extend existing bundler using same foldername (which would end up deleted/cleaned up)
        // fixes "Make it possible to have additional resources for bundlers"
        // see https://github.com/FibreFoX/javafx-gradle-plugin/issues/38
        if( additionalBundlerResources != null && additionalBundlerResources.exists() ){
            boolean skipCopyAdditionalBundlerResources = false;

            // keep previous behaviour
            Path additionalBundlerResourcesPath = additionalBundlerResources.toPath();
            Path resolvedBundlerFolder = additionalBundlerResourcesPath.resolve(currentRunningBundlerID);

            if( verbose ){
                getLog().info("Additional bundler resources are specified, trying to copy all files into build root, using:" + additionalBundlerResources.getAbsolutePath());
            }

            File bundlerImageRoot = AbstractBundler.IMAGES_ROOT.fetchFrom(paramsToBundleWith);
            Path targetFolder = bundlerImageRoot.toPath();
            Path sourceFolder = additionalBundlerResourcesPath;

            // only do copy-stuff when having non-image bundlers
            switch(currentRunningBundlerID) {
                case "windows.app":
                    // no copy required, as we already have "additionalAppResources"
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "windows.service":
                    // no copy required, as we already have "additionalAppResources"
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "mac.app":
                    // custom mac bundler might be used
                    if( skipMacBundlerWorkaround ){
                        getLog().warn("The bundler with ID 'mac.app' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
                    }
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "mac.daemon":
                    // this bundler just deletes everything ... it has no bundlerRoot
                    getLog().warn("The bundler with ID 'mac.daemon' is not supported, as that bundler does not provide any way to copy additional bundler-files.");
                    skipCopyAdditionalBundlerResources = true;
                    break;
                case "linux.app":
                    // no copy required, as we already have "additionalAppResources"
                    skipCopyAdditionalBundlerResources = true;
                    break;
            }

            if( !skipCopyAdditionalBundlerResources ){
                // new behaviour, use bundler-name as folder-name
                if( Files.exists(resolvedBundlerFolder) ){
                    if( verbose ){
                        getLog().info("Found additional bundler resources for bundler " + currentRunningBundlerID);
                    }
                    sourceFolder = resolvedBundlerFolder;
                    // change behaviour to have more control for all bundlers being inside JDK
                    switch(currentRunningBundlerID) {
                        case "exe":
                            File exeBundlerFolder = WinExeBundler.EXE_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = exeBundlerFolder.toPath();
                            break;
                        case "msi":
                            File msiBundlerFolder = WinMsiBundler.MSI_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = msiBundlerFolder.toPath();
                            break;
                        case "mac.appStore":
                            // custom mac bundler might be used
                            if( skipMacBundlerWorkaround ){
                                getLog().warn("The bundler with ID 'mac.appStore' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "dmg":
                            // custom mac bundler might be used
                            if( skipMacBundlerWorkaround ){
                                getLog().warn("The bundler with ID 'dmg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "pkg":
                            // custom mac bundler might be used
                            if( skipMacBundlerWorkaround ){
                                getLog().warn("The bundler with ID 'pkg' is not supported for using 'additionalBundlerResources', as that bundler does not provide any way to copy additional bundler-files.");
                            }
                            skipCopyAdditionalBundlerResources = true;
                            break;
                        case "deb":
                            File linuxDebBundlerFolder = LinuxDebBundler.DEB_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = linuxDebBundlerFolder.toPath();
                            break;
                        case "rpm":
                            File linuxRpmBundlerFolder = LinuxRpmBundler.RPM_IMAGE_DIR.fetchFrom(paramsToBundleWith);
                            targetFolder = linuxRpmBundlerFolder.toPath();
                            break;
                        default:
                            // we may have custom bundler ;)
                            getLog().warn("Unknown bundler-ID found, copying from root of additionalBundlerResources into IMAGES_ROOT.");
                            sourceFolder = additionalBundlerResources.toPath();
                            break;
                    }
                } else {
                    if( verbose ){
                        getLog().info("No additional bundler resources for bundler " + currentRunningBundlerID + " were found, copying all files instead.");
                    }
                }
                if( !skipCopyAdditionalBundlerResources ){
                    try{
                        if( verbose ){
                            getLog().info("Copying additional bundler resources into: " + targetFolder.toFile().getAbsolutePath());
                        }
                        copyRecursive(sourceFolder, targetFolder);
                    } catch(IOException e){
                        getLog().warn("Couldn't copy additional bundler resource-file(s).", e);
                    }
                }
            }
        }

        // check if we need to inform the user about low performance even on SSD
        // https://github.com/FibreFoX/javafx-gradle-plugin/issues/41
        if( System.getProperty("os.name").toLowerCase().startsWith("linux") && "deb".equals(currentRunningBundlerID) ){
            AtomicBoolean needsWarningAboutSlowPerformance = new AtomicBoolean(false);
            nativeOutputDir.toPath().getFileSystem().getFileStores().forEach(store -> {
                if( "ext4".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
                if( "btrfs".equals(store.type()) ){
                    needsWarningAboutSlowPerformance.set(true);
                }
            });
            if( needsWarningAboutSlowPerformance.get() ){
                getLog().info("This bundler might take some while longer than expected.");
                getLog().info("For details about this, please go to: https://wiki.debian.org/Teams/Dpkg/FAQ#Q:_Why_is_dpkg_so_slow_when_using_new_filesystems_such_as_btrfs_or_ext4.3F");
            }
        }
    }

    /*
     * Sometimes we need to work with some bundler, even if it wasn't requested. This happens when one bundler was selected and we need
     * to work with the outcome of some image-bundler (because that JDK-bundler is faulty).
     */
    private boolean shouldBundlerRun(String requestedBundler, String currentRunningBundlerID, Map<String, ? super Object> params) {
        if( requestedBundler != null && !"ALL".equalsIgnoreCase(requestedBundler) && !requestedBundler.equalsIgnoreCase(currentRunningBundlerID) ){
            // this is not the specified bundler
            return false;
        }

        if( skipJNLP && "jnlp".equalsIgnoreCase(currentRunningBundlerID) ){
            getLog().info("Skipped JNLP-bundling as requested.");
            return false;
        }

        boolean runBundler = true;
        // Workaround for native installer bundle not creating working executable native launcher
        // (this is a comeback of issue 124)
        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205
        // do run application bundler and put the cfg-file to application resources
        if( System.getProperty("os.name").toLowerCase().startsWith("linux") ){
            if( workarounds.isWorkaroundForBug205Needed() ){
                // check if special conditions for this are met (not jnlp, but not linux.app too, because another workaround already works)
                if( !"jnlp".equalsIgnoreCase(requestedBundler) && !"linux.app".equalsIgnoreCase(requestedBundler) && "linux.app".equalsIgnoreCase(currentRunningBundlerID) ){
                    if( !skipNativeLauncherWorkaround205 ){
                        getLog().info("Detected linux application bundler ('linux.app') needs to run before installer bundlers are executed.");
                        runBundler = true;
                        params.put(CFG_WORKAROUND_MARKER, "true");
                    } else {
                        getLog().info("Skipped workaround for native linux installer bundlers.");
                    }
                }
            }
        }
        return runBundler;
    }

    private void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }

    private void signJarFilesUsingBlobSigning() throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration();

        SignJarParams signJarParams = new SignJarParams();
        signJarParams.setVerbose(verbose);
        signJarParams.setKeyStore(keyStore);
        signJarParams.setAlias(keyStoreAlias);
        signJarParams.setStorePass(keyStorePassword);
        signJarParams.setKeyPass(keyPassword);
        signJarParams.setStoreType(keyStoreType);

        signJarParams.addResource(nativeOutputDir, jfxMainAppJarName);

        // add all gathered jar-files as resources so be signed
        workarounds.getJARFilesFromJNLPFiles().forEach(jarFile -> signJarParams.addResource(nativeOutputDir, jarFile));

        getLog().info("Signing JAR files for jnlp bundle using BLOB-method");
        getPackagerLib().signJar(signJarParams);
    }

    private void signJarFiles() throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration();

        AtomicReference<MojoExecutionException> exception = new AtomicReference<>();
        workarounds.getJARFilesFromJNLPFiles().stream().map(relativeJarFilePath -> new File(nativeOutputDir, relativeJarFilePath)).forEach(jarFile -> {
            try{
                // only sign when there wasn't already some problem
                if( exception.get() == null ){
                    signJar(jarFile.getAbsoluteFile());
                }
            } catch(MojoExecutionException ex){
                // rethrow later (same trick is done inside apache-tomee project ;D)
                exception.set(ex);
            }
        });
        if( exception.get() != null ){
            throw exception.get();
        }
    }

    private void checkSigningConfiguration() throws MojoFailureException {
        if( !keyStore.exists() ){
            throw new MojoFailureException("Keystore does not exist, use 'jfx:generate-key-store' command to make one (expected at: " + keyStore + ")");
        }

        if( keyStoreAlias == null || keyStoreAlias.isEmpty() ){
            throw new MojoFailureException("A 'keyStoreAlias' is required for signing JARs");
        }

        if( keyStorePassword == null || keyStorePassword.isEmpty() ){
            throw new MojoFailureException("A 'keyStorePassword' is required for signing JARs");
        }

        // fallback
        if( keyPassword == null ){
            keyPassword = keyStorePassword;
        }
    }

    private void signJar(File jarFile) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add(getEnvironmentRelativeExecutablePath() + "jarsigner");
        Optional.ofNullable(additionalJarsignerParameters).ifPresent(jarsignerParameters -> {
            command.addAll(jarsignerParameters);
        });
        command.add("-strict");
        command.add("-keystore");
        command.add(keyStore.getAbsolutePath());
        command.add("-storepass");
        command.add(keyStorePassword);
        command.add("-keypass");
        command.add(keyPassword);
        command.add(jarFile.getAbsolutePath());
        command.add(keyStoreAlias);

        if( verbose ){
            command.add("-verbose");
        }

        try{
            ProcessBuilder pb = new ProcessBuilder()
                    .inheritIO()
                    .directory(project.getBasedir())
                    .command(command);

            if( verbose ){
                getLog().info("Running command: " + String.join(" ", command));
            }

            getLog().info("Signing JAR files for jnlp bundle using jarsigner-method");
            Process p = pb.start();
            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new MojoExecutionException("Signing jar using jarsigner wasn't successful! Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new MojoExecutionException("There was an exception while signing jar-file: " + jarFile.getAbsolutePath(), ex);
        }
    }

    private boolean isClassInsideJarFile(String classname, File jarFile) {
        String requestedJarEntryName = classname.replace(".", "/") + ".class";
        try{
            JarFile jarFileToSearchIn = new JarFile(jarFile, false, JarFile.OPEN_READ);
            return jarFileToSearchIn.stream().parallel().filter(jarEntry -> {
                return jarEntry.getName().equals(requestedJarEntryName);
            }).findAny().isPresent();
        } catch(IOException ex){
            // NO-OP
        }
        return false;
    }
}
