package uk.co.chriskurzeja.prepaidcard.card;

import io.atlassian.fugue.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.chriskurzeja.prepaidcard.data.jpa.repository.TransactionRepository;
import uk.co.chriskurzeja.prepaidcard.model.CardBalance;
import uk.co.chriskurzeja.prepaidcard.model.MerchantTransactionResult;
import uk.co.chriskurzeja.prepaidcard.model.Transaction;
import uk.co.chriskurzeja.prepaidcard.model.TransactionBalance;
import uk.co.chriskurzeja.prepaidcard.model.TransactionKey;
import uk.co.chriskurzeja.prepaidcard.model.TransactionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final Map<String, CardBalance> cardBalances = new HashMap<>();
    private final Map<TransactionKey, TransactionBalance> transactionBalance = new HashMap<>();

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    public Either<String, Object> handleTransaction(Transaction transaction) {
        return userService.getCard(transaction.getTransactionKey().getCardId())
            .flatMap(card -> processTransaction(transaction));

    }

    private Either<String,Object> processTransaction(Transaction transaction) {
        switch(transaction.getTransactionType()) {
            case LOAD_FUNDS:
                return loadFunds(transaction);
            case AUTHORISE_TRANSACTION:
                return authoriseTransaction(transaction);
            case CAPTURE_TRANSACTION_FUNDS:
                return captureFunds(transaction);
            case REVERSE_TRANSACTION:
                return reverseAuthorisation(transaction);
            case REFUND_CAPTURED_FUNDS:
                return refundAuthorisation(transaction);
            default:
                return Either.left("Unknown transaction of type " + transaction.getTransactionType());
        }
    }

    public Either<String, List<Transaction>> getTransactions(String cardId) {
        List<Transaction> transactionList = transactionRepository.findByTransactionKeyCardId(cardId);
        if (transactionList.isEmpty()) {
            return Either.left("Could not retrieve transactions for cardId: " + cardId);
        } else {
            return Either.right(transactionList);
        }
    }

    private Either<String, Object> loadFunds(Transaction transaction) {
        return ifValidAmount(transaction, () -> {
            if (transactionRepository.findTransactionByTransactionKeyAndTransactionType(transaction.getTransactionKey(), transaction.getTransactionType()) != null) {
                return Either.left("Transaction has already been handled");
            }

            getBalanceForCard(transaction).load(transaction.getAmount());
            transactionRepository.save(transaction);
            return Either.right("Successfully loaded funds");
        });
    }

    private Either<String, Object> authoriseTransaction(Transaction transaction) {
        return ifValidAmount(transaction, () -> {
           if (transactionAlreadyHandled(transaction)) {
               return Either.left("Transaction has already been handled");
           }

           CardBalance cardBalance = getBalanceForCard(transaction);
           TransactionBalance transactionBalance = getTransactionBalance(transaction);
           if (!cardBalance.canBlock(transaction.getAmount())) {
               return Either.left("Insufficient funds on the card");
           }

           cardBalance.block(transaction.getAmount());
           transactionBalance.block(transaction.getAmount());
           transactionRepository.save(transaction);
           return Either.right(new MerchantTransactionResult(transaction, transactionBalance));
        });
    }

    private Either<String, Object> captureFunds(Transaction transaction) {
        return ifValidAmount(transaction, () -> {
            if (fundsWereNotAuthorisedForTransaction(transaction)) {
                return Either.left("Funds were not authorised for transaction");
            }

            CardBalance balance = getBalanceForCard(transaction);
            TransactionBalance transactionBalance = getTransactionBalance(transaction);

            if (!balance.canCapture(transaction.getAmount()) || !transactionBalance.canCapture(transaction.getAmount()) ) {
                return Either.left("Insufficient amount available to capture");
            }

            balance.capture(transaction.getAmount());
            transactionBalance.capture(transaction.getAmount());
            transactionRepository.save(transaction);
            return Either.right(new MerchantTransactionResult(transaction, transactionBalance));
        });
    }

    private Either<String, Object> reverseAuthorisation(Transaction transaction) {
        return ifValidAmount(transaction, () -> {
            if (fundsWereNotAuthorisedForTransaction(transaction)) {
                return Either.left("Funds were not authorised for transaction");
            }

            CardBalance balance = getBalanceForCard(transaction);
            TransactionBalance transactionBalance = getTransactionBalance(transaction);

            if (!balance.canReverse(transaction.getAmount()) || !transactionBalance.canReverse(transaction.getAmount())) {
                return Either.left("Insufficient amount available to reverse");
            }

            balance.reverse(transaction.getAmount());
            transactionBalance.reverse(transaction.getAmount());
            transactionRepository.save(transaction);
            return Either.right(new MerchantTransactionResult(transaction, transactionBalance));
        });
    }

    private Either<String, Object> refundAuthorisation(Transaction transaction) {
        return ifValidAmount(transaction, () -> {
            if (fundsWereNotAuthorisedForTransaction(transaction)) {
                return Either.left("Funds were not authorised for transaction");
            }

            CardBalance balance = getBalanceForCard(transaction);
            TransactionBalance transactionBalance = getTransactionBalance(transaction);

            if (!balance.canRefund(transaction.getAmount()) || !transactionBalance.canRefund(transaction.getAmount())) {
                return Either.left("Insufficient amount available to refund");
            }

            balance.refund(transaction.getAmount());
            transactionBalance.refund(transaction.getAmount());
            transactionRepository.save(transaction);
            return Either.right(new MerchantTransactionResult(transaction, transactionBalance));
        });
    }

    private CardBalance getBalanceForCard(Transaction transaction) {
        return cardBalances.computeIfAbsent(transaction.getTransactionKey().getCardId(), k -> new CardBalance());
    }

    private TransactionBalance getTransactionBalance(Transaction transaction) {
        return transactionBalance.computeIfAbsent(transaction.getTransactionKey(), k -> new TransactionBalance());
    }

    private boolean transactionAlreadyHandled(Transaction transaction) {
        TransactionKey key = transaction.getTransactionKey();
        TransactionType type = transaction.getTransactionType();

        Transaction existingTransaction = transactionRepository.findTransactionByTransactionKeyAndTransactionType(key, type);
        return existingTransaction != null;
    }

    private boolean fundsWereNotAuthorisedForTransaction(Transaction transaction) {
        TransactionKey key = transaction.getTransactionKey();
        TransactionType type = TransactionType.AUTHORISE_TRANSACTION;

        Transaction existingTransaction = transactionRepository.findTransactionByTransactionKeyAndTransactionType(key, type);
        return existingTransaction == null;
    }

    private Either<String, Object> ifValidAmount(Transaction transaction, Supplier<Either<String,Object>> supplier) {
        if (transaction.getAmount() <= 0) {
            return Either.left("Amount for transaction must be positive and non-zero");
        } else {
            return supplier.get();
        }
    }

}
