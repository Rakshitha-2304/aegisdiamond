package com.aegisdiamond.vault.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vaults")
public class Vault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private int capacity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vault_inventory", joinColumns = @JoinColumn(name = "vault_id"))
    @Column(name = "diamond_id")
    private List<Long> diamondIds = new ArrayList<>();

    private double latitude;
    private double longitude;

    public Vault() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<Long> getDiamondIds() { return diamondIds; }
    public void setDiamondIds(List<Long> diamondIds) { this.diamondIds = diamondIds; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isFull() {
        return diamondIds.size() >= capacity;
    }
}
