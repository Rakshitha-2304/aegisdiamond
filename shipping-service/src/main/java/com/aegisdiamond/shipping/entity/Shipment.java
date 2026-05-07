package com.aegisdiamond.shipping.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shipment_diamonds", joinColumns = @JoinColumn(name = "shipment_id"))
    @Column(name = "diamond_id")
    private List<Long> diamondIds = new ArrayList<>();

    private Long shipperId;
    private String status;
    private String containerId;
    private String sealId;
    private boolean isSealed;

    public Shipment() {
        this.status = "CREATED";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public List<Long> getDiamondIds() { return diamondIds; }
    public void setDiamondIds(List<Long> diamondIds) { this.diamondIds = diamondIds; }

    public Long getShipperId() { return shipperId; }
    public void setShipperId(Long shipperId) { this.shipperId = shipperId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public String getSealId() { return sealId; }
    public void setSealId(String sealId) { this.sealId = sealId; }

    public boolean isSealed() { return isSealed; }
    public void setSealed(boolean sealed) { isSealed = sealed; }
}
