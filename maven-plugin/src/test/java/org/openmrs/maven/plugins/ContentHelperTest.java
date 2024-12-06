package org.openmrs.maven.plugins;

import org.junit.Test;
import org.openmrs.maven.plugins.model.ContentPackage;
import org.openmrs.maven.plugins.model.ContentProperties;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.utility.ContentHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentHelperTest {

    Map<String, ContentPackage> contentPackages = new LinkedHashMap<>();
    Map<String, ContentProperties> contentProperties = new LinkedHashMap<>();

    @Test
    public void getContentPackagesInInstallationOrder_shouldOrderDependenciesBeforeDependentPackages() throws Exception {
        addTestContentPackage("baseVersion");
        addTestContentPackage("districtVersion", "countryVersion");
        addTestContentPackage("facilityVersion", "districtVersion");
        addTestContentPackage("countryVersion", "baseVersion");

        ContentHelper contentHelper = mock(ContentHelper.class);
        when(contentHelper.getContentPackagesInInstallationOrder(any())).thenCallRealMethod();
        when(contentHelper.getContentProperties(any())).thenAnswer(invocation -> {
            ContentPackage contentPackage = invocation.getArgument(0, ContentPackage.class);
            return contentProperties.get(contentPackage.getArtifactId());
        });

        Properties p = new Properties();
        p.put("content.facilityVersion", "1.0.0");
        p.put("content.countryVersion", "1.0.0");
        p.put("content.districtVersion", "1.0.0");
        p.put("content.baseVersion", "1.0.0");
        DistroProperties distroProperties = new DistroProperties(p);

        List<ContentPackage> packages = contentHelper.getContentPackagesInInstallationOrder(distroProperties);
        assertNotNull(packages);
        assertThat(packages.size(), equalTo(4));
        assertThat(packages.get(0).getArtifactId(), equalTo("baseVersion"));
        assertThat(packages.get(1).getArtifactId(), equalTo("countryVersion"));
        assertThat(packages.get(2).getArtifactId(), equalTo("districtVersion"));
        assertThat(packages.get(3).getArtifactId(), equalTo("facilityVersion"));
    }

    void addTestContentPackage(String artifactId, String... dependantPackages) {
        ContentPackage contentPackage = new ContentPackage();
        contentPackage.setArtifactId(artifactId);
        contentPackage.setGroupId("org.openmrs.content");
        contentPackage.setVersion("1.0.0");
        contentPackage.setType("zip");
        contentPackages.put(artifactId, contentPackage);
        Properties properties = new Properties();
        for (String dependantPackage : dependantPackages) {
            properties.put("content." + dependantPackage, "1.0.0");
        }
        contentProperties.put(artifactId, new ContentProperties(properties));
    }
}
