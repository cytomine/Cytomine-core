package be.cytomine;

import be.cytomine.domain.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.UUID;

@Component
public class BasicInstanceBuilder {

    @Autowired
    EntityManager em;

    public User given_a_user() {
        //User user2 = new User();
        User user = given_a_user_not_persisted();
        em.persist(user);
        em.flush();
        return user;
    }

    public static User given_a_user_not_persisted() {
        //User user2 = new User();
        User user = new User();
        user.setFirstname("firstname");
        user.setLastname("lastname");
        user.setUsername(UUID.randomUUID().toString());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPublicKey(UUID.randomUUID().toString());
        user.setPrivateKey(UUID.randomUUID().toString());
        user.setPassword(UUID.randomUUID().toString());
        user.setOrigin("unkown");
        return user;
    }
}
