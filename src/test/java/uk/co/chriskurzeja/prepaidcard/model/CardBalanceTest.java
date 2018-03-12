package uk.co.chriskurzeja.prepaidcard.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CardBalanceTest {

    //can block
    //funds have been authorised

    private final CardBalance balance = new CardBalance();
    private final long amount = 100L;
    private final long lessThanAmount = amount - 1;
    private final long moreThanAmount = amount + 1;

    @Test
    public void when_no_funds_have_been_loaded_we_cannot_block_an_amount() {
        assertThat(balance.canBlock(amount)).isFalse();
    }

    @Test
    public void when_more_funds_have_been_loaded_than_we_wish_to_block_we_can_block_an_amount() {
        balance.load(amount +1);
        assertThat(balance.canBlock(amount)).isTrue();
    }

    @Test
    public void when_the_exact_funds_have_been_loaded_that_we_wish_to_block_we_can_block_an_amount() {
        balance.load(amount);
        assertThat(balance.canBlock(amount)).isTrue();
    }

    @Test
    public void when_less_funds_have_been_loaded_than_we_wish_to_block_we_cannot_block_an_amount() {
        balance.load(amount - 1);
        assertThat(balance.canBlock(amount)).isFalse();
    }

    @Test
    public void when_no_funds_have_been_loaded_we_cannot_capture_an_amount() {
        assertThat(balance.canCapture(amount)).isFalse();
    }

    @Test
    public void when_funds_have_been_loaded_but_not_blocked_we_cannot_capture_an_amount() {
        balance.load(amount);
        assertThat(balance.canCapture(amount)).isFalse();
    }

    @Test
    public void when_more_funds_have_been_blocked_than_we_wish_to_capture_we_can_capture_an_amount() {
        balance.load(amount+1);
        balance.block(amount +1);
        assertThat(balance.canCapture(amount)).isTrue();
    }

    @Test
    public void when_the_exact_funds_have_been_blocked_that_we_wish_to_capture_we_can_capture_an_amount() {
        balance.load(amount);
        balance.block(amount);
        assertThat(balance.canCapture(amount)).isTrue();
    }

    @Test
    public void when_less_funds_have_been_blocked_than_we_wish_to_capture_we_cannot_capture_an_amount() {
        balance.load(amount);
        balance.block(lessThanAmount);
        assertThat(balance.canCapture(amount)).isFalse();
    }

    @Test
    public void when_more_funds_have_been_captured_than_we_wish_to_refund_we_can_refund_a_transaction() {
        balance.load(amount);
        balance.capture(amount);
        assertThat(balance.canRefund(lessThanAmount)).isTrue();
    }

    @Test
    public void when_the_exact_funds_have_been_captured_than_we_wish_to_refund_we_can_refund_a_transaction() {
        balance.load(amount);
        balance.capture(amount);
        assertThat(balance.canRefund(amount)).isTrue();
    }

    @Test
    public void when_less_funds_have_been_captured_than_we_wish_to_refund_we_cannot_refund_a_transaction() {
        balance.load(amount);
        balance.capture(amount);
        assertThat(balance.canRefund(moreThanAmount)).isFalse();
    }

    @Test
    public void when_more_funds_have_been_blocked_than_we_wish_to_reverse_we_can_reverse_a_transaction() {
        balance.load(amount);
        balance.block(amount);
        assertThat(balance.canReverse(lessThanAmount)).isTrue();
    }

    @Test
    public void when_the_exact_funds_have_been_blocked_than_we_wish_to_reverse_we_can_reverse_a_transaction() {
        balance.load(amount);
        balance.block(amount);
        assertThat(balance.canReverse(amount)).isTrue();
    }

    @Test
    public void when_less_funds_have_been_blocked_than_we_wish_to_reverse_we_cannot_reverse_a_transaction() {
        balance.load(amount);
        balance.block(amount);
        assertThat(balance.canReverse(moreThanAmount)).isFalse();
    }

    @Test
    public void if_we_reverse_a_transaction_we_cannot_capture_the_original_amount() {
        balance.load(amount);
        balance.block(amount);
        balance.reverse(lessThanAmount);
        assertThat(balance.canCapture(amount)).isFalse();
    }

    @Test
    public void if_we_refund_a_transaction_we_can_block_for_the_amount_refunded() {
        balance.load(amount);
        balance.block(amount);
        assertThat(balance.canBlock(amount)).isFalse();

        balance.capture(amount);
        balance.refund(amount);
        assertThat(balance.canBlock(amount)).isTrue();
    }

}
