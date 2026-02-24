package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.openmrs.maven.plugins.model.Version;

import java.io.File;

/**
 * Validates that the current JDK is compatible with a given OpenMRS Platform version.
 *
 * Each platform generation has both minimum and maximum supported JDK versions:
 * Platform 1.x requires JDK 7 (JDK 8 generates a warning).
 * Platform 2.0–2.3 requires exactly JDK 8.
 * Platform 2.4–2.6 requires JDK 8–11.
 * Platform 2.7 requires JDK 8–17.
 * Platform 2.8+ requires JDK 8–21.
 * Platform 3.x requires JDK 21+.
 */
public class PlatformJdkValidator {

	private PlatformJdkValidator() {
	}

	public static void validate(Version platformVersion, String jdkVersion, Wizard wizard, File propertiesFile)
			throws MojoExecutionException {
		int platformMajor = platformVersion.getMajorVersion();
		int platformMinor = platformVersion.getMinorVersion();
		int jdkMajor = JDKVersionHelper.parseMajorVersion(jdkVersion);

		if (platformMajor == 1) {
			validatePlatform1(jdkVersion, jdkMajor, platformVersion, wizard, propertiesFile);
		} else if (platformMajor == 2) {
			validatePlatform2(jdkVersion, jdkMajor, platformMinor, platformVersion, wizard, propertiesFile);
		} else if (platformMajor == 3) {
			validatePlatform3(jdkVersion, jdkMajor, platformVersion, wizard, propertiesFile);
		} else {
			throw new MojoExecutionException("Invalid server platform version: " + platformVersion);
		}
	}

	private static void validatePlatform1(String jdkVersion, int jdkMajor, Version platformVersion,
			Wizard wizard, File propertiesFile) throws MojoExecutionException {
		if (jdkMajor > 8) {
			wizard.showJdkErrorMessage(jdkVersion, platformVersion.toString(), "JDK 7 or 8",
					propertiesFile.getPath());
			throw new MojoExecutionException(
					String.format("The JDK %s is not compatible with OpenMRS Platform %s.", jdkVersion,
							platformVersion));
		} else if (jdkMajor == 8) {
			wizard.showMessage(
					"Please note that it is not recommended to run OpenMRS platform " + platformVersion
							+ " on JDK 8.\n");
		}
	}

	private static void validatePlatform2(String jdkVersion, int jdkMajor, int platformMinor,
			Version platformVersion, Wizard wizard, File propertiesFile) throws MojoExecutionException {

		if (jdkMajor < 8) {
			throwIncompatible(jdkVersion, platformVersion, "JDK 8+", wizard, propertiesFile);
		}

		int maxJdk;
		if (platformMinor < 4) {
			// Platform 2.0–2.3: only JDK 8
			maxJdk = 8;
		} else if (platformMinor <= 6) {
			// Platform 2.4–2.6: JDK 8–11
			maxJdk = 11;
		} else if (platformMinor == 7) {
			// Platform 2.7: JDK 8–17
			maxJdk = 17;
		} else {
			// Platform 2.8+: JDK 8–21
			maxJdk = 21;
		}

		if (jdkMajor > maxJdk) {
			throwIncompatible(jdkVersion, platformVersion, "JDK 8 through " + maxJdk, wizard, propertiesFile);
		}
	}

	private static void validatePlatform3(String jdkVersion, int jdkMajor, Version platformVersion,
			Wizard wizard, File propertiesFile) throws MojoExecutionException {
		if (jdkMajor < 21) {
			throwIncompatible(jdkVersion, platformVersion, "JDK 21+", wizard, propertiesFile);
		}
	}

	private static void throwIncompatible(String jdkVersion, Version platformVersion, String required,
			Wizard wizard, File propertiesFile) throws MojoExecutionException {
		wizard.showJdkErrorMessage(jdkVersion, platformVersion.toString(), required,
				propertiesFile.getAbsolutePath());
		throw new MojoExecutionException(
				String.format("The JDK %s is not compatible with OpenMRS Platform %s. Minimum required: %s.",
						jdkVersion, platformVersion, required));
	}
}
