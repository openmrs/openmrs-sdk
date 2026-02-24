#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package ${package};

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import ${package}.api.${moduleClassnamePrefix}Service;
import org.openmrs.test.jupiter.BaseContextSensitiveTest;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

import java.sql.SQLException;

/**
 * Tests for {@link ${moduleClassnamePrefix}Service}, verifying its core logic using a context sensitive approach.
 * <p>
 * This test class extends {@link BaseModuleContextSensitiveTest}, which sets up a full Spring application
 * context and in-memory database.
 */
public class ${moduleClassnamePrefix}ServiceTest extends BaseModuleContextSensitiveTest {


	/**
	 * Run this before each unit test in this class. This adds a bit more data to the base data that is
	 * done in the "@Before" method in {@link BaseContextSensitiveTest} (which is run right before this
	 * method).
	 *
	 * @throws SQLException An exception that provides information on a database access error.
	 */
	@BeforeEach
	public void runBeforeEachTest() throws SQLException {
		initializeInMemoryDatabase();
		executeDataSet("org/openmrs/api/include/item.xml");
	}

	@Test
	public void saveItem_shouldSetOwnerIfNotSet() {
		${moduleClassnamePrefix}Service service = Context.getService(${moduleClassnamePrefix}Service.class);
		UserService userService = Context.getUserService();
		Item savedItem = service.getItemByUuid("46e35514-ef9d-49c3-8d68-8ec1d0da0639");

		savedItem.setOwner(userService.getUser(0));

		assertEquals("daemon", savedItem.getOwner().getSystemId());
		assertEquals("Does not have an owner", savedItem.getDescription());
	}
}
