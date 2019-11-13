
package com.sp.maven.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Helper.
 *
 * 2017-12-06 10:18:39
 */
public class Helper {

    /** The serial version unique id. */
    private static final long serialVersionUID = 20171206101839L;

    /**
     * Constructor.
     */
    private Helper() {
    }

    /**
     * Generate the backup (if needed) and save the given model in the pom.xml.
     * @param project Maven Project for which we want to write the pom.xml
     * @param mavenModel The Maven Model to apply.
     * @param generateBackup True to generate a backup before saving the pom.xml.
     * @param logger The logger to use for messages.
     * @throws MojoExecutionException If an error occurs during the process.
     */
    public static void savePomFile(final MavenProject project,
            final Model mavenModel, final boolean generateBackup, final Log logger) throws MojoExecutionException {
        generateBackupPoms(project, generateBackup, logger);
        Path outputFilePath = project.getFile().toPath();
        saveMavenModel(outputFilePath, mavenModel, logger);
    }

    private static void generateBackupPoms(final MavenProject project,
            final boolean generateBackup, final Log logger) throws MojoExecutionException {
        try {
            if (generateBackup) {
                File pomFile = project.getFile();
                File backupFile = new File(pomFile.getParentFile(), pomFile.getName() + "." + Constants.BACKUP_EXT);
                if (!backupFile.exists()) {
                    logger.debug("Backing up " + pomFile + " to " + backupFile);                              // NOI18N
                    FileUtils.copyFile(pomFile, backupFile);
                } else {
                    logger.debug("Leaving existing backup " + backupFile + " unmodified");                    // NOI18N
                }
            } else {
                logger.debug("Skipping generation of backup file");                                           // NOI18N
            }
        } catch (IOException e) {
            throw new MojoExecutionException("An error occurred during the backup of the POM file : " + e.getMessage(), e);
        }
    }

    private static void saveMavenModel(final Path pomFilePath,
            final Model mavenModel, final Log logger) throws MojoExecutionException {
        logger.debug("Save the POM file : " + pomFilePath);                                                   // NOI18N
        try {
            if (!Files.exists(pomFilePath)) {
                Files.createDirectories(pomFilePath.getParent());
                Files.createFile(pomFilePath);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("An error occurred saving " + pomFilePath, ex);                  // NOI18N
        }
        try (FileOutputStream fos = new FileOutputStream(pomFilePath.toFile())) {
            MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
            mavenWriter.write(fos, mavenModel);
        } catch (IOException ex) {
            throw new MojoExecutionException("An error occurred saving " + pomFilePath, ex);                  // NOI18N
        }
    }



    /**
     * Load the Maven Model associated to the given Path.
     * @param pomFilePath Path to the POM file.
     * @return the Maven Model associated to the given Path.
     * @throws Exception If an error occurred retrieving the Maven Model.
     */
    public static Model loadMavenModel(final Path pomFilePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(pomFilePath.toFile());
            Reader reader = new InputStreamReader(fis)) {
            MavenXpp3Reader mavenreader = new MavenXpp3Reader();
            Model mavenModel = mavenreader.read(reader);
            mavenModel.setPomFile(pomFilePath.toFile());
            return mavenModel;
        } catch (IOException | XmlPullParserException ex) {
            throw new Exception("An error occurred loading " + pomFilePath, ex);                              // NOI18N
        }
    }

    /**
     * Returns groupId:artifactId
     * Warning: if the supplied model is not the effective one, groupId may be null & the method will throw NPE.
     * @param mavenModel the maven pom used to get the id.
     * @return String groupId:artifactId
     */
    public static String getId(final Model mavenModel) {
        return mavenModel.getGroupId()
                        + Constants.ID_SEPARATOR
                        + mavenModel.getArtifactId();
    }

    /**
     * Bump the project & saves it using the supplied parameters
     * @param project project to bump
     * @param bumpType bump type
     * @param bumpToSnapshot true if needs to bump to snapshot
     * @param generateBackupPoms if backup required
     * @return the new version
     * @throws MojoExecutionException
     */
    public static String bumpWithParams(final MavenProject project,
                                      final ChangeType bumpType,
                                      final boolean bumpToSnapshot,
                                      final boolean generateBackupPoms) throws MojoExecutionException {
        Model originalModel = project.getOriginalModel();
        String originalVersion = originalModel.getVersion();
        if (originalVersion == null) {
            throw new MojoExecutionException("Project version is inherited from parent.");                   // NOI18N
        }
        // Compute the new version
        String newVersion = VersionHelper.bumpVersion(originalVersion, bumpType);
        if (bumpToSnapshot) {
            newVersion += Constants.SNAPSHOT;
        }
        // Apply the new version and save the POM file
        originalModel.setVersion(newVersion);
        return newVersion;
    }

    /**
     * Bump the project & saves it using the supplied parameters
     * @param project project to bump
     * @param replaceExisting bump type
     * @param addSnapShot true if needs to bump to snapshot
     * @param suffix add suffix to version if required
     * @param generateBackupPoms if backup required
     * @return the new version with qualifier add or replaced
     * @throws MojoExecutionException
     */
    public static String addQualifier(final MavenProject project,
                                      final boolean replaceExisting,
                                      final boolean addSnapShot,
                                      final String suffix,
                                      final boolean generateBackupPoms) throws MojoExecutionException {
        Model originalModel = project.getOriginalModel();
        String originalVersion = originalModel.getVersion();
        if (originalVersion == null) {
            throw new MojoExecutionException("Project version is inherited from parent.");                   // NOI18N
        } else {
            //Remove -SNAPSHOT from the original version to prevent results like: 1.2.7-SNAPSHOT-19R1-beta-SNAPSHOT
            originalVersion = originalVersion.replace(Constants.SNAPSHOT, "");
        }
        // Compute the new version
        String newVersion;
        int indexOf = originalVersion.indexOf("-");
        if (replaceExisting && indexOf > 0) {
            newVersion = originalVersion.substring(0, indexOf) + suffix;
        } else {
            newVersion = originalVersion + suffix;
        }

        if (addSnapShot) {
            newVersion += Constants.SNAPSHOT;
        }

        // Apply the new version and save the POM file
        originalModel.setVersion(newVersion);
        return newVersion;
    }

    /**
     * Scans the supplied collections and extracts the dependency management projects.
     * @param allProjects projects to scan
     * @return the dependency management projects
     */
    public static List<MavenProject> getDependencyManagementProjects(Collection<MavenProject> allProjects) {
        return allProjects.stream().
                filter(mvnProj -> {
                    return mvnProj.getGroupId().equals(Constants.DEP_MGMT_GROUP_ID);
                })
                .collect(Collectors.toList());
    }
}
