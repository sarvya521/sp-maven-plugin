
package com.sp.maven.utils;

public class Constants {

    /** Snapshot. */
    public static final String SNAPSHOT = "-SNAPSHOT";
    /** Extension for POM backup. */
    public static final String BACKUP_EXT = "versionsBackup";
    /** ID separator. */
    public static final String ID_SEPARATOR = ":";
    /** ID for parent pom. */
    public final static String PARENT_POM_ID = "com.sp.maven:parent";
    /** Group ID for dependency management poms. */
    public final static String DEP_MGMT_GROUP_ID = "com.sp.maven.depmgmt";

    /**
     * Constructor.
     */
    private Constants() {
    }

}
