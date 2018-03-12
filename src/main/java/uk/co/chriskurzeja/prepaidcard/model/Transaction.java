package uk.co.chriskurzeja.prepaidcard.model;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class Transaction implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    private TransactionKey transactionKey;
    private String merchant;
    private long amountInPence;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    public Transaction() {}

    public Transaction(TransactionKey transactionKey, String merchant, long amountInPence, TransactionType transactionType) {
        this.transactionKey = transactionKey;
        this.merchant = merchant;
        this.amountInPence = amountInPence;
        this.transactionType = transactionType;
    }

    public Long getId() {
        return id;
    }

    public TransactionKey getTransactionKey() {
        return transactionKey;
    }

    public String getMerchant() {
        return merchant;
    }

    public long getAmount() {
        return amountInPence;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionKey=" + transactionKey +
                ", merchant='" + merchant + '\'' +
                ", amount=" + amountInPence +
                ", transactionType=" + transactionType +
                '}';
    }
}
