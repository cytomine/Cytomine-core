package be.cytomine.domain.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.exceptions.*;
import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.UUID;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="class",
        discriminatorType = DiscriminatorType.STRING)
public class SecUser extends CytomineDomain {

    @Autowired
    @Transient
    PasswordEncoder passwordEncoder;

//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO, generator = "myGen")
//    @SequenceGenerator(name = "myGen", sequenceName = "hibernate_sequence", allocationSize=1)
//    protected Long id;


    @NotNull
    @NotBlank
    @Column(nullable = false)
//    @Pattern(regexp = "^[^\\ ].*[^\\ ]\\$") TODO
    protected String username;

    @NotNull
    @NotBlank
    @Column(name = "`password`", nullable = false)
    protected String password;

    @Transient
    String newPassword = null;

    @NotBlank
    @Column(nullable = true)
    protected String publicKey;

    @NotBlank
    @Column(nullable = true)
    protected String privateKey;

    protected Boolean enabled = true;

    protected Boolean accountExpired = false;

    protected Boolean accountLocked = false;

    protected Boolean passwordExpired = false;

    @NotBlank
    @Column(nullable = true)
    protected String origin;

    public SecUser() {
    }


    public String humanUsername() {
        return username;
    }

    public void generateKeys() {
        String privateKey = UUID.randomUUID().toString();
        String publicKey = UUID.randomUUID().toString();
        this.setPrivateKey(privateKey);
        this.setPublicKey(publicKey);
    }

    public Boolean isAlgo() {
        return false;
    }

    public String toString() {
        return username;
    }

    protected void encodePassword() {
        byte minLength = 8;
        if(password.length() < minLength) {
            throw new WrongArgumentException("Your password must have at least $minLength characters!");
        }
        password = passwordEncoder.encode(password);
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    public static JsonObject getDataFromDomain(CytomineDomain domain) {
        JsonObject returnArray = CytomineDomain.getDataFromDomain(domain);
        SecUser user = (SecUser)domain;
        returnArray.put("username", user.username);
        returnArray.put("origin", user.origin);
        returnArray.put("algo", user.isAlgo());
        return returnArray;
    }

    @Override
    public String toJSON() {
        return getDataFromDomain(this).toJsonString();
    }

    @Override
    public JsonObject toJsonObject() {
        return getDataFromDomain(this);
    }
}
