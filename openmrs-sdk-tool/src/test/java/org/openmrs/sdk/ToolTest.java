package org.openmrs.sdk;

import org.dom4j.Document;
import org.dom4j.Element;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class ToolTest {

	@Test
	public void testSelectingArtifactItems() throws Exception {
		Tool tool = new Tool();
		Document document = tool.readXml(getClass().getResource("test-pom.xml").getPath());

		Element element = tool.updateArtifactItem(document,"org.openmrs.module", "logic", "123456789");

		Assert.assertThat(element, CoreMatchers.is(CoreMatchers.notNullValue()));
		Assert.assertThat(element.asXML(), CoreMatchers.containsString("123456789"));
	}

}
