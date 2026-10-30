package com.npo.ticket;

public class BasicTicket implements ITicket {
    private double price;
    private String description;

    public BasicTicket(double price, String description) {
        this.price = price;
        this.description = description;
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public String getDescription() {
        return description;
    }

    
}
