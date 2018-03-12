package uk.co.chriskurzeja.prepaidcard;

import com.google.common.collect.Lists;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uk.co.chriskurzeja.prepaidcard.card.UserService;
import uk.co.chriskurzeja.prepaidcard.controllers.UserCardController;
import uk.co.chriskurzeja.prepaidcard.data.jpa.repository.CardRepository;
import uk.co.chriskurzeja.prepaidcard.data.jpa.repository.TransactionRepository;
import uk.co.chriskurzeja.prepaidcard.model.Card;
import uk.co.chriskurzeja.prepaidcard.model.Transaction;
import uk.co.chriskurzeja.prepaidcard.model.TransactionKey;
import uk.co.chriskurzeja.prepaidcard.model.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SpringBootApplication
public class PrepaidcardApplication {

	private static final String alice = "alice";
	private static final String bob = "bob";
	private static final String chris = "chris";

	public static void main(String[] args) {
		SpringApplication.run(PrepaidcardApplication.class, args);
	}

	@Bean
	CommandLineRunner persistPreconfiguredCards(CardRepository cardRepository) {
		return (evt) -> {
			cardRepository.save(new Card(alice));
			cardRepository.save(new Card(bob));
			cardRepository.save(new Card(chris));
		};
	}

	@Bean
	CommandLineRunner persistPreconfiguredTransactions(CardRepository cardRepository, TransactionRepository transactionRepository) {
		return (evt) -> {
			Card aliceCard = cardRepository.getCardByUserIdEquals(alice);
			transactionRepository.save(loadMoney(aliceCard.getId(), 1000L));
			transactionRepository.save(loadMoney(aliceCard.getId(), 3000L));
			transactionRepository.saveAll(createTransaction(aliceCard.getId(), "Tesco", 2000, 2000, 0, 0));
			transactionRepository.saveAll(createTransaction(aliceCard.getId(), "Sainsburys", 1000, 1000,0, 1000));
			transactionRepository.saveAll(createTransaction(aliceCard.getId(), "HMV", 2000, 0, 1000, 0));

		};
	}

	private Transaction loadMoney(String cardId, long amount) {
		String transactionId = UUID.randomUUID().toString();
		return transaction(key(transactionId, cardId), "user", amount, TransactionType.LOAD_FUNDS);
	}

	private List<Transaction> createTransaction(String cardId, String merchant, long authorised, long captured, long reversed, long refunded) {
		String transactionId = UUID.randomUUID().toString();

		List<Transaction> transactions = new ArrayList<>();
		transactions.add(transaction(key(transactionId, cardId), merchant, authorised, TransactionType.AUTHORISE_TRANSACTION));

		if (captured > 0) {
			transactions.add(transaction(key(transactionId, cardId), merchant, refunded, TransactionType.CAPTURE_TRANSACTION_FUNDS));
		}

		if (reversed > 0) {
			transactions.add(transaction(key(transactionId, cardId), merchant, reversed, TransactionType.REVERSE_TRANSACTION));
		}

		if (refunded > 0 && captured >= refunded) {
			transactions.add(transaction(key(transactionId, cardId), merchant, refunded, TransactionType.REFUND_CAPTURED_FUNDS));
		}

		return transactions;
	}

	private Transaction transaction(TransactionKey key, String merchant, long amount, TransactionType type) {
		return new Transaction(key, merchant, amount, type);
	}

	private TransactionKey key(String transactionId, String cardId) {
		return new TransactionKey(transactionId, cardId);
	}

}
