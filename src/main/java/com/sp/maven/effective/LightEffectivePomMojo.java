
package com.sp.maven.effective;

import com.sp.maven.utils.Helper;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This goal should be used to generate a light effective POM file.
 * The main difference with the versions-maven-plugin is that only the dependencyManagement section is included.
 * This should be used to backup the assembly pom files with all resolved dependency information needed to replay the build.
 *
 * 2017-12-06 09:33:32
 */
@Mojo(name = "light-effective", aggregator = true)
public class LightEffectivePomMojo extends AbstractMojo {

    /** The serial version unique id. */
    private static final long serialVersionUID = 20171206093332L;

    /** The Maven project. */
    @Component
    private MavenProject project;

    /** Controls whether a backup pom should be created. */
    @Parameter(property = "generateBackupPoms", defaultValue = "true")
    private boolean generateBackupPoms;

    /** Whether to process the dependencies section of the project. */
    @Parameter(property = "processDependencies", defaultValue = "false")
    protected boolean processDependencies;

    /** Whether to process the dependencyManagement section of the project. */
    @Parameter(property = "processDependencyManagement", defaultValue = "true")
    protected boolean processDependencyManagement;

    /** Whether to process the build/plugins section of the project. */
    @Parameter(property = "processPlugins", defaultValue = "false")
    protected boolean processPlugins;

    /** Whether to process the build/pluginManagement section of the project. */
    @Parameter(property = "processPluginManagement", defaultValue = "true")
    protected boolean processPluginManagement;

    /** Whether to process the properties section of the project. */
    @Parameter(property = "processProperties", defaultValue = "false")
    protected boolean processProperties;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // The full effective model
        Model fullEffectivePom = project.getModel();
        // The original model
        Model originalPom = project.getOriginalModel();
        // Merge the effective pom and the original one according to the options
        if (processDependencyManagement) {
            originalPom.setDependencyManagement(fullEffectivePom.getDependencyManagement());
        }
        if (processDependencies) {
            originalPom.setDependencies(fullEffectivePom.getDependencies());
        }
        if (processPluginManagement && fullEffectivePom.getBuild() != null) {
            Build originalBuild = originalPom.getBuild();
            if (originalBuild == null) {
                originalBuild = new Build();
                originalPom.setBuild(originalBuild);
            }
            originalBuild.setPluginManagement(fullEffectivePom.getBuild().getPluginManagement());
        }
        if (processPlugins && fullEffectivePom.getBuild() != null) {
            Build originalBuild = originalPom.getBuild();
            if (originalBuild == null) {
                originalBuild = new Build();
                originalPom.setBuild(originalBuild);
            }
            originalPom.getBuild().setPlugins(fullEffectivePom.getBuild().getPlugins());
        }
        if (processProperties) {
            originalPom.setProperties(fullEffectivePom.getProperties());
        }
        // Filter the dependencyManagement to keep only the dependencies effectively used
        filterDependencyManagement(originalPom);
        // Filter the pluginManagement to keep only the plugin effectively used
        filterPluginManagement(originalPom);
        // Generate the backup and save
        Helper.savePomFile(project, originalPom, generateBackupPoms, getLog());
    }

    /**
     * Filter the dependencyManagement to keep only the dependencies effectively used
     * @param model Model to filter.
     */
    private void filterDependencyManagement(final Model model) {
        DependencyManagement dependencyManagement = model.getDependencyManagement();
        if (dependencyManagement != null) {
            List<Dependency> dependencies = model.getDependencies();
            if (dependencies.isEmpty()) {
                getLog().warn("There is no dependency defined in this POM. Filtering is skipped as it seems to be a depmgmt.");
            } else {
                List<String> dependenciesIds = new ArrayList<>();
                dependencies.forEach((d) -> {
                    String id = d.getGroupId() + ":" + d.getArtifactId();
                    dependenciesIds.add(id);
                });
                List<Dependency> depMgmtDependencies = dependencyManagement.getDependencies();
                List<Dependency> toRemove = new ArrayList<>();
                depMgmtDependencies.forEach((d) -> {
                    String id = d.getGroupId() + ":" + d.getArtifactId();
                    if (!dependenciesIds.contains(id)) {
                        toRemove.add(d);
                    }
                });
                depMgmtDependencies.removeAll(toRemove);
            }
        }
    }

    /**
     * Filter the pluginManagement to keep only the plugin effectively used
     * @param model Model to filter.
     */
    private void filterPluginManagement(final Model model) {
        Build build = model.getBuild();
        PluginManagement pluginManagement = build.getPluginManagement();
        if (pluginManagement != null) {
            List<Plugin> plugins = build.getPlugins();
            if (plugins.isEmpty()) {
                getLog().warn("There is no plugins defined in this POM. Filtering is skipped as it seems to be an aggregator.");
            } else {
                List<String> pluginsIds = new ArrayList<>();
                plugins.forEach((p) -> {
                    String id = p.getGroupId() + ":" + p.getArtifactId();
                    pluginsIds.add(id);
                });
                List<Plugin> pluginMgmtPlugins = pluginManagement.getPlugins();
                List<Plugin> toRemove = new ArrayList<>();
                pluginMgmtPlugins.forEach((p) -> {
                    String id = p.getGroupId() + ":" + p.getArtifactId();
                    if (!pluginsIds.contains(id)) {
                        toRemove.add(p);
                    }
                });
                pluginMgmtPlugins.removeAll(toRemove);
            }
        }
    }

}
