package com.aegisdiamond.customs.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customs_declarations")
public class CustomsDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long shipmentId;

    private String originCountry;
    private String destinationCountry;
    private double declarationValue;
    private String status;
    private boolean isCompliant;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customs_documents", joinColumns = @JoinColumn(name = "declaration_id"))
    @Column(name = "document_id")
    private List<Long> documentIds = new ArrayList<>();

    private LocalDateTime submittedAt;
    private LocalDateTime clearedAt;

    public CustomsDeclaration() {
        this.submittedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }

    public double getDeclarationValue() { return declarationValue; }
    public void setDeclarationValue(double declarationValue) { this.declarationValue = declarationValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCompliant() { return isCompliant; }
    public void setCompliant(boolean compliant) { isCompliant = compliant; }

    public List<Long> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<Long> documentIds) { this.documentIds = documentIds; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getClearedAt() { return clearedAt; }
    public void setClearedAt(LocalDateTime clearedAt) { this.clearedAt = clearedAt; }
}
