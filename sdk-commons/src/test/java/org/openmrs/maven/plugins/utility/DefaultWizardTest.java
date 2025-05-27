package org.openmrs.maven.plugins.utility;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

public class DefaultWizardTest {

	DefaultWizard wizard;

	@Before
	public void before() throws IOException {
		wizard = new DefaultWizard();
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfNoParamsSpecified()
			throws URISyntaxException, MojoExecutionException {
		String uri = "jdbc:mysql://localhost:3016/openmrs";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:mysql://localhost:3016/openmrs?autoReconnect=true&sessionVariables=default_storage_engine%3DInnoDB&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8);
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(expectedUri, StandardCharsets.UTF_8);

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfSomeParamsSpecified()
			throws URISyntaxException, MojoExecutionException {
		String uri = "jdbc:mysql://localhost:3016/openmrs?autoReconnect=false&useUnicode=false";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:mysql://localhost:3016/openmrs?autoReconnect=true&sessionVariables=default_storage_engine%3DInnoDB&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8);
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(expectedUri, StandardCharsets.UTF_8);

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}

	@Test
	public void addPostgresParamsIfMissing_shouldAddMissingPostgresParamsIfNoParamsSpecified()
			throws URISyntaxException, MojoExecutionException {
		String uri = "jdbc:postgresql://localhost:5438/openmrs";

		String correctUri = wizard.addPostgreSQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:postgresql://localhost:5438/openmrs?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8);
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8);

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}

	@Test
	public void addPostgresParamsIfMissing_shouldAddMissingPostgresParamsIfSomeParamsSpecified()
			throws URISyntaxException, MojoExecutionException {
		String uri = "jdbc:postgresql://localhost:5438/openmrs?autoReconnect=false&useUnicode=false";

		String correctUri = wizard.addPostgreSQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:postgresql://localhost:5438/openmrs?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8);
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8);

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}


	@Test
	 public void testIsJava8OrAbove() throws Exception {
		assertTrue(invokeIsJava8OrAbove("1.8.0_292"));
		assertTrue(invokeIsJava8OrAbove("11"));
		assertTrue(invokeIsJava8OrAbove("24"));
		assertFalse(invokeIsJava8OrAbove("1.6.0_65"));
	}



	private boolean invokeIsJava8OrAbove(String version) throws Exception {
		Method method = DefaultWizard.class.getDeclaredMethod("isJava8orAbove", String.class);
		method.setAccessible(true);
		return (boolean) method.invoke(new DefaultWizard(), version);
	}

}
