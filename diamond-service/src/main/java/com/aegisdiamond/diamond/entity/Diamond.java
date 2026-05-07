package com.aegisdiamond.diamond.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "diamonds")
public class Diamond {

    @Id
    private String id;

    @Column(nullable = false)
    private String cut;

    @Column(nullable = false)
    private String clarity;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private double carat;

    @Column(unique = true, nullable = false)
    private String certificateId;

    private String ownerId;

    private String status;

    public Diamond() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCut() { return cut; }
    public void setCut(String cut) { this.cut = cut; }

    public String getClarity() { return clarity; }
    public void setClarity(String clarity) { this.clarity = clarity; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getCarat() { return carat; }
    public void setCarat(double carat) { this.carat = carat; }

    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String certificateId) { this.certificateId = certificateId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
