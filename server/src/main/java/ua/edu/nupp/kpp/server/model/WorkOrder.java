
package ua.edu.nupp.kpp.server.model;

import java.time.LocalDate;

public class WorkOrder {
    private Long id;
    private Long requestId;
    private String assignedTo;
    private LocalDate orderDate;
    private String completionNotes;

    public WorkOrder() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getAssignedTo() {
        return assignedTo;
    }
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }
    public void setOrderDate(LocalDate date) {
        this.orderDate = date;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }
    public void setCompletionNotes(String notes) {
        this.completionNotes = notes;
    }
}
