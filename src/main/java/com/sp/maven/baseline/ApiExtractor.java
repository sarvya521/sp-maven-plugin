package com.sp.maven.baseline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * ApiExtractor.
 */
public class ApiExtractor {

    /** The serial version unique id. */
    private static final long serialVersionUID = 20170803060827L;
    /** Extension for .class files. */
    private static final String CLASS_EXTENSION = ".class";
    /** Netbeans tag for exposed packages. */
    private static final String TAG_EXPOSED_PACKAGES_NETBEANS = "OpenIDE-Module-Public-Packages";
    /** OSGi tag for exposed packages. */
    private static final String TAG_EXPOSED_PACKAGES_OSGI = "Export-Package";
    /** OSGi tag. */
    private static final String TAG_OSGI_BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";

    /**
     * Constructor.
     */
    private ApiExtractor() {
    }

    /**
     * Check if the jar file contains source code.
     * @param file The jar file to browse.
     * @return True if the jar file contain a .class entry, else return false.
     * @throws MojoExecutionException if the file can't be accessed.
     */
    public static boolean hasSourceCode(final File file) throws MojoExecutionException {
        try {
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(CLASS_EXTENSION)) {
                    return true;
                }
            }
            return false;
        } catch (IOException ioe) {
            throw new MojoExecutionException("The file can't be accessed : " + file.getAbsolutePath(), ioe);
        }
    }

    /**
     * Get the list of all the classes contain in the given JAR file.
     * @param jarFile JAR file to analyse.
     * @return The list of all the classes contain in the given JAR file.
     */
    private static List<String> getAllClasses(final JarFile jarFile) {
        List<String> classNames = new ArrayList<>();
        jarFile.stream().forEach((JarEntry entry) -> {
            if (!entry.isDirectory() && entry.getName().endsWith(CLASS_EXTENSION)) {
                String fullClassName = entry.getName().replace('/', '.'); // including ".class"
                classNames.add(fullClassName.substring(0, fullClassName.length() - CLASS_EXTENSION.length()));
            }
        });
        return classNames;
    }

    /**
     * Get the list of exposed packages for the given JAR file.
     * @param jarFile JAR file to analyse.
     * @return The list of exposed packages for the given JAR file.
     * @throws MojoExecutionException If an error occurs retrieving the MANIFEST.
     */
    public static List<String> getPublicPackages(final JarFile jarFile) throws MojoExecutionException {
        try {
            List<String> result = new ArrayList<>();
            Manifest manifest = jarFile.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String[] arr;
            // If the manifest contains OSGI tag
            if (attributes.containsKey(new Attributes.Name(TAG_OSGI_BUNDLE_SYMBOLICNAME))) {
                if (attributes.containsKey(new Attributes.Name(TAG_EXPOSED_PACKAGES_OSGI))) {
                    arr = attributes.getValue(TAG_EXPOSED_PACKAGES_OSGI).split("\",");
                    for (int i = 0; i < arr.length; i++) {
                        String packageWithVersionAndUsage = arr[i];
                        int indexOf = packageWithVersionAndUsage.indexOf(';');
                        if (indexOf != -1) {
                            arr[i] = packageWithVersionAndUsage.substring(0, indexOf) + ".*";
                        }
                    }
                } else {
                    arr = new String[0];
                }
            } else if (attributes.containsKey(new Attributes.Name(TAG_EXPOSED_PACKAGES_NETBEANS))) {
                arr = attributes.getValue(TAG_EXPOSED_PACKAGES_NETBEANS).split(", ");
            } else {
                final Set<String> packages = new HashSet<>();
                List<String> allClasses = getAllClasses(jarFile);
                allClasses.stream().forEach(fullClassName -> {
                    if (!fullClassName.contains(".impl.") && !fullClassName.contains(".internal.")) {
                        String packageName = fullClassName.substring(0, fullClassName.lastIndexOf('.'));
                        packages.add(packageName);
                    }
                });
                arr = packages.toArray(new String[packages.size()]);
            }
            result.addAll(Arrays.asList(arr));
            return result;
        } catch (IOException ioe) {
            throw new MojoExecutionException("The Manifest can't be accessed.", ioe);
        }
    }

}
