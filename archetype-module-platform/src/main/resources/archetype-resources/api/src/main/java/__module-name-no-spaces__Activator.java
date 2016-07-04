#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package ${package};

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ${groupId}.Activator;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class ${module-name-no-spaces}Activator implements Activator {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * @see ${groupId}.Activator${symbol_pound}startup()
	 */
	public void startup() {
		log.info("Starting ${module-name}");
	}
	
	/**
	 * @see ${groupId}.Activator${symbol_pound}shutdown()
	 */
	public void shutdown() {
		log.info("Shutting down ${module-name}");
	}
	
}
