package uk.co.chriskurzeja.prepaidcard.card;

import io.atlassian.fugue.Either;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.co.chriskurzeja.prepaidcard.PrepaidcardApplication;
import uk.co.chriskurzeja.prepaidcard.data.jpa.repository.CardRepository;
import uk.co.chriskurzeja.prepaidcard.data.jpa.repository.TransactionRepository;
import uk.co.chriskurzeja.prepaidcard.model.MerchantTransactionResult;
import uk.co.chriskurzeja.prepaidcard.model.Transaction;
import uk.co.chriskurzeja.prepaidcard.model.TransactionKey;
import uk.co.chriskurzeja.prepaidcard.model.TransactionType;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.chriskurzeja.prepaidcard.model.TransactionType.LOAD_FUNDS;
import static uk.co.chriskurzeja.prepaidcard.model.TransactionType.AUTHORISE_TRANSACTION;
import static uk.co.chriskurzeja.prepaidcard.model.TransactionType.CAPTURE_TRANSACTION_FUNDS;
import static uk.co.chriskurzeja.prepaidcard.model.TransactionType.REFUND_CAPTURED_FUNDS;
import static uk.co.chriskurzeja.prepaidcard.model.TransactionType.REVERSE_TRANSACTION;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PrepaidcardApplication.class)
public class TransactionServiceTest {

    @Autowired
    private TransactionService service;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CardRepository cardRepository;

    private final String unknownCard = "unknownCard";
    private String cardWithoutMoney;
    private String cardWithPreloadedMoney;
    private final long loadedAmount = 1000L;
    private final long lessThanLoaded = 900L;
    private final long moreThanLoaded = 1200L;
    private final long negativeAmount = -10L;
    private final long zeroAmount = 0L;

