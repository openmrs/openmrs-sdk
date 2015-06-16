package org.openmrs.maven.plugins.model;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Class for Artifact model
 */
public class Artifact {
    private String version;
    private String groupId;
    private String artifactId;
    private String type;
    private String destFileName;

    public static final String GROUP_MODULE = "org.openmrs.module";
    public static final String GROUP_WEB = "org.openmrs.web";
    public static final String GROUP_OPENMRS = "org.openmrs";
    public static final String TYPE_OMOD = "omod";
    public static final String TYPE_WAR = "war";
    public static final String DEST_TEMPLATE = "%s-%s.%s";

    public Artifact() {};

    /**
     * Constructor if type is not set, and groupId is default
     * @param artifactId
     * @param version
     */
    public Artifact(String artifactId, String version) {
        this(artifactId, version, GROUP_MODULE);
    }

    /**
     * Constructor if type is not set
     * @param artifactId
     * @param version
     * @param groupId
     */
    public Artifact(String artifactId, String version, String groupId) {
        this(artifactId, version, groupId, null);
    }

    /**
     * Default constructor for all parameters
     * @param artifactId
     * @param version
     * @param groupId
     * @param type
     */
    public Artifact(String artifactId, String version, String groupId, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        // get values for destFileName
        String artifactType = (type != null) ? type : TYPE_OMOD;
        String id = artifactId.split("-")[0];
        // make destFileName
        this.destFileName = String.format(DEST_TEMPLATE, id, version, artifactType);
        // set type if not null
        if (type != null) this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestFileName() {
        return destFileName;
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

    /**
     * Convert object to Xpp3Dom model
     * @return
     */
    public Xpp3Dom toArtifactItem() {
        Xpp3Dom item = new Xpp3Dom("artifactItem");
        // create groupId
        Xpp3Dom domGroupId = new Xpp3Dom("groupId");
        domGroupId.setValue(groupId);
        item.addChild(domGroupId);
        // create artifactId
        Xpp3Dom domArtifactId = new Xpp3Dom("artifactId");
        domArtifactId.setValue(artifactId);
        item.addChild(domArtifactId);
        // create destFileName
        Xpp3Dom domDestFileName = new Xpp3Dom("destFileName");
        domDestFileName.setValue(destFileName);
        item.addChild(domDestFileName);
        // create version
        Xpp3Dom domVersion = new Xpp3Dom("version");
        domVersion.setValue(version);
        item.addChild(domVersion);
        // create type (if it set)
        if (type != null) {
            Xpp3Dom domType = new Xpp3Dom("type");
            domType.setValue(type);
            item.addChild(domType);
        }
        return item;
    }

    /**
     * create object from Xpp3Dom model
     * @param artifactItem
     * @return
     */
    public static final Artifact fromArtifactItem(Xpp3Dom artifactItem) {
        Artifact result = null;
        Xpp3Dom domArtifactId = artifactItem.getChild("artifactId");
        Xpp3Dom domGroupId = artifactItem.getChild("groupId");
        Xpp3Dom domVersion = artifactItem.getChild("version");
        Xpp3Dom domType = artifactItem.getChild("type");
        if ((domArtifactId != null) && (domGroupId != null) && (domVersion != null)) {
            result = new Artifact(domArtifactId.getValue(), domVersion.getValue(), domGroupId.getValue(), domType.getValue());
        }
        return result;
    }
}
