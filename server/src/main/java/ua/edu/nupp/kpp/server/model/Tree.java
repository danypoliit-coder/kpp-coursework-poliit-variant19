
package ua.edu.nupp.kpp.server.model;

import java.math.BigDecimal;

public class Tree {
    private Long id;
    private String species;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal technicalState;
    private BigDecimal peopleIntensity;
    private BigDecimal greenArea;

    public Tree() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) {
        this.species = species;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getTechnicalState() {
        return technicalState;
    }
    public void setTechnicalState(BigDecimal value) {
        this.technicalState = value;
    }

    public BigDecimal getPeopleIntensity() {
        return peopleIntensity;
    }
    public void setPeopleIntensity(BigDecimal value) {
        this.peopleIntensity = value;
    }

    public BigDecimal getGreenArea() {
        return greenArea;
    }
    public void setGreenArea(BigDecimal value) {
        this.greenArea = value;
    }
}
