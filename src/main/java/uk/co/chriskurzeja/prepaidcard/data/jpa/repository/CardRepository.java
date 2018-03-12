package uk.co.chriskurzeja.prepaidcard.data.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.chriskurzeja.prepaidcard.model.Card;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, String> {

    Card getCardByUserIdEquals(String userId);

    boolean existsCardByUserId(String userId);

}
