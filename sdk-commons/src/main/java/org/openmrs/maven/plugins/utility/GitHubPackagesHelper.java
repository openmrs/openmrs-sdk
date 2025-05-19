package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Server;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.GithubPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Helper class for managing GitHub Packages configuration in Maven settings.xml
 */
public class GitHubPackagesHelper {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubPackagesHelper.class);

    private final SettingsManager settingsManager;

    private static final String GITHUB_MAVEN_URL = "https://maven.pkg.github.com/%s/%s";

    public GitHubPackagesHelper(MavenEnvironment mavenEnvironment) throws MojoExecutionException {
        this.settingsManager = new SettingsManager(mavenEnvironment.getMavenSession());
    }

    public void configureGitHubPackages(DistroProperties distroProperties) throws MojoExecutionException {
        List<GithubPackage> packages = distroProperties.getGithubPackage();
        log.info("configureGithubPackages(): {}", packages);
        if (packages == null || packages.isEmpty()) {
            log.info("No GitHub Packages configurations found in distro properties");
            return;
        }

        log.info("Configuring {} GitHub Package repositories", packages.size());
        for (GithubPackage pkg : packages) {
            configureAuthentication(pkg);
            configureRepository(pkg);
        }

        try (OutputStream out = Files.newOutputStream(settingsManager.getSettingsFile().toPath())) {
            settingsManager.apply(out);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to save Maven settings", e);
        }
    }

    private void configureAuthentication(GithubPackage pkg) {
        log.info("Configuring authentication for GitHub Package: {}", pkg.getArtifactKey());
        if (pkg.getUsername() == null || pkg.getToken() == null || pkg.getOwner() == null || pkg.getRepository() == null) {
            log.warn("Skipping incomplete GitHub Package configuration for: {}", pkg.getArtifactKey());
            return;
        }
        settingsManager.getSettings().getServers().removeIf(s -> s.getId().equals("github"));
        Server server = new Server();
        server.setId("github");
        server.setUsername(pkg.getUsername());
        server.setPassword(pkg.getToken());
        settingsManager.getSettings().addServer(server);
    }

    private void configureRepository(GithubPackage pkg) {
        log.info("Configuring repository for GitHub Package: {}", pkg.getArtifactKey());
        if (pkg.getUsername() == null || pkg.getToken() == null || pkg.getOwner() == null || pkg.getRepository() == null) {
            log.warn("Skipping incomplete GitHub Package repository configuration for: {}", pkg.getArtifactKey());
            return;
        }
        Profile profile = settingsManager.getSettings().getProfiles().stream()
                .filter(p -> p.getId().equals("github"))
                .findFirst()
                .orElseGet(() -> {
                    Profile p = new Profile();
                    p.setId("github");
                    settingsManager.getSettings().addProfile(p);
                    return p;
                });

        profile.getRepositories().removeIf(r -> r.getId().equals("github") || r.getId().equals("central"));
        profile.addRepository(getCentralRepository());
        profile.addRepository(getRepository(pkg));

        if (!settingsManager.getSettings().getActiveProfiles().contains("github")) {
            settingsManager.getSettings().addActiveProfile("github");
        }
    }

    private static Repository getCentralRepository() {
        Repository central = new Repository();
        central.setId("central");
        central.setUrl("https://repo1.maven.org/maven2");
        return central;
    }

    private static Repository getRepository(GithubPackage pkg) {
        Repository repository = new Repository();
        repository.setId("github");
        repository.setName("GitHub OWNER Apache Maven Packages");
        repository.setUrl(String.format(GITHUB_MAVEN_URL, pkg.getOwner(), pkg.getRepository()));

        RepositoryPolicy snapshotPolicy = new RepositoryPolicy();
        snapshotPolicy.setEnabled(true);
        snapshotPolicy.setUpdatePolicy("always");
        repository.setSnapshots(snapshotPolicy);
        return repository;
    }
} 