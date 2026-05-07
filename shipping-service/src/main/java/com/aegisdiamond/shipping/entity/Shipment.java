package com.aegisdiamond.shipping.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    private String id;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shipment_diamonds", joinColumns = @JoinColumn(name = "shipment_id"))
    @Column(name = "diamond_id")
    private List<String> diamondIds = new ArrayList<>();

    private String shipperId;
    private String status;
    private String containerId;
    private String sealId;
    private boolean isSealed;

    public Shipment() {
        this.id = UUID.randomUUID().toString();
        this.status = "CREATED";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public List<String> getDiamondIds() { return diamondIds; }
    public void setDiamondIds(List<String> diamondIds) { this.diamondIds = diamondIds; }

    public String getShipperId() { return shipperId; }
    public void setShipperId(String shipperId) { this.shipperId = shipperId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public String getSealId() { return sealId; }
    public void setSealId(String sealId) { this.sealId = sealId; }

    public boolean isSealed() { return isSealed; }
    public void setSealed(boolean sealed) { isSealed = sealed; }
}
