package com.npo.payment;


/** Concrete Strategy: the donor pays with a bank check. */
public class CheckPayment implements IPaymentMethod {

    private String checkNo;
    private String bankName;

    public CheckPayment(String checkNo, String bankName) {
        this.checkNo = checkNo;
        this.bankName = bankName;
    }

    @Override
    public boolean pay(double amount) {
        return validate() && amount > 0;     // check accepted (clearing happens at the bank)
    }

    @Override
    public boolean refund(double amount) {
        return validate() && amount > 0;
    }

    @Override
    public boolean validate() {
        return checkNo != null && checkNo.matches("\\d+")
                && bankName != null && !bankName.trim().isEmpty();
    }

    public String getCheckNo() { return checkNo; }
    public String getBankName() { return bankName; }
}
