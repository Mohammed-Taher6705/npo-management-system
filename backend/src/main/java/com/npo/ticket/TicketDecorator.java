package com.npo.ticket;

public abstract class TicketDecorator implements ITicket {
    protected ITicket wrappee;

    public TicketDecorator(ITicket wrappee) {
        this.wrappee = wrappee;
    }

    @Override
    public double getPrice() {
        return wrappee.getPrice();
    }

    @Override
    public String getDescription() {
        return wrappee.getDescription();
    }
    
}
