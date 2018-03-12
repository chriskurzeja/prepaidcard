package uk.co.chriskurzeja.prepaidcard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class MerchantTransactionResult implements Serializable {

    @JsonProperty("card_id")
    private final String cardId;
    @JsonProperty("merchant")
    private final String merchant;
    @JsonProperty("transaction_id")
    private final String transactionId;
    @JsonProperty("blocked")
    private final long blocked;
    @JsonProperty("captured")
    private final long captured;
    @JsonProperty("transaction_type")
    private final TransactionType transactionType;

    public MerchantTransactionResult(Transaction transaction, TransactionBalance transactionBalance) {
        this.cardId = transaction.getTransactionKey().getCardId();
        this.merchant = transaction.getMerchant();
        this.transactionId = transaction.getTransactionKey().getRequestId();
        this.blocked = transactionBalance.getBlocked();
        this.captured = transactionBalance.getCaptured();
        this.transactionType = transaction.getTransactionType();
    }

    public long getBlocked() {
        return blocked;
    }

    public long getCaptured() {
        return captured;
    }
}
