package com.ludditelabs.intellij.common;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import java.util.Arrays;
import java.util.List;

public class UtilsTests extends LightPlatformCodeInsightFixtureTestCase {
    // Test: how sortVersions() sorts version strings.
    public void testSortVersions() throws Throwable {
        List<String> versions = Arrays.asList("1.0.1", "0.0.2", "0.3.3", "0.2.0");
        Utils.sortVersions(versions);
        assertOrderedEquals(versions, "0.0.2", "0.2.0", "0.3.3", "1.0.1");
    }

    // Test: find suitable version.
    public void testVersions() throws Throwable {
        List<String> versions = Arrays.asList("0.0.2", "0.0.3", "0.2.0");
        Utils.sortVersions(versions);

        assertEquals("0.0.2", Utils.findClosestVersion(versions, "0.0.2"));
        assertEquals("0.0.3", Utils.findClosestVersion(versions, "0.1"));
        assertEquals("0.2.0", Utils.findClosestVersion(versions, "0.3.1"));
        assertNull(Utils.findClosestVersion(versions, "0.0.1"));
    }
}
