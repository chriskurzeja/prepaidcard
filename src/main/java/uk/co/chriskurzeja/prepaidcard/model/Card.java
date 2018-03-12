package uk.co.chriskurzeja.prepaidcard.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.UUID;

@Entity
public class Card implements Serializable {

    @Id
    private String id;

    private String userId;

    public Card() {

    }

    public Card(String userId) {
        id = UUID.randomUUID().toString();
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

}
