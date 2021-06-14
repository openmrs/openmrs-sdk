package org.openmrs.maven.plugins.utility;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;

public class DefaultWizardTest {

	DefaultWizard wizard;

	@Before
	public void before() throws IOException {
		wizard = new DefaultWizard();
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfNoParamsSpecified() throws URISyntaxException {
		String uri = "jdbc:mysql://localhost:3016/openmrs";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:mysql://localhost:3016/openmrs?autoReconnect=true&sessionVariables=default_storage_engine%3DInnoDB&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8.name());
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(expectedUri, StandardCharsets.UTF_8.name());

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfSomeParamsSpecified() throws URISyntaxException {
		String uri = "jdbc:mysql://localhost:3016/openmrs?autoReconnect=false&useUnicode=false";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:mysql://localhost:3016/openmrs?autoReconnect=true&sessionVariables=default_storage_engine%3DInnoDB&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8.name());
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(expectedUri, StandardCharsets.UTF_8.name());

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}

	@Test
	public void addPostgresParamsIfMissing_shouldAddMissingPostgresParamsIfNoParamsSpecified() throws URISyntaxException {
		String uri = "jdbc:postgresql://localhost:5438/openmrs";

		String correctUri = wizard.addPostgreSQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:postgresql://localhost:5438/openmrs?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8.name());
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(expectedUri, StandardCharsets.UTF_8.name());

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}

	@Test
	public void addPostgresParamsIfMissing_shouldAddMissingPostgresParamsIfSomeParamsSpecified() throws URISyntaxException {
		String uri = "jdbc:postgresql://localhost:5438/openmrs?autoReconnect=false&useUnicode=false";

		String correctUri = wizard.addPostgreSQLParamsIfMissing(uri);

		URI actualUri = new URI(correctUri);
		URI expectedUri = new URI("jdbc:postgresql://localhost:5438/openmrs?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");

		assertThat(actualUri.getScheme(), equalTo(expectedUri.getScheme()));
		assertThat(actualUri.getHost(), equalTo(expectedUri.getHost()));
		assertThat(actualUri.getPort(), equalTo(expectedUri.getPort()));
		assertThat(actualUri.getPath(), equalTo(expectedUri.getPath()));

		List<NameValuePair> actualNameValuePairs = URLEncodedUtils.parse(actualUri, StandardCharsets.UTF_8.name());
		List<NameValuePair> expectedNameValuePairs = URLEncodedUtils.parse(expectedUri, StandardCharsets.UTF_8.name());

		assertThat(actualNameValuePairs, everyItem(in(expectedNameValuePairs)));
	}

}
