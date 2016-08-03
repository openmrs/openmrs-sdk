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

import org.springframework.stereotype.Component;

/**
 * Contains module's config.
 */
@Component("${rootArtifactId}.${moduleClassnamePrefix}Config")
public class ${moduleClassnamePrefix}Config {
	
	public final static String MODULE_PRIVILEGE = "${moduleName} Privilege";
}
