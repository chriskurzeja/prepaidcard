package uk.co.chriskurzeja.prepaidcard.model;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TransactionKey implements Serializable {

    private String requestId;
    private String cardId;

    public TransactionKey() {}

    public TransactionKey(String requestId, String cardId) {
        this.requestId = requestId;
        this.cardId = cardId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getCardId() {
        return cardId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionKey that = (TransactionKey) o;
        return Objects.equals(requestId, that.requestId) &&
                Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(requestId, cardId);
    }
}
