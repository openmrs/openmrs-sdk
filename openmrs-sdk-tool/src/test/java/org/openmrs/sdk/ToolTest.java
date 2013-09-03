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

		Element element = tool.selectArtifactItem(document, "coreapps");

		Assert.assertThat(element, CoreMatchers.is(CoreMatchers.notNullValue()));
		Assert.assertThat(element.getName(), CoreMatchers.is("coreapps"));
	}

}