    @Before
    public void setup() {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();

        cardWithoutMoney = userService.createCardForUser("user").right().get();
        cardWithPreloadedMoney = userService.createCardForUser("anotherUser").right().get();

        Either<String, Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, LOAD_FUNDS, loadedAmount));
        assertThat(result.isRight()).isTrue();
    }

    @Test
    public void handling_a_transaction_for_an_unknown_card_will_fail() {
        Either<String, Object> result = service.handleTransaction(transaction(unknownCard, LOAD_FUNDS, 100));
        assertTransactionFailed(result);
    }

    @Test
    public void loading_funds_with_a_negative_amount_will_fail() {
        Either<String, Object> result = service.handleTransaction(transaction(cardWithoutMoney, LOAD_FUNDS, -100L));
        assertTransactionFailed(result);
    }

    @Test
    public void loading_funds_with_a_zero_amount_will_fail() {
        Either<String, Object> result = service.handleTransaction(transaction(cardWithoutMoney, LOAD_FUNDS, 0L));
        assertTransactionFailed(result);
    }

    @Test
    public void loading_funds_with_a_positive_amount_will_succeed() {
        Either<String, Object> result = service.handleTransaction(transaction(cardWithoutMoney, LOAD_FUNDS, 100L));
        assertThat(result.isRight()).isTrue();
        assertThat(getTransactions(cardWithoutMoney))
                .extracting(Transaction::getAmount)
                .containsOnly(100L);
    }

    @Test
    public void authorising_a_transaction_on_a_card_with_no_balance_will_fail() {
        Either<String, Object> result = service.handleTransaction(transaction(cardWithoutMoney, AUTHORISE_TRANSACTION, 100L));
        assertTransactionFailed(result);
    }

    @Test
    public void authorising_a_transaction_on_a_card_with_sufficient_balance_will_succeed() {
        Either<String, Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, AUTHORISE_TRANSACTION, loadedAmount));

        assertTransactionSucceeded(result, MerchantTransactionResult::getBlocked, loadedAmount);
        assertThat(getTransactions(cardWithPreloadedMoney))
                .filteredOn(t -> t.getTransactionType() == AUTHORISE_TRANSACTION)
                .extracting(Transaction::getAmount)
                .containsOnly(loadedAmount);
    }

    @Test
    public void authorising_a_transaction_on_a_card_with_more_than_sufficient_balance_will_succeed() {
        Either<String, Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, AUTHORISE_TRANSACTION, lessThanLoaded));

        assertTransactionSucceeded(result, MerchantTransactionResult::getBlocked, lessThanLoaded);
        assertThat(getTransactions(cardWithPreloadedMoney))
                .filteredOn(t -> t.getTransactionType() == AUTHORISE_TRANSACTION)
                .extracting(Transaction::getAmount)
                .containsOnly(lessThanLoaded);
    }

    @Test
    public void authorising_a_transaction_on_a_card_with_insufficient_balance_will_fail() {
        Either<String, Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, AUTHORISE_TRANSACTION, moreThanLoaded));
        assertTransactionFailed(result);
    }

    @Test
    public void authorising_two_transactions_with_the_same_transaction_id_will_fail() {
        long toAuthorise = loadedAmount / 3;
        Either<String, Object> firstResult = service.handleTransaction(transaction(cardWithPreloadedMoney, AUTHORISE_TRANSACTION, toAuthorise));
        Either<String, Object> secondResult = service.handleTransaction(transaction(cardWithPreloadedMoney, AUTHORISE_TRANSACTION, toAuthorise));

        assertTransactionSucceeded(firstResult, MerchantTransactionResult::getBlocked, toAuthorise);
        assertTransactionFailed(secondResult);
    }

    //TODO: is this test strictly necessary?
    @Test
    public void authorising_multiple_transactions_that_add_up_to_less_than_the_loaded_balance_will_succeed() {
        long toAuthorise = loadedAmount / 3;

        Either<String, Object> firstResult = service.handleTransaction(transaction(cardWithPreloadedMoney, AUTHORISE_TRANSACTION, toAuthorise));
        Either<String, Object> secondResult = service.handleTransaction(transaction(cardWithPreloadedMoney, "anotherRequest", AUTHORISE_TRANSACTION, toAuthorise));

        assertTransactionSucceeded(firstResult, MerchantTransactionResult::getBlocked, toAuthorise);
        assertTransactionSucceeded(secondResult, MerchantTransactionResult::getBlocked, toAuthorise);
    }

    @Test
    public void capturing_funds_for_an_existing_transaction_will_succeed() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, loadedAmount));

        assertTransactionSucceeded(result, MerchantTransactionResult::getCaptured, loadedAmount);
        assertThat(getTransactions(cardWithPreloadedMoney))
                .filteredOn(t -> t.getTransactionType() == CAPTURE_TRANSACTION_FUNDS)
                .extracting(Transaction::getAmount)
                .containsOnly(loadedAmount);
    }

    @Test
    public void capturing_funds_for_a_transaction_that_does_not_exist_will_fail() {
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, loadedAmount));
        assertTransactionFailed(result);
    }

    @Test
    public void capturing_funds_for_a_transaction_with_a_negative_amount_will_fail() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, negativeAmount));

        assertTransactionFailed(result);
    }

    @Test
    public void capturing_funds_for_a_transaction_with_a_zero_amount_will_fail() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, zeroAmount));

        assertTransactionFailed(result);
    }

    @Test
    public void capturing_funds_for_a_transaction_with_an_amount_greater_than_authorised_will_fail() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, moreThanLoaded));

        assertTransactionFailed(result);
    }

    @Test
    public void capturing_funds_for_a_transaction_multiple_with_a_smaller_than_remaining_authorised_amount_will_succeed() {
        long captureAmount = loadedAmount / 3;

        authoriseTransaction();
        Either<String,Object> firstResult = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, captureAmount));
        Either<String,Object> secondResult = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, captureAmount));

        assertTransactionSucceeded(firstResult, MerchantTransactionResult::getCaptured, captureAmount);
        assertTransactionSucceeded(secondResult, MerchantTransactionResult::getCaptured, captureAmount + captureAmount);

        assertThat(getTransactions(cardWithPreloadedMoney))
                .filteredOn(t -> t.getTransactionType() == CAPTURE_TRANSACTION_FUNDS)
                .extracting(Transaction::getAmount)
                .containsExactly(captureAmount, captureAmount);
    }


    @Test
    public void reversing_a_transaction_for_an_unknown_transaction_will_fail() {
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, loadedAmount));
        assertTransactionFailed(result);
    }

    @Test
    public void reversing_a_transaction_for_a_known_transaction_will_succeed() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, loadedAmount));
        assertThat(result.isRight()).isTrue();
    }

    @Test
    public void reversing_a_transaction_for_more_than_the_authorized_amount_will_fail() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, moreThanLoaded));

        assertTransactionFailed(result);
    }

    @Test
    public void reversing_a_transaction_with_a_negative_amount_will_fail() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, negativeAmount));

        assertTransactionFailed(result);
    }

    @Test
    public void reversing_a_transaction_with_a_zeroed_amount_will_fail() {
        authoriseTransaction();
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, zeroAmount));

        assertTransactionFailed(result);
    }

    @Test
    public void reversing_a_transaction_multiple_times_will_succeed_if_the_amount_is_always_less_than_the_remaining_authorised_amount() {
        long captureAmount = loadedAmount / 3;

        authoriseTransaction();
        Either<String,Object> firstResult = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, captureAmount));
        Either<String,Object> secondResult = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, captureAmount));

        assertThat(firstResult.isRight()).isTrue();
        assertThat(secondResult.isRight()).isTrue();
        assertThat(getTransactions(cardWithPreloadedMoney))
                .filteredOn(t -> t.getTransactionType() == REVERSE_TRANSACTION)
                .extracting(Transaction::getAmount)
                .containsExactly(captureAmount, captureAmount);
    }

    @Test
    public void reversing_a_transaction_and_trying_to_capture_for_the_initial_authorized_amount_will_fail() {
        long captureAmount = loadedAmount / 3;

        authoriseTransaction();
        Either<String,Object> firstResult = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, captureAmount));
        Either<String,Object> secondResult = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, loadedAmount));
        assertThat(firstResult.isRight()).isTrue();
        assertThat(secondResult.isRight()).isFalse();
    }

    @Test
    public void reversing_a_transaction_and_trying_to_capture_the_remaining_authorized_amount_will_succeed() {
        long toReverse = loadedAmount / 3;
        long toCapture = loadedAmount - toReverse;

        authoriseTransaction();
        Either<String,Object> firstResult = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, toReverse));
        Either<String,Object> secondResult = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, toCapture));
        assertThat(firstResult.isRight()).isTrue();
        assertThat(secondResult.isRight()).isTrue();
    }

    @Test
    public void reversing_a_transaction_and_trying_to_capture_less_than_the_remaining_authorized_amount_will_succeed() {
        long toReverse = loadedAmount / 3;
        long toCapture = toReverse;

        authoriseTransaction();
        Either<String,Object> firstResult = service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, toReverse));
        Either<String,Object> secondResult = service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, toCapture));
        assertThat(firstResult.isRight()).isTrue();
        assertThat(secondResult.isRight()).isTrue();
    }

    @Test
    public void refunding_a_transaction_for_an_unknown_transaction_id_will_fail() {
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, loadedAmount));
        assertTransactionFailed(result);
    }

    @Test
    public void refunding_a_transaction_for_a_known_transaction_id_that_has_been_captured_will_succeed() {
        authoriseTransaction();
        captureAmount();

        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, loadedAmount));
        assertThat(result.isRight()).isTrue();
    }

    @Test
    public void refunding_an_amount_that_is_less_than_has_been_captured_will_succeed() {
        authoriseTransaction();
        captureAmount();

        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, lessThanLoaded));
        assertThat(result.isRight()).isTrue();
    }

    @Test
    public void refunding_an_amount_that_is_more_than_has_been_captured_will_fail() {
        authoriseTransaction();
        captureAmount();

        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, moreThanLoaded));
        assertTransactionFailed(result);
    }

    @Test
    public void refunding_multiple_amounts_that_are_less_in_total_than_the_amount_captured_will_succeed() {
        authoriseTransaction();
        captureAmount();

        long partialRefund = loadedAmount / 3;
        Either<String,Object> firstRefund = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, partialRefund));
        Either<String,Object> secondRefund = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, partialRefund));

        assertThat(firstRefund.isRight()).isTrue();
        assertThat(secondRefund.isRight()).isTrue();
    }

    @Test
    public void refunding_a_second_time_more_than_the_remaining_captured_amount_will_fail() {
        authoriseTransaction();
        captureAmount();

        long partialRefund = loadedAmount / 3;
        Either<String,Object> firstRefund = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, partialRefund));
        Either<String,Object> secondRefund = service.handleTransaction(transaction(cardWithPreloadedMoney, REFUND_CAPTURED_FUNDS, loadedAmount));

        assertThat(firstRefund.isRight()).isTrue();
        assertThat(secondRefund.isLeft()).isTrue();
    }

    private void assertTransactionFailed(Either<String, Object> result) {
        assertThat(result.isLeft()).isTrue();
    }

    private <T> void assertTransactionSucceeded(Either<String, Object> result, Function<MerchantTransactionResult, T> extractor, T expectedValue) {
        assertThat(result.isRight()).isTrue();
        assertThat(result.right().get()).isInstanceOf(MerchantTransactionResult.class);
        assertThat(result.map(MerchantTransactionResult.class::cast).right().get())
                .extracting(extractor::apply)
                .containsOnly(expectedValue);
    }

    private Transaction transaction(String card, TransactionType type, long amount) {
        return transaction(card, "request", type, amount);
    }

    private Transaction transaction(String cardId, String requestId, TransactionType type, long amount) {
        return new Transaction(new TransactionKey(requestId, cardId), "user", amount, type);
    }

    private List<Transaction> getTransactions(String cardId) {
        Either<String,List<Transaction>> wrappedTransactions = service.getTransactions(cardId);

        assertThat(wrappedTransactions.isRight()).isTrue();
        return wrappedTransactions.right().get();
    }

    private void authoriseTransaction() {
        Either<String,Object> result = service.handleTransaction(transaction(cardWithPreloadedMoney, AUTHORISE_TRANSACTION, loadedAmount));
        assertThat(result.isRight()).isTrue();
    }

    private void captureAmount() {
        captureAmount(loadedAmount);
    }

    private void captureAmount(long amount) {
        service.handleTransaction(transaction(cardWithPreloadedMoney, CAPTURE_TRANSACTION_FUNDS, amount));
    }

    private void reverseTransaction(long amount) {
        service.handleTransaction(transaction(cardWithPreloadedMoney, REVERSE_TRANSACTION, amount));
    }


}
