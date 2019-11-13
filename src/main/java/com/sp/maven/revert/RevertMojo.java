
package com.sp.maven.revert;

import com.sp.maven.utils.Constants;
import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * This goal is used to revert the changes made on the POM file and restore the backup which has been made previously.
 *
 * 2017-11-27 07:08:41
 */
@Mojo(name="revert", requiresProject=true, requiresDirectInvocation=true)                                     // NOI18N
public class RevertMojo extends AbstractMojo {

    /** The serial version unique id. */
    private static final long serialVersionUID = 20171127070841L;
    @Parameter(defaultValue = "${project}", required = true, readonly = true)                                 // NOI18N
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File outFile = this.project.getFile();
        File backupFile = new File(outFile.getParentFile(), outFile.getName() + "." + Constants.BACKUP_EXT);  // NOI18N
        if (backupFile.exists()) {
            getLog().info("Restoring " + outFile + " from " + backupFile);                                    // NOI18N
            try {
                FileUtils.copyFile(backupFile, outFile);
                FileUtils.forceDelete(backupFile);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

}
