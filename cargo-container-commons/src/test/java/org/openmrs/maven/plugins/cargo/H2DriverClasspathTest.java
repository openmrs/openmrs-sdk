package org.openmrs.maven.plugins.cargo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Test;

/**
 * Webapps run by the embedded Cargo container load classes through this plugin's classloader, so the H2 driver must be
 * a dependency of this module (see SDK-409). Guards against the dependency being dropped.
 */
public class H2DriverClasspathTest {

	@Test
	public void h2DriverShouldBeOnTheClasspath() throws Exception {
		assertNotNull(Class.forName("org.h2.Driver"));
	}

	@Test
	public void h2DriverShouldOpenTheDefaultStyleUrl() throws Exception {
		try (Connection c = DriverManager.getConnection("jdbc:h2:mem:sdk409;DB_CLOSE_DELAY=-1", "root", "root")) {
			assertEquals("H2", c.getMetaData().getDatabaseProductName());
		}
	}
}
