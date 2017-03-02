/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zenjava.javafx.maven.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The configuration of the NativeMojo.
 *
 * TODO: Consider to use a XML-, JSON-, or reflection based approach.
 *
 * @author puce
 *
 */
public class NativeMojoConfiguration {

    public void storeConfiguration(AbstractNativeMojo nativeMojo, Path nativeMojoConfigPath) throws IOException {
        Properties nativeMojoConfig = new Properties();

        storeConfiguration(nativeMojo, nativeMojoConfig);

        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(nativeMojoConfigPath))) {
            nativeMojoConfig.store(bos, "");
        }

    }

    public void storeConfiguration(AbstractNativeMojo nativeMojo, Properties nativeMojoConfig) throws IOException {

        nativeMojoConfig.setProperty("identifier", nativeMojo.identifier);

        nativeMojoConfig.setProperty("vendor", nativeMojo.vendor);

//        Map<String, String> jvmProperties;

//        List<String> jvmArgs;

//        Map<String, String> userJvmArgs;

//        List<String> launcherArguments;

        nativeMojoConfig.setProperty("nativeReleaseVersion", nativeMojo.nativeReleaseVersion);

        nativeMojoConfig.setProperty("needShortcut", String.valueOf(nativeMojo.needShortcut));

        nativeMojoConfig.setProperty("needMenu", String.valueOf(nativeMojo.needMenu));

//        Map<String, String> bundleArguments;

        nativeMojoConfig.setProperty("appName", nativeMojo.appName);

//        File additionalBundlerResources;

        nativeMojoConfig.setProperty("skipNativeLauncherWorkaround124", String.valueOf(nativeMojo.skipNativeLauncherWorkaround124));

//        List<NativeLauncher> secondaryLaunchers;

        nativeMojoConfig.setProperty("skipNativeLauncherWorkaround167", String.valueOf(nativeMojo.skipNativeLauncherWorkaround167));

        for (int i = 0; i < nativeMojo.fileAssociations.size(); i++) {
            nativeMojoConfig.setProperty("fileAssociations." + i + ".description", nativeMojo.fileAssociations.get(i).getDescription());
            nativeMojoConfig.setProperty("fileAssociations." + i + ".extensions", nativeMojo.fileAssociations.get(i).getExtensions());
            nativeMojoConfig.setProperty("fileAssociations." + i + ".contentType", nativeMojo.fileAssociations.get(i).getContentType());
            if (nativeMojo.fileAssociations.get(i).getIcon() != null) {
                nativeMojoConfig.setProperty("fileAssociations." + i + ".icon", nativeMojo.fileAssociations.get(i).getIcon().getName());
            }

        }

        nativeMojoConfig.setProperty("skipJNLPRessourcePathWorkaround182", String.valueOf(nativeMojo.skipJNLPRessourcePathWorkaround182));

        File keyStore;

        nativeMojoConfig.setProperty("keyStoreAlias", nativeMojo.keyStoreAlias);

        nativeMojoConfig.setProperty("keyStorePassword", nativeMojo.keyStorePassword);

        nativeMojoConfig.setProperty("keyPassword", nativeMojo.keyPassword);

        nativeMojoConfig.setProperty("keyStoreType", nativeMojo.keyStoreType);

        nativeMojoConfig.setProperty("skipSigningJarFilesJNLP185", String.valueOf(nativeMojo.skipSigningJarFilesJNLP185));

        nativeMojoConfig.setProperty("skipSizeRecalculationForJNLP185", String.valueOf(nativeMojo.skipSizeRecalculationForJNLP185));

        nativeMojoConfig.setProperty("noBlobSigning", String.valueOf(nativeMojo.noBlobSigning));

        List<String> customBundlers;

        nativeMojoConfig.setProperty("skipNativeLauncherWorkaround205", String.valueOf(nativeMojo.skipNativeLauncherWorkaround205));

        nativeMojoConfig.setProperty("skipMacBundlerWorkaround", String.valueOf(nativeMojo.skipMacBundlerWorkaround));

        nativeMojoConfig.setProperty("failOnError", String.valueOf(nativeMojo.failOnError));

        nativeMojoConfig.setProperty("onlyCustomBundlers", String.valueOf(nativeMojo.onlyCustomBundlers));

        nativeMojoConfig.setProperty("skipJNLP", String.valueOf(nativeMojo.skipJNLP));

        nativeMojoConfig.setProperty("skipNativeVersionNumberSanitizing", String.valueOf(nativeMojo.skipNativeVersionNumberSanitizing));

        List<String> additionalJarsignerParameters = new ArrayList<>();

        nativeMojoConfig.setProperty("skipMainClassScanning", String.valueOf(nativeMojo.skipMainClassScanning));

    }

    public void loadConfiguration(AbstractNativeMojo nativeMojo, Path nativeMojoConfigPath, Path fileAssociationsIconsDirPath) throws IOException {
        Properties nativeMojoConfig = new Properties();

        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(nativeMojoConfigPath))) {
            nativeMojoConfig.load(bis);
        }
        loadConfiguration(nativeMojo, nativeMojoConfig);
    }

    private void loadConfiguration(AbstractNativeMojo nativeMojo, Properties nativeMojoConfig) {
    }
}
