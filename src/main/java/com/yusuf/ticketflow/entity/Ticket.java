package com.yusuf.ticketflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    private Long id;
    private String name;
    private int stock;

    // 1. Empty Constructor
    public Ticket() {
    }

    // 2. Full Constructor (Used in DataSeeder)
    public Ticket(Long id, String name, int stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    // 3. Getters and Setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}