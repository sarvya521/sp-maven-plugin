
package com.sp.maven.versions;

import com.sp.maven.utils.ChangeType;
import com.sp.maven.utils.Helper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This goal is used to bump the version of the POM file based on the given parameters.
 *
 */
@Mojo(name = "bump-version")                                                                                  // NOI18N
public class BumpVersionMojo extends AbstractMojo {

    /** The serial version unique id. */
    private static final long serialVersionUID = 20170915151746L;
    /** The Maven project. */
    @Component
    private MavenProject project;
    /** Controls whether a backup pom should be created. */
    @Parameter(property = "generateBackupPoms", defaultValue = "true")                                        // NOI18N
    private boolean generateBackupPoms;
    /** Define the kind of bump (MAJOR, MINOR, MICRO, NONE). */
    @Parameter(property = "bumpType", required = true)
    private String bumpType;
    /** Define the kind of bump (MAJOR, MINOR, MICRO). */
    @Parameter(property = "bumpToSnapshot", required = true)                                                  // NOI18N
    private boolean bumpToSnapshot;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Helper.bumpWithParams(
                project, ChangeType.valueOf(bumpType), bumpToSnapshot, generateBackupPoms);
        // Generate the backup and save
        Helper.savePomFile(project, project.getOriginalModel(), generateBackupPoms, getLog());
    }

}
