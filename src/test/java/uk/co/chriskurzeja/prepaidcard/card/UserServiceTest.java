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
import uk.co.chriskurzeja.prepaidcard.model.Card;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PrepaidcardApplication.class)
public class UserServiceTest {

    private final String userWithCard = "user";
    private final String userWithoutCard = "userWithoutCard";
    private String cardIdForUserWithCard;

    @Autowired
    private UserService userService;

    @Autowired
    private CardRepository cardRepository;

    @Before
    public void setup() {
        cardRepository.deleteAll();
        Either<String,String> result = userService.createCardForUser(userWithCard);
        assertThat(result.isRight()).isTrue();
        cardIdForUserWithCard = result.right().get();
    }

    @Test
    public void creating_a_card_for_a_user_will_return_the_cards_id() {
        Either<String,String> card = userService.createCardForUser(userWithoutCard);
        assertThat(card.isRight()).isTrue();
    }

    @Test
    public void creating_a_card_for_an_existing_user_will_fail() {
        Either<String,String> card = userService.createCardForUser(userWithCard);
        assertThat(card.isLeft()).isTrue();
    }

    @Test
    public void get_user_ids_returns_the_ids_of_all_users_with_a_card() {
        userService.createCardForUser(userWithoutCard);

        assertThat(userService.getUserIds())
                .containsOnly(userWithCard, userWithoutCard);
    }

    @Test
    public void get_card_id_for_user_for_an_unknown_user_will_fail() {
        Either<String,String> result = userService.getCardIdForUser(userWithoutCard);
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    public void get_card_id_for_known_user_will_return_card_id() {
        Either<String,String> result = userService.getCardIdForUser(userWithCard);
        assertThat(result.right().get()).isEqualTo(cardIdForUserWithCard);
    }

    @Test
    public void get_card_by_id_that_doesnt_exist_will_fail() {
        Either<String,Card> result  = userService.getCard("unknown");
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    public void get_card_by_id_that_exists_returns_the_card() {
        Either<String,Card> result = userService.getCard(cardIdForUserWithCard);
        assertThat(result.isRight()).isTrue();
        assertThat(result.right().get())
                .extracting(Card::getId)
                .containsOnly(cardIdForUserWithCard);
        assertThat(result.right().get())
                .extracting(Card::getUserId)
                .containsOnly(userWithCard);
    }

}
