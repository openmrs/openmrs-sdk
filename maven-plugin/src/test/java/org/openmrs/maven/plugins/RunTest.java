package org.openmrs.maven.plugins;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


public class RunTest {

	@Test
	public void adjustXmxToAtLeast_shouldOverwriteExistingValueInMegabytes() {
		String mavenOpts = "-XX:MaxPermSize=128m -Xmx128m";

		String adjusted = new Run().adjustXmxToAtLeast(mavenOpts, 512);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=128m -Xmx512m")));
	}

	@Test
	public void adjustXmxToAtLeast_shouldOverwriteExistingValueInGigabytes() {
		String mavenOpts = "-XX:MaxPermSize=128m -Xmx1g";

		String adjusted = new Run().adjustXmxToAtLeast(mavenOpts, 2000);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=128m -Xmx2000m")));
	}

	@Test
	public void adjustXmxToAtLeast_shouldOverwriteExistingValueInKilobytes() {
		String mavenOpts = "-XX:MaxPermSize=128m -Xmx100k";

		String adjusted = new Run().adjustXmxToAtLeast(mavenOpts, 128);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=128m -Xmx128m")));
	}

	@Test
	public void adjustXmxToAtLeast_shouldOverwriteExistingValueInBytes() {
		String mavenOpts = "-XX:MaxPermSize=128m -Xmx100";

		String adjusted = new Run().adjustXmxToAtLeast(mavenOpts, 256);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=128m -Xmx256m")));
	}

	@Test
	public void adjustXmxToAtLeast_shouldSetValueIfMissing() {
		String mavenOpts = "-XX:MaxPermSize=128m";

		String adjusted = new Run().adjustXmxToAtLeast(mavenOpts, 256);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=128m -Xmx256m")));
	}

	@Test
	public void adjustMaxPermSizeToAtLeast_shouldOverwriteExistingValueInMegabytes() {
		String mavenOpts = "-XX:MaxPermSize=128m -Xmx128m";

		String adjusted = new Run().adjustMaxPermSizeToAtLeast(mavenOpts, 512);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=512m -Xmx128m")));
	}

	@Test
	public void adjustMaxPermSizeToAtLeast_shouldOverwriteExistingValueInGigabytes() {
		String mavenOpts = "-XX:MaxPermSize=1g -Xmx1g";

		String adjusted = new Run().adjustMaxPermSizeToAtLeast(mavenOpts, 2000);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=2000m -Xmx1g")));
	}

	@Test
	public void adjustMaxPermSizeToAtLeast_shouldOverwriteExistingValueInKilobytes() {
		String mavenOpts = "-XX:MaxPermSize=128k -Xmx100k";

		String adjusted = new Run().adjustMaxPermSizeToAtLeast(mavenOpts, 128);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=128m -Xmx100k")));
	}

	@Test
	public void adjustMaxPermSizeToAtLeast_shouldOverwriteExistingValueInBytes() {
		String mavenOpts = "-XX:MaxPermSize=128 -Xmx100";

		String adjusted = new Run().adjustMaxPermSizeToAtLeast(mavenOpts, 256);

		assertThat(adjusted, is(equalTo("-XX:MaxPermSize=256m -Xmx100")));
	}

	@Test
	public void adjustMaxPermSizeToAtLeast_shouldSetValueIfMissing() {
		String mavenOpts = "-Xmx256m";

		String adjusted = new Run().adjustMaxPermSizeToAtLeast(mavenOpts, 256);

		assertThat(adjusted, is(equalTo("-Xmx256m -XX:MaxPermSize=256m")));
	}

	@Test
	public void adjustMaxPermSizeToAtLeast_shouldSetValueIfMavenOptsEmpty() {
		String mavenOpts = "";

		String adjusted = new Run().adjustMaxPermSizeToAtLeast(mavenOpts, 256);

		assertThat(adjusted, is(equalTo(" -XX:MaxPermSize=256m")));
	}

}
