package uk.co.chriskurzeja.prepaidcard.model;

public class CardBalance {

    private long balance = 0L;
    private long blocked = 0L;
    private long captured = 0L;

    public CardBalance() {

    }

    public void load(long amount) {
        balance += amount;
    }

    public void block(long amount) {
        blocked += amount;
        balance -= amount;
    }

    public void capture(long amount) {
        blocked -= amount;
        captured += amount;
    }

    public void refund(long amount) {
        captured -= amount;
        balance += amount;
    }

    public void reverse(long amount) {
        blocked -= amount;
        balance += amount;
    }

    public boolean canBlock(long amount) {
        return balance >= amount;
    }

    public boolean canCapture(long amount) {
        return blocked >= amount;
    }

    public boolean canRefund(long amount) {
        return captured >= amount;
    }

    public boolean canReverse(long amount) {
        return blocked >= amount;
    }

}
