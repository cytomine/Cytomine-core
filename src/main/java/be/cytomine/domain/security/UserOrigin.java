package be.cytomine.domain.security;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum UserOrigin {

    BOOTSTRAP("BOOTSTRAP", false),
    SYSTEM("SYSTEM", false),
    ADMINISTRATOR("ADMINISTRATOR", false),
    LDAP("LDAP", true),
    LTI("LTI", true),
    SAML("SAML", true),
    SHIBBOLETH("SHIBBOLETH", true);

    private final String name;

    @Getter
    private final boolean idPDelegated;

    UserOrigin(String name, boolean idPDelegated) {
        this.name = name;
        this.idPDelegated = idPDelegated;
    }

    private static final Map<String, UserOrigin> map;

    static {
        map = new HashMap<>();
        for (UserOrigin uo : UserOrigin.values()) {
            map.put(uo.name, uo);
        }
    }

    public static UserOrigin findByName(String name) {
        return map.get(name);
    }

    @Override
    public String toString() {
        return this.name;
    }

}
