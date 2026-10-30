package com.npo.ticket;

public class VIPDecorator extends TicketDecorator {
    private double vipFee = 200.0;

    public VIPDecorator(ITicket wrappee) {
        super(wrappee);
    }

    @Override
    public double getPrice() {
        return super.getPrice() + vipFee;
    }

    @Override
    public String getDescription() {
       return super.getDescription() + " + VIP"; 
    }
    
}
