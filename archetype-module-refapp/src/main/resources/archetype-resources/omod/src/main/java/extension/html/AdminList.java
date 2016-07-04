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
package ${package}.extension.html;

import java.util.LinkedHashMap;
import java.util.Map;

import ${groupId}.Extension;
import ${groupId}.web.extension.AdministrationSectionExt;

/**
 * This class defines the links that will appear on the administration page under the
 * "${parentArtifactId}.title" heading. 
 */
public class AdminList extends AdministrationSectionExt {
	
	/**
	 * @see AdministrationSectionExt${symbol_pound}getMediaType()
	 */
	public Extension.MEDIA_TYPE getMediaType() {
		return Extension.MEDIA_TYPE.html;
	}
	
	/**
	 * @see AdministrationSectionExt${symbol_pound}getTitle()
	 */
	public String getTitle() {
		return "${parentArtifactId}.title";
	}
	
	/**
	 * @see AdministrationSectionExt${symbol_pound}getLinks()
	 */
	public Map<String, String> getLinks() {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		map.put("/module/${parentArtifactId}/manage.form", "${parentArtifactId}.manage");
		return map;
	}
	
}
