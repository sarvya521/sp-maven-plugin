
package com.sp.maven.versions;

import com.sp.maven.utils.ChangeType;
import com.sp.maven.utils.Constants;
import com.sp.maven.utils.Helper;
import com.sp.maven.utils.VersionHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This goal is used to do a major-SNAPSHOT bump of the specified project if required and a micro-SNAPSHOT bump
 * of upstream dependencies if required. It also ensures that the modified poms will be using the latest parent version
 * and that the new versions are synchronized in the dependency management pom.
 */
@Mojo(  name = "major-bump-helper",
        aggregator = true,
        defaultPhase = LifecyclePhase.VALIDATE)
public class MajorBumpHelperMojo extends AbstractMojo {

    /** Controls whether a backup pom should be created. */
    @Parameter(property = "generateBackupPoms", defaultValue = "true")
    private boolean generateBackupPoms;

    /** gives the input param. */
    @Parameter(property = "groupId", required = true)
    private String targetGroupId;
    /** gives the input param. */
    @Parameter(property = "artifactId", required = true)
    private String targetArtifactId;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    private MavenProject parentPom = null;
    private MavenProject majorPom = null;

    private String majorPomId;

    private final static String LOG_LINE = "-----------------------------------";

    private void logEphasizedInfoMessage(final String mess) {
        getLog().info(LOG_LINE);
        getLog().info(mess);
        getLog().info(LOG_LINE);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        majorPomId = targetGroupId + ":" + targetArtifactId;
        logEphasizedInfoMessage("Starting dependency check....");

        List<MavenProject> allProjects = session.getAllProjects();
        List<MavenProject> impactedPoms = allProjects.stream()
                .filter(mvnPom -> {
                    init(mvnPom);
                    for (Dependency d : mvnPom.getDependencies()) {
                        String groupId = d.getGroupId();
                        String artifactId = d.getArtifactId();
                        String identifier = groupId + ":" + artifactId;
                        if (identifier.equals(majorPomId)) {
                            getLog().info("\tPom with dependency found: " + mvnPom.getFile().getPath());
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

        logEphasizedInfoMessage("Dependency check complete");

        //Extract the dependency management modules
        List<MavenProject> depMgmtProjects = Helper.getDependencyManagementProjects(allProjects);

        //Check init
        if (depMgmtProjects.isEmpty()) {
            throw new MojoExecutionException("Can't find any dep mgmt pom");
        } else {
            getLog().debug(depMgmtProjects.size() + " dependency management projects detected.");
        }

        final String parentVersion = parentPom.getVersion();
        getLog().info("Will ensure each bumped pom will use the parent in version: " + parentVersion);
        logEphasizedInfoMessage("Starting dependent modules bump...");
        //This map will hold all the version bumped during the process
        Map<String, String> updatedVersions = new HashMap<>();
        //Now check the poms that need bumping
        for (MavenProject pom : impactedPoms) {
            //Only bump pom that were not bumped previously
            if (!pom.getVersion().endsWith(Constants.SNAPSHOT)) {
                //Do micro bump
                String newVersion = updatePomVersion(pom);

                //Make sure it uses last defined parent
                Model originalModel = pom.getOriginalModel();
                Parent parentModel = originalModel.getParent();
                parentModel.setVersion(parentVersion);
                originalModel.setParent(parentModel);

                //Persist pom that has been bumped
                Helper.savePomFile(pom, pom.getOriginalModel(), generateBackupPoms, getLog());

                //Register version change
                String groupId = pom.getGroupId();
                String artifactId = pom.getArtifactId();
                String identifier = groupId + ":" + artifactId;
                updatedVersions.put(identifier, newVersion);

                getLog().info("Done:\t" + identifier + " bumped to: " + newVersion);
            }
        }
        logEphasizedInfoMessage("Dependent modules bump complete.");

        logEphasizedInfoMessage("Finalizing major bump updating depmgmt...");

        //Do major bump if required.
        Model majorPomModel = majorPom.getOriginalModel();
        String majorPomVersion = majorPomModel.getVersion();
        if (!majorPomVersion.endsWith(".0.0-SNAPSHOT")) {
            String newMajorVersion =
                    VersionHelper.bumpVersion(majorPomVersion, ChangeType.MAJOR) + Constants.SNAPSHOT;
            majorPomModel.setVersion(newMajorVersion);
            updatedVersions.put(majorPomId, newMajorVersion);
            //Make sure it uses last defined parent
            majorPomModel.getParent().setVersion(parentVersion);

            //Persist major bump
            Helper.savePomFile(majorPom, majorPomModel, generateBackupPoms, getLog());
        } //else it was alredy major bumped since last release, no need to do it again


        //Start process of updating dependency managers:
        for (MavenProject projToUpdate : depMgmtProjects) {
            updateDepMgmt(projToUpdate, updatedVersions);
        }
        logEphasizedInfoMessage("Process complete.");
    }

    private void updateDepMgmt(final MavenProject projToUpdate, final Map<String, String> updatedVersions)
            throws MojoExecutionException {
        Model mavenModel = projToUpdate.getOriginalModel();
        DependencyManagement dependencyManagement = mavenModel.getDependencyManagement();
        List<Dependency> dependencies = dependencyManagement.getDependencies();
        dependencies.forEach(d -> {
            String groupId = d.getGroupId();
            String artifactId = d.getArtifactId();
            String identifier = groupId + ":" + artifactId;
            String newVersion  = updatedVersions.get(identifier);
            if (newVersion != null) {
                d.setVersion(newVersion);
            }
        });
        //Persist project change
        Helper.savePomFile(projToUpdate, mavenModel, generateBackupPoms, getLog());
    }

    private String updatePomVersion(final MavenProject project) throws MojoExecutionException {
        // Compute the new version
        Model originalModel = project.getOriginalModel();
        String originalVersion = originalModel.getVersion();
        String newVersion = VersionHelper.bumpVersion(originalVersion, ChangeType.MICRO) + Constants.SNAPSHOT;
        // Apply the new version and save the POM file
        originalModel.setVersion(newVersion);
        return newVersion;
    }

    private void init(final MavenProject mvnPom) {
        final String pomId = Helper.getId(mvnPom.getOriginalModel());
        if (parentPom == null && pomId.equals(Constants.PARENT_POM_ID)) {
            parentPom = mvnPom;
        }
        if (majorPom == null && pomId.equals(majorPomId)) {
            majorPom = mvnPom;
        }
    }

}
