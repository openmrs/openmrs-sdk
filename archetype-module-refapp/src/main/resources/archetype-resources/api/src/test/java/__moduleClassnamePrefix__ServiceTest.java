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
package ${package}.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import ${package}.Item;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

/**
 * Tests for {@link ${moduleClassnamePrefix}Service}, verifying its core logic using a context sensitive approach.
 * <p>
 * This test class extends {@link BaseModuleContextSensitiveTest}, which sets up a full Spring application
 * context and in-memory database.
 */
public class ${moduleClassnamePrefix}ServiceTest extends BaseModuleContextSensitiveTest {

	private static final String ITEM_UUID = "46e35514-ef9d-49c3-8d68-8ec1d0da0639";

	@BeforeEach
	public void runBeforeEachTest() throws Exception {
		executeDataSet("org/openmrs/api/include/item.xml");
	}

	@Test
	public void saveItem_shouldSetOwnerIfNotSet() {
		// Given
		${moduleClassnamePrefix}Service service = Context.getService(${moduleClassnamePrefix}Service.class);
		UserService userService = Context.getUserService();
		Item savedItem = service.getItemByUuid(ITEM_UUID);

		// When
		savedItem.setOwner(userService.getUser(0));

		// Then
		assertEquals("daemon", savedItem.getOwner().getSystemId());
		assertEquals("Does not have an owner", savedItem.getDescription());
	}
}
