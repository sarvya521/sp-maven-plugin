
package com.sp.maven.baseline;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import com.sp.maven.utils.ChangeType;
import com.sp.maven.utils.Helper;
import com.sp.maven.utils.VersionHelper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This goal is based on the maven-bundle-plugin and should be used to ensure
 * that the module version is bumped correctly when a change occurs.
 *
 */
@Mojo(name = "baseline", threadSafe = true,
       requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.VERIFY)
public class BaselineMojo extends AbstractMojo {

    /** The serial version unique id. */
    private static final long serialVersionUID = 20170915151746L;

    @Component
    protected ArtifactResolver resolver;
    @Component
    private ArtifactMetadataSource metadataSource;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> remoteRepositories;
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File buildDirectory;
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true )
    private String finalName;

    /** Flag to easily skip execution. */
    @Parameter(property = "sp.baseline.skip", defaultValue = "false")
    protected boolean skip;
    /** Flag to trigger the version replacement. */
    @Parameter(property = "sp.baseline.replaceVersions", defaultValue = "false")
    protected boolean replaceVersions;
   /** Controls whether a backup pom should be created. */
    @Parameter(property = "generateBackupPoms", defaultValue = "true")
    private boolean generateBackupPoms;
    /** Group id to compare the current code against. */
    @Parameter(property = "sp.baseline.comparisonGroupId", defaultValue = "${project.groupId}")
    protected String comparisonGroupId;
    /** Artifact to compare the current code against. */
    @Parameter(property = "sp.baseline.comparisonArtifactId", defaultValue = "${project.artifactId}")
    protected String comparisonArtifactId;
    /** Version to compare the current code against. */
    @Parameter(property = "sp.baseline.comparisonVersion", defaultValue = "(,${project.version})")
    protected String comparisonVersion;
    /** Artifact to compare the current code against. */
    @Parameter(property = "sp.baseline.comparisonPackaging", defaultValue = "${project.packaging}")
    protected String comparisonPackaging;
    /** Classifier for the artifact to compare the current code against. */
    @Parameter(property = "sp.baseline.comparisonClassifier")
    protected String comparisonClassifier;
    /** Project types which this plugin supports. */
    @Parameter(property = "sp.baseline.supportedProjectTypes", defaultValue = "jar,bundle,nbm")
    protected List<String> supportedProjectTypes = Arrays.asList(new String[]{"jar", "bundle", "nbm"});

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // If the execution is skipped, we return immediatly.
        if (skip) {
            getLog().warn("Skipping Baseline execution" );
            return;
        }
        // If the project type is not supported, we return immediatly.
        Artifact artifact = project.getArtifact();
        String artifactType = artifact.getType();
        if (!supportedProjectTypes.contains(artifactType)) {
            getLog().warn("Skipping Baseline (project type " + artifactType + " not supported)");
            return;
        }

        // We try to retrieve the previous released version (SNAPSHOT are ignored).
        Artifact previousArtifact;
        try {
            previousArtifact = getPreviousArtifact(false);
        } catch (MojoExecutionException | MojoFailureException e) {
            previousArtifact = getPreviousArtifact(true);
        }
        // If there is not previous released version, we return immediatly.
        if (previousArtifact == null) {
            getLog().warn("Not generating Baseline report as there is no previous version of the library to compare against");
            return;
        }

        // Comparison can be done
        File previousFile = previousArtifact.getFile();
        File currentFile = new File(buildDirectory, getBundleName(false));
        if (!currentFile.exists()) {
            currentFile = new File(buildDirectory, getBundleName(true));
        }

        // Now we should have found the files. If they don't exist, we log a warning and stop
        if (!previousFile.exists()) {
            getLog().error("Skipping Baseline execution because the following file doesn't exist "
                    + previousFile.getAbsolutePath());
            return;
        }
        if (!currentFile.exists()) {
            getLog().error("Skipping Baseline execution because the following file doesn't exist "
                    + currentFile.getAbsolutePath());
            return;
        }

        getLog().info("Compare the version " + previousArtifact.getVersion() + " with the version " + project.getVersion());
        getLog().info("Old File : " + previousFile.getAbsolutePath());
        getLog().info("New File : " + currentFile.getAbsolutePath());

        Map<ChangeType, List<String>> detectedChanges = new HashMap<>();
        detectedChanges.put(ChangeType.MAJOR, new ArrayList<>());
        detectedChanges.put(ChangeType.MINOR, new ArrayList<>());
        detectedChanges.put(ChangeType.MICRO, new ArrayList<>());
        detectedChanges.put(ChangeType.NONE, new ArrayList<>());

        // Ensure that the jar files contain source code, else we skip the baselining.
        boolean previousHasSourceCode = ApiExtractor.hasSourceCode(previousFile);
        boolean currentHasSourceCode = ApiExtractor.hasSourceCode(currentFile);

        if (previousHasSourceCode && currentHasSourceCode) {
            // Rely on BND o compute the differences
            Set<Baseline.Info> bndDifferences = compareJarFile(previousFile, currentFile);
            // Extract the public packages
            List<String> previousPublicPackages = getPublicPackages(previousFile.getAbsolutePath());
            getLog().debug("Previously exposed packages:");
            previousPublicPackages.forEach((previousPublicPackage) -> {
                getLog().debug("\t" + previousPublicPackage);
            });
            List<String> currentPublicPackages = getPublicPackages(currentFile.getAbsolutePath());
            getLog().debug("Currently exposed packages:");
            currentPublicPackages.forEach((currentPublicPackage) -> {
                getLog().debug("\t" + currentPublicPackage);
            });
            // Check if we expose less packages
            for (String previousPublicPackage : previousPublicPackages) {
                if (!currentPublicPackages.contains(previousPublicPackage)) {
                    detectedChanges.get(ChangeType.MAJOR).add("The package " + previousPublicPackage + " is no more exposed");
                }
            }
            // Check if we expose more packages
            for (String currentPublicPackage : currentPublicPackages) {
                if (!previousPublicPackages.contains(currentPublicPackage)) {
                    detectedChanges.get(ChangeType.MINOR).add("The package " + currentPublicPackage + " is now exposed");
                }
            }
            // Check all the BND differences which occurred on public packages
            for (Baseline.Info info : bndDifferences) {
                String packageName = info.packageName;
                if (currentPublicPackages.contains(packageName + ".*")) {
                    Diff packageDiff = info.packageDiff;
                    StringBuilder sb = new StringBuilder();
                    getDiffMessages(packageDiff, sb, "");
                    Delta delta = packageDiff.getDelta();
                    switch (delta) {
                        case MAJOR :
                            detectedChanges.get(ChangeType.MAJOR).add(sb.toString());
                            break;
                        case MINOR :
                            detectedChanges.get(ChangeType.MINOR).add(sb.toString());
                            break;
                        case MICRO :
                            detectedChanges.get(ChangeType.MICRO).add(sb.toString());
                            break;
                    }
                }
            }
        } else {
            getLog().warn("No source code found in the files.");
        }

        // Compute the main change type
        ChangeType changeType;
        if (!detectedChanges.get(ChangeType.MAJOR).isEmpty()) {
            changeType = ChangeType.MAJOR;
        } else if (!detectedChanges.get(ChangeType.MINOR).isEmpty()) {
            changeType = ChangeType.MINOR;
        } else {
            changeType = ChangeType.MICRO;
        }
        logDetectedChanges(changeType, detectedChanges.get(changeType));
        // Check if the version has been correctly bumped
        String version = project.getVersion();
        String expectedVersion = VersionHelper.bumpVersion(previousArtifact.getVersion(), changeType);
        String expectedVersionSnap = expectedVersion + "-SNAPSHOT";
        if (!version.equals(expectedVersion) && !version.equals(expectedVersionSnap)) {
            if (replaceVersions) {
                Model model = project.getOriginalModel();
                model.setVersion(expectedVersionSnap);
                Helper.savePomFile(project, model, generateBackupPoms, getLog());
                getLog().info("Version was incorrect and has been changed from :" + version + " to: " + expectedVersionSnap);
            } else {
                StringBuilder msg = new StringBuilder();
                msg.append("Module version is not appropriate.");
                msg.append("\nDefined version: \t").append(project.getVersion());
                msg.append("\nExpected version: \t").append(expectedVersion).append(" or ").append(expectedVersionSnap);
                List<String> details = detectedChanges.get(changeType);
                for (String detail : details) {
                    msg.append("\n").append(detail);
                }
                throw new MojoFailureException(msg.toString());
            }
        } else {
            getLog().info("Version is correct.");
        }
    }

    private void logDetectedChanges(final ChangeType changeType, final List<String> details) {
        if (!details.isEmpty()) {
            getLog().debug("The following " + changeType + " have been detected:");
            for (String detail : details) {
                getLog().debug("\t" + detail);
            }
        }
    }

    private void getDiffMessages(final Diff diff, final StringBuilder sb, final String indent) {
        switch (diff.getDelta()) {
            case MAJOR :
                sb.append(indent);
                sb.append("A major change occurs in the ");
                sb.append(diff.getNewer().getType().toString().toLowerCase());
                sb.append(" ");
                sb.append(diff.getNewer().getName());
                sb.append("\n");
                break;
            case MINOR :
                sb.append(indent);
                sb.append("A minor change occurs in the ");
                sb.append(diff.getNewer().getType().toString().toLowerCase());
                sb.append(" ");
                sb.append(diff.getNewer().getName());
                sb.append("\n");
                break;
            case MICRO :
                sb.append(indent);
                sb.append("A micro change occurs in the ");
                sb.append(diff.getNewer().getType().toString().toLowerCase());
                sb.append(" ");
                sb.append(diff.getNewer().getName());
                sb.append("\n");
                break;


            case ADDED :
                sb.append(indent);
                sb.append("+ ");
                sb.append(diff.getNewer().getType().toString().toLowerCase());
                sb.append(" ");
                sb.append(diff.getNewer().getName());
                sb.append("\n");
                break;
            case REMOVED :
                sb.append(indent);
                sb.append("- ");
                sb.append(diff.getOlder().getType().toString().toLowerCase());
                sb.append(" ");
                sb.append(diff.getOlder().getName());
                sb.append("\n");
                break;
        }
        for (Diff childDiff : diff.getChildren()) {
            getDiffMessages(childDiff, sb, indent + "  ");
        }
    }

    private Artifact getPreviousArtifact(final boolean retry) throws MojoFailureException, MojoExecutionException {
        // Find the previous version JAR and resolve it, and it's dependencies
        final VersionRange range;
        try {
            range = VersionRange.createFromVersionSpec(comparisonVersion);
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoFailureException("Invalid comparison version: " + e.getMessage());
        }

        DefaultArtifactCoordinate dac = new DefaultArtifactCoordinate();
        dac.setGroupId(comparisonGroupId);
        dac.setArtifactId(comparisonArtifactId);
        if (comparisonClassifier != null) {
            dac.setClassifier(comparisonClassifier);
        } else if (retry && "bundle".equals(project.getArtifact().getType())) {
            dac.setClassifier(project.getArtifact().getType());
        }
        try {
            dac.setExtension(project.getArtifact().getArtifactHandler().getExtension());
        } catch (Throwable e) {
            dac.setExtension(project.getArtifact().getType());
        }
        try {
            List<ArtifactVersion> availableVersions = metadataSource.retrieveAvailableVersions(
                    project.getArtifact(), session.getLocalRepository(), project.getRemoteArtifactRepositories());
            filterSnapshots(availableVersions);
            ArtifactVersion version = range.matchVersion(availableVersions);
            if (version != null) {
                dac.setVersion(version.toString());
            }
        } catch (ArtifactMetadataRetrievalException amre) {
            throw new MojoExecutionException("Error determining previous version: " + amre.getMessage(), amre);
        }

        // If there is no version, then we don't try to retrieve the artifact and return directly.
        if (dac.getVersion() == null) {
            return null;
        } else {
            final Artifact previousArtifact;
            try {
                ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(this.session.getProjectBuildingRequest());
                buildingRequest.setRemoteRepositories(this.remoteRepositories);
                previousArtifact = this.resolver.resolveArtifact(buildingRequest, dac).getArtifact();
            } catch (ArtifactResolverException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
            return previousArtifact;
        }
    }

    private void filterSnapshots(final Collection<ArtifactVersion> versions) {
        versions.removeIf(av -> av.getQualifier() != null && av.getQualifier().endsWith("SNAPSHOT"));
    }

    private String getBundleName(final boolean retry) {
        String extension;
        try {
            extension = project.getArtifact().getArtifactHandler().getExtension();
        } catch (Throwable e) {
            extension = project.getArtifact().getType();
        }

        if (StringUtils.isEmpty(extension) || "bundle".equals(extension) || "pom".equals(extension) || "nbm".equals(extension)) {
            extension = "jar"; // just in case maven gets confused
        }

        String classifier = this.comparisonClassifier != null ? this.comparisonClassifier : project.getArtifact().getClassifier();
        if (null != classifier && classifier.trim().length() > 0) {
            return finalName + '-' + classifier + '.' + extension;
        }

        // Fix a bug in the maven-baseline-plugin when the artifact produces a xxx-bundle.jar but not xxx.jar
        if (retry && "bundle".equals(project.getArtifact().getType())) {
            return finalName + '-' + project.getArtifact().getType() + '.' + extension;
        }

        return finalName + '.' + extension;
    }

    public Set<Baseline.Info> compareJarFile(final File baselineFile, final File newFile) throws MojoExecutionException {
        try {
            Processor processor = new Processor();
            DiffPluginImpl differ = new DiffPluginImpl();
            Baseline baseline = new Baseline(processor, differ);
            try (Jar older = new Jar(baselineFile);
                    Jar newer = new Jar(newFile);) {
                Set<Baseline.Info> infoSet = baseline.baseline(newer, older, null);
                return infoSet;
            }
        }   catch (Exception ex) {
            throw new MojoExecutionException("An error occurred trying to compare the files.", ex);
        }
    }

    /**
     * This method access the JAR File based on the given path and returns the list of exposed packages.
     * @param jarFilePath The JAR file to analyze.
     * @return The list of exposed packages.
     * @throws MojoExecutionException If an error occurs during the scan.
     */
    private List<String> getPublicPackages(final String jarFilePath) throws MojoExecutionException {
        try {
            JarFile jarFile = new JarFile(jarFilePath);
            List<String> publicPackages = ApiExtractor.getPublicPackages(jarFile);
            return publicPackages;
        } catch (IOException ioe) {
            throw new MojoExecutionException("The file can't be accessed : " + jarFilePath, ioe);
        }
    }

}
