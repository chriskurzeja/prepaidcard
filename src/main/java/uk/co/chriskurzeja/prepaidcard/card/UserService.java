package uk.co.chriskurzeja.prepaidcard.card;

import io.atlassian.fugue.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.chriskurzeja.prepaidcard.data.jpa.repository.CardRepository;
import uk.co.chriskurzeja.prepaidcard.model.Card;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final CardRepository cardRepository;

    @Autowired
    public UserService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Either<String,String> createCardForUser(String userId) {
        if (cardRepository.existsCardByUserId(userId)) {
            return Either.left("Cannot create duplicate card for userId");
        }

        Card card = persistCard(new Card(userId));
        return Either.right(card.getId());
    }

    private Card persistCard(Card card) {
        return cardRepository.save(card);
    }

    public Set<String> getUserIds() {
        return cardRepository.findAll().stream()
                .map(Card::getUserId)
                .collect(Collectors.toSet());
    }

    public Either<String,String> getCardIdForUser(String userId) {
        return Optional.ofNullable(cardRepository.getCardByUserIdEquals(userId))
                .map(c -> Either.<String,String>right(c.getId()))
                .orElse(Either.left("Card does not exist for supplied userId"));
    }

    public Either<String,Card> getCard(String cardId) {
        return cardRepository.findById(cardId)
                .map(Either::<String, Card>right)
                .orElse(Either.left("Card does not exist"));
    }

}
