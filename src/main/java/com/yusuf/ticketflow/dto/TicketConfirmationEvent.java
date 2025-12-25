package com.yusuf.ticketflow.dto;

public class TicketConfirmationEvent {
    private String email;
    private String ticketId;
    private String eventName;
    private Double price;

    // 1. No-Args Constructor
    public TicketConfirmationEvent() {
    }

    // 2. All-Args Constructor
    public TicketConfirmationEvent(String email, String ticketId, String eventName, Double price) {
        this.email = email;
        this.ticketId = ticketId;
        this.eventName = eventName;
        this.price = price;
    }

    // 3. Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    // 4. toString() (For logging)
    @Override
    public String toString() {
        return "TicketConfirmationEvent{" +
                "email='" + email + '\'' +
                ", ticketId='" + ticketId + '\'' +
                ", eventName='" + eventName + '\'' +
                ", price=" + price +
                '}';
    }
}