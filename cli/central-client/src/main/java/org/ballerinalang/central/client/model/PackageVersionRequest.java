package org.ballerinalang.central.client.model;

/**
 * {@code PackageVersionRequest} represents package version request from central.
 */
public class PackageVersionRequest {

    private String organization;
    private String name;

    public PackageVersionRequest(String organization, String name) {
        this.organization = organization;
        this.name = name;
    }

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
}
