package uk.co.chriskurzeja.prepaidcard.data.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.chriskurzeja.prepaidcard.model.Transaction;
import uk.co.chriskurzeja.prepaidcard.model.TransactionKey;
import uk.co.chriskurzeja.prepaidcard.model.TransactionType;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, TransactionKey> {

    Transaction findTransactionByTransactionKeyAndTransactionType(TransactionKey transactionKey, TransactionType transactionType);

    List<Transaction> findByTransactionKeyCardId(String cardId);

    List<Transaction> findByTransactionKey(TransactionKey transactionKey);

}
