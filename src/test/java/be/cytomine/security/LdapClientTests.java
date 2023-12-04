package be.cytomine.security;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.security.ldap.LdapClient;
import org.junit.jupiter.api.Test;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapClientTests {

    private LdapClient ldapClient = new LdapClient(
            BasicInstanceBuilder.LDAP_URL,
        "CN=admin,OU=users,DC=mtr,DC=com",
        "itachi"
    );

    @Test
    public void retrieve_existing_user_from_ldap() throws NamingException {
        Map<String, Object> jdoe = ldapClient.getUserInfo("OU=users,DC=mtr,DC=com", "cn", "jdoeLDAP", List.of("cn", "sn", "givenname", "mail", "userPassword"));
        assertThat(jdoe).isNotNull();
        assertThat(jdoe).containsOnlyKeys("cn", "sn", "givenname", "mail");
        assertThat(jdoe.get("cn")).isEqualTo("jdoeLDAP");
        assertThat(jdoe.get("mail")).isEqualTo("jdoeLDAP@email.com");
    }

    @Test
    public void retrieve_existing_user_from_ldap_with_attribute_name() throws NamingException {
        Map<String, Object> jdoe = ldapClient.getUserInfo("OU=users,DC=mtr,DC=com", "uid", "jdoeLDAP", List.of("cn", "sn", "givenname", "mail", "userPassword"));
        assertThat(jdoe).isNotNull();
        assertThat(jdoe).containsOnlyKeys("cn", "sn", "givenname", "mail");
        assertThat(jdoe.get("cn")).isEqualTo("jdoeLDAP");
        assertThat(jdoe.get("mail")).isEqualTo("jdoeLDAP@email.com");
    }

    @Test
    public void retrieve_existing_user_from_ldap_with_invalid_attribute_name() throws NamingException {
        Map<String, Object> jdoe = ldapClient.getUserInfo("OU=users,DC=mtr,DC=com", "notgood", "jdoeLDAP", List.of("cn", "sn", "givenname", "mail", "userPassword"));
        assertThat(jdoe).isNull();
    }

    @Test
    public void retrieve_unexisting_user_from_ldap() throws NamingException {
        Map<String, Object> jdoe = ldapClient.getUserInfo("OU=users,DC=mtr,DC=com", "cn", "jdoeNotInLDAP", List.of("cn", "sn", "givenname", "mail"));
        assertThat(jdoe).isNull();
    }

    @Test
    public void check_existing_user_from_ldap() throws NamingException {
        assertThat(ldapClient.isInLDAP("OU=users,DC=mtr,DC=com", "cn", "jdoeLDAP", List.of("cn"))).isTrue();
    }

    @Test
    public void check_unexisting_user_from_ldap() throws NamingException {
        assertThat(ldapClient.isInLDAP("OU=users,DC=mtr,DC=com", "cn", "jdoeNotInLDAP", List.of("cn"))).isFalse();
    }

    @Test
    public void check_existing_user_from_ldap_with_attribute_name() throws NamingException {
        assertThat(ldapClient.isInLDAP("OU=users,DC=mtr,DC=com", "uid", "jdoeLDAP", List.of("cn"))).isTrue();
    }


    @Test
    public void check_existing_user_from_ldap_with_invalid_attribute_name() throws NamingException {
        assertThat(ldapClient.isInLDAP("OU=users,DC=mtr,DC=com", "notgood", "jdoeLDAP", List.of("cn"))).isFalse();
    }

    @Test
    public void check_valid_attribute_value() throws NamingException {
        assertThat(ldapClient.hasAttributeValue("cn=jdoeLDAP,OU=users,DC=mtr,DC=com", "mail", "jdoeLDAP@email.com")).isTrue();
    }

    @Test
    public void check_unvalid_attribute_value() throws NamingException {
        assertThat(ldapClient.hasAttributeValue("cn=jdoeLDAP,OU=users,DC=mtr,DC=com", "mail", "bademail@email.com")).isFalse();
    }

    @Test
    public void check_valid_password() throws NamingException {
        assertThat(ldapClient.hasValidCredential("cn=jdoeLDAP,OU=users,DC=mtr,DC=com", "goodPassword")).isTrue();
    }

    @Test
    public void check_unvalid_password_for_user() throws NamingException {
        assertThat(ldapClient.hasValidCredential("cn=admin,OU=users,DC=mtr,DC=com", "goodPassword")).isFalse();
    }

    @Test
    public void check_unvalid_password() throws NamingException {
        assertThat(ldapClient.hasValidCredential("cn=jdoeLDAP,OU=users,DC=mtr,DC=com",  "badPassword")).isFalse();
    }
}




