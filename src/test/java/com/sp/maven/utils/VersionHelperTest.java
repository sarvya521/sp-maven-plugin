
package com.sp.maven.utils;

import junit.framework.TestCase;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class VersionHelperTest extends TestCase {

    public VersionHelperTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of bumpToNextMajorVersion method, of class VersionHelper.
     */
    public void testDefaultArtifactVersion() {
        System.out.println("DefaultArtifactVersion");
        DefaultArtifactVersion dav;

        dav = new DefaultArtifactVersion("");
        assertEquals(dav.getMajorVersion(), 0);
        assertEquals(dav.getMinorVersion(), 0);
        assertEquals(dav.getIncrementalVersion(), 0);
        assertEquals(dav.getBuildNumber(), 0);
        assertEquals(dav.getQualifier(), "");

        dav = new DefaultArtifactVersion("1");
        assertEquals(dav.getMajorVersion(), 1);
        assertEquals(dav.getMinorVersion(), 0);
        assertEquals(dav.getIncrementalVersion(), 0);
        assertEquals(dav.getBuildNumber(), 0);
        assertNull(dav.getQualifier());

        dav = new DefaultArtifactVersion("1.2");
        assertEquals(dav.getMajorVersion(), 1);
        assertEquals(dav.getMinorVersion(), 2);
        assertEquals(dav.getIncrementalVersion(), 0);
        assertEquals(dav.getBuildNumber(), 0);
        assertNull(dav.getQualifier());

        dav = new DefaultArtifactVersion("1.2.3");
        assertEquals(dav.getMajorVersion(), 1);
        assertEquals(dav.getMinorVersion(), 2);
        assertEquals(dav.getIncrementalVersion(), 3);
        assertEquals(dav.getBuildNumber(), 0);
        assertNull(dav.getQualifier());

        dav = new DefaultArtifactVersion("1.2.3-qualifier");
        assertEquals(dav.getMajorVersion(), 1);
        assertEquals(dav.getMinorVersion(), 2);
        assertEquals(dav.getIncrementalVersion(), 3);
        assertEquals(dav.getBuildNumber(), 0);
        assertEquals(dav.getQualifier(), "qualifier");

        dav = new DefaultArtifactVersion("1.2.3-456qualifier");
        assertEquals(dav.getMajorVersion(), 1);
        assertEquals(dav.getMinorVersion(), 2);
        assertEquals(dav.getIncrementalVersion(), 3);
        assertEquals(dav.getBuildNumber(), 0);
        assertEquals(dav.getQualifier(), "456qualifier");

        dav = new DefaultArtifactVersion("1.2.3-4");
        assertEquals(dav.getMajorVersion(), 1);
        assertEquals(dav.getMinorVersion(), 2);
        assertEquals(dav.getIncrementalVersion(), 3);
        assertEquals(dav.getBuildNumber(), 4);
        assertNull(dav.getQualifier());
    }

    /**
     * Test of bumpToNextMajorVersion method, of class VersionHelper.
     */
    public void testBumpToNextMajorVersion() {
        System.out.println("bumpToNextMajorVersion");
        String version = "1.13.5";
        String expResult = "2.0.0";
        String result = VersionHelper.bumpToNextMajorVersion(version);
        assertEquals(expResult, result);
    }

    /**
     * Test of bumpToNextMinorVersion method, of class VersionHelper.
     */
    public void testBumpToNextMinorVersion() {
        System.out.println("bumpToNextMinorVersion");
        String version = "1.13.5";
        String expResult = "1.14.0";
        String result = VersionHelper.bumpToNextMinorVersion(version);
        assertEquals(expResult, result);
    }

    /**
     * Test of bumpToNextMicroVersion method, of class VersionHelper.
     */
    public void testBumpToNextMicroVersion() {
        System.out.println("bumpToNextMicroVersion");
        String version = "1.13.5";
        String expResult = "1.13.6";
        String result = VersionHelper.bumpToNextMicroVersion(version);
        assertEquals(expResult, result);
    }

}
