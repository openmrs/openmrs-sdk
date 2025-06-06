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
package ${package}.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ${package}.Item;
import ${package}.api.dao.${moduleClassnamePrefix}Dao;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * It is an integration test (extends BaseModuleContextSensitiveTest), which verifies DAO methods
 * against the in-memory H2 database. The database is initially loaded with data from
 * standardTestDataset.xml in openmrs-api. All test methods are executed in transactions, which are
 * rolled back by the end of each test method.
 */
public class ${moduleClassnamePrefix}DaoTest extends BaseModuleContextSensitiveTest {

	@Autowired
	${moduleClassnamePrefix}Dao dao;

	@BeforeEach
	public void runBeforeEachTest() throws Exception {
		executeDataSet("org/openmrs/api/include/item.xml");
	}

	@Test
	public void saveItem_shouldSaveAllPropertiesInDb() {
		Item savedItem = dao.getItemByUuid("076824da-b692-44b7-x33f-abf2u4i0474d");
		assertNotNull(savedItem);
		assertEquals("1010d442-e134-11de-babe-001e378eb67e", savedItem.getOwner().getUuid());
		assertEquals("admin", savedItem.getOwner().getUsername());
		assertEquals("This is a test item description.", savedItem.getDescription());
	}
}
