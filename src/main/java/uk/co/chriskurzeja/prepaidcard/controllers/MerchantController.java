package uk.co.chriskurzeja.prepaidcard.controllers;

import io.atlassian.fugue.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.chriskurzeja.prepaidcard.card.TransactionService;
import uk.co.chriskurzeja.prepaidcard.model.Transaction;
import uk.co.chriskurzeja.prepaidcard.model.TransactionKey;
import uk.co.chriskurzeja.prepaidcard.model.TransactionType;

import static uk.co.chriskurzeja.prepaidcard.controllers.utils.EitherUtils.eitherToResponse;

@RestController
@RequestMapping("/api/merchant/{merchantName}")
public class MerchantController {

    private final TransactionService transactionService;

    @Autowired
    public MerchantController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RequestMapping(path = "{action}/{cardId}/{transactionId}/{amountInPence}", method = RequestMethod.POST)
    public ResponseEntity<?> authoriseTransaction(
            @PathVariable String merchantName,
            @PathVariable String action,
            @PathVariable String cardId,
            @PathVariable String transactionId,
            @PathVariable long amountInPence) {

        Either<String,TransactionType> transactionType = transactionTypeFromString(action);
        if (transactionType.isRight()) {
            Transaction transaction = buildTransaction(merchantName, cardId, transactionId, amountInPence, transactionType.right().get());
            return eitherToResponse(transactionService.handleTransaction(transaction));
        } else {
            return eitherToResponse(transactionType);
        }
    }

    private Either<String,TransactionType> transactionTypeFromString(String action) {
        switch (action) {
            case "authorise":
                return Either.right(TransactionType.AUTHORISE_TRANSACTION);
            case "capture":
                return Either.right(TransactionType.CAPTURE_TRANSACTION_FUNDS);
            case "reverse":
                return Either.right(TransactionType.REVERSE_TRANSACTION);
            case "refund":
                return Either.right(TransactionType.REFUND_CAPTURED_FUNDS);
            default:
                return Either.left("Unknown transaction type " + action);
        }
    }

    private Transaction buildTransaction(String merchant, String cardId, String transactionId, long amountInPence, TransactionType type) {
        return new Transaction(
                new TransactionKey(transactionId, cardId),
                merchant,
                amountInPence,
                type
        );
    }
}
