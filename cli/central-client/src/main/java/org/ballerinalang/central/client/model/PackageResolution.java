package org.ballerinalang.central.client.model;

import java.util.List;

/**
 * {@code PackageResolution} represents package resolution from central.
 */
public class PackageResolution {

    private String organization;
    private String name;
    private List<String> versions;
    private List<PackageResolution> dependencies;

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    public List<PackageResolution> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<PackageResolution> dependencies) {
        this.dependencies = dependencies;
    }
}
