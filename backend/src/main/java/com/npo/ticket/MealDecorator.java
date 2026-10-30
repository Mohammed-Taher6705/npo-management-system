package com.npo.ticket;

public class MealDecorator extends TicketDecorator {
    private double mealFee = 50.0;
    private String mealType;            // e.g. "Standard", "Vegetarian"


    public MealDecorator(ITicket wrappee, String mealType) {
        super(wrappee);
        this.mealType = mealType;

    }

    @Override
    public double getPrice() {
        return super.getPrice() + mealFee;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + " + mealType + " meal";
    }
    
}
