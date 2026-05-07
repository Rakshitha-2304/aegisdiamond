package com.aegisdiamond.diamond.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "diamonds")
public class Diamond {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cut;

    @Column(nullable = false)
    private String clarity;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private double carat;

    @Column(unique = true, nullable = false)
    private Long certificateId;

    private Long ownerId;

    private String status;

    public Diamond() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCut() { return cut; }
    public void setCut(String cut) { this.cut = cut; }

    public String getClarity() { return clarity; }
    public void setClarity(String clarity) { this.clarity = clarity; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getCarat() { return carat; }
    public void setCarat(double carat) { this.carat = carat; }

    public Long getCertificateId() { return certificateId; }
    public void setCertificateId(Long certificateId) { this.certificateId = certificateId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
