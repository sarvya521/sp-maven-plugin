
package com.sp.maven.utils;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * VersionHelper.
 *
 */
public class VersionHelper {

    /** The serial version unique id. */
    private static final long serialVersionUID = 20170918160348L;

    /**
     * Constructor.
     */
    private VersionHelper() {
    }

    /**
     * This method parse the given version and bump to the next one according to the given changeType.
     * @param version The current version.
     * @param changeType The change type (Major, Minor, Micro).
     * @return The next version according to the given changeType.
     */
    public static String bumpVersion(final String version, final ChangeType changeType) {
        // Split the version to Major . Minor . Micro
        DefaultArtifactVersion dav = new DefaultArtifactVersion(version);
        int majorVersion = dav.getMajorVersion();
        int minorVersion = dav.getMinorVersion();
        int microVersion = dav.getIncrementalVersion();
        switch (changeType) {
            case MAJOR : return (majorVersion + 1) + ".0.0";
            case MINOR : return majorVersion + "." + (minorVersion + 1) + ".0";
            case MICRO : return majorVersion + "." + minorVersion + "." + (microVersion + 1);
            case NONE : return version;
            default: return version;
        }
    }

    /**
     * This method parse the given version and bump to the next major release version.
     * @param version The current version.
     * @return The next major release version.
     */
    public static String bumpToNextMajorVersion(final String version) {
        return bumpVersion(version, ChangeType.MAJOR);
    }

    /**
     * This method parse the given version and bump to the next minor release version.
     * @param version The current version.
     * @return The next minor release version.
     */
    public static String bumpToNextMinorVersion(final String version) {
        return bumpVersion(version, ChangeType.MINOR);
    }

    /**
     * This method parse the given version and bump to the next micro release version.
     * @param version The current version.
     * @return The next micro release version.
     */
    public static String bumpToNextMicroVersion(final String version) {
        return bumpVersion(version, ChangeType.MICRO);
    }

}
