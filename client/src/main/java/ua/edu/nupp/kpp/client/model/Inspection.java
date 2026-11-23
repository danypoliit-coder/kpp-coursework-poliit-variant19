
package ua.edu.nupp.kpp.client.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Inspection {

    private Long id;
    private Long treeId;
    private LocalDate inspectionDate;
    private String notes;
    private BigDecimal technicalState;
    private String inspector;

    public Inspection() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTreeId() {
        return treeId;
    }

    public void setTreeId(Long treeId) {
        this.treeId = treeId;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDate inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getTechnicalState() {
        return technicalState;
    }

    public void setTechnicalState(BigDecimal technicalState) {
        this.technicalState = technicalState;
    }

    public String getInspector() {
        return inspector;
    }

    public void setInspector(String inspector) {
        this.inspector = inspector;
    }
}
