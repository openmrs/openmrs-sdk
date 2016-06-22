package org.openmrs.maven.plugins.utility;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.utility.Wizard;
import org.openmrs.maven.plugins.utility.DefaultWizard;

public class WizardTest {

	Wizard wizard;

	@Before
	public void before() {
		wizard = new DefaultWizard(null);
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfNoParamsSpecified() {
		String uri = "jdbc:mysql://localhost:3016/openmrs";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		assertThat(correctUri, is(equalTo(
		    "jdbc:mysql://localhost:3016/openmrs?autoReconnect=true&sessionVariables=storage_engine%3DInnoDB&useUnicode=true&characterEncoding=UTF-8")));
	}

	@Test
	public void addMySQLParamsIfMissing_shouldAddMissingMySQLParamsIfSomeParamsSpecified() throws Exception {
		String uri = "jdbc:mysql://localhost:3016/openmrs?autoReconnect=false&useUnicode=false";

		String correctUri = wizard.addMySQLParamsIfMissing(uri);

		assertThat(correctUri, is(equalTo(
		    "jdbc:mysql://localhost:3016/openmrs?autoReconnect=false&sessionVariables=storage_engine%3DInnoDB&useUnicode=false&characterEncoding=UTF-8")));
	}

}
