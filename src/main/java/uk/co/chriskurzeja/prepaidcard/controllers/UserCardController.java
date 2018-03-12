package uk.co.chriskurzeja.prepaidcard.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.chriskurzeja.prepaidcard.card.TransactionService;
import uk.co.chriskurzeja.prepaidcard.card.UserService;
import uk.co.chriskurzeja.prepaidcard.model.Transaction;
import uk.co.chriskurzeja.prepaidcard.model.TransactionKey;
import uk.co.chriskurzeja.prepaidcard.model.TransactionType;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static uk.co.chriskurzeja.prepaidcard.controllers.utils.EitherUtils.eitherToResponse;

@RestController
@RequestMapping("/api/card")
public class UserCardController {

    private final UserService userService;
    private final TransactionService transactionService;

    @Autowired
    public UserCardController(UserService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    @RequestMapping(path = "create/{userId}", method = RequestMethod.GET)
    ResponseEntity<?> createCard(@PathVariable String userId) {
        return eitherToResponse(userService.createCardForUser(userId));
    }

    @RequestMapping(path = "load/{cardId}/{requestId}/{amountInPence}", method = RequestMethod.GET)
    ResponseEntity<?> loadMoney(@PathVariable String cardId, @PathVariable String requestId, @PathVariable long amountInPence) {
        Transaction transaction = new Transaction(
                new TransactionKey(requestId, cardId),
                "user",
                amountInPence,
                TransactionType.LOAD_FUNDS
        );

        return eitherToResponse(transactionService.handleTransaction(transaction));
    }

    @RequestMapping(path = "transactions/{cardId}", method = RequestMethod.GET)
    ResponseEntity<?> getTransactions(@PathVariable String cardId) {
        return eitherToResponse(transactionService.getTransactions(cardId));
    }

    @RequestMapping(path = "balance/loaded/{cardId}", method = RequestMethod.GET)
    ResponseEntity<?> getLoadedAmount(@PathVariable String cardId) {
        return eitherToResponse(transactionService.getTransactions(cardId).map(this::getAmountLoaded));
    }

    @RequestMapping(path = "balance/available/{cardId}", method = RequestMethod.GET)
    ResponseEntity<?> getAvailableAmount(@PathVariable String cardId)  {
        return eitherToResponse(transactionService.getTransactions(cardId).map(this::getAmountAvailable));
    }

    @RequestMapping(path = "balance/blocked/{cardId}", method = RequestMethod.GET)
    ResponseEntity<?> getBlockedAmount(@PathVariable String cardId)  {
        return eitherToResponse(transactionService.getTransactions(cardId).map(this::getAmountBlocked));
    }

    private long getAmountLoaded(List<Transaction> transactions) {
        Predicate<Transaction> filter = t -> t.getTransactionType() == TransactionType.LOAD_FUNDS;
        return filterMapReduceTransactions(transactions, filter, Transaction::getAmount);
    }

    private long getAmountAvailable(List<Transaction> transactions) {
        Predicate<Transaction> filter = t -> t.getTransactionType() != TransactionType.CAPTURE_TRANSACTION_FUNDS;
        Function<Transaction,Long> map = t -> (t.getTransactionType() == TransactionType.AUTHORISE_TRANSACTION) ? -t.getAmount() : t.getAmount();

        return filterMapReduceTransactions(transactions, filter, map);
    }

    private long getAmountBlocked(List<Transaction> transactions) {
        Predicate<Transaction> filter = t -> t.getTransactionType() != TransactionType.LOAD_FUNDS;
        Function<Transaction, Long> map = t -> (t.getTransactionType() == TransactionType.AUTHORISE_TRANSACTION) ? t.getAmount() : -t.getAmount();

        return filterMapReduceTransactions(transactions, filter, map);
    }

    private long filterMapReduceTransactions(List<Transaction> transactions, Predicate<Transaction> filter, Function<Transaction,Long> map) {
        return transactions.stream()
                .filter(filter)
                .map(map)
                .reduce((a,b) -> a + b)
                .orElse(0L);
    }

}
