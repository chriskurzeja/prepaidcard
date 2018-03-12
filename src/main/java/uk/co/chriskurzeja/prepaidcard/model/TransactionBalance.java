package uk.co.chriskurzeja.prepaidcard.model;

public class TransactionBalance {

    private long blocked = 0L;
    private long captured = 0L;

    public long getBlocked() {
        return blocked;
    }

    public long getCaptured() {
        return captured;
    }


    public void block(long amount) {
        blocked += amount;
    }

    public void capture(long amount) {
        blocked -= amount;
        captured += amount;
    }

    public void refund(long amount) {
        captured -= amount;
    }

    public void reverse(long amount) {
        blocked -= amount;
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
