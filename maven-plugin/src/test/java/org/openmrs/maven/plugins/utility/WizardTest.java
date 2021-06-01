package org.openmrs.maven.plugins.utility;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class WizardTest {

	Wizard wizard;

	@Before
	public void before() {
		wizard = new Wizard(null);
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfNoParamsSpecified() {
		String uri = "jdbc:mysql://localhost:3016/openmrs";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		assertThat(correctUri, is(equalTo(
		    "jdbc:mysql://localhost:3016/openmrs?autoReconnect=true&sessionVariables=default_storage_engine%3DInnoDB&useUnicode=true&characterEncoding=UTF-8")));
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfSomeParamsSpecified() throws Exception {
		String uri = "jdbc:mysql://localhost:3016/openmrs?autoReconnect=false&useUnicode=false";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		assertThat(correctUri, is(equalTo(
		    "jdbc:mysql://localhost:3016/openmrs?autoReconnect=true&sessionVariables=default_storage_engine%3DInnoDB&useUnicode=true&characterEncoding=UTF-8")));
	}

}
