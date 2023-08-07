package be.cytomine.security.ldap;

import be.cytomine.domain.meta.Configuration;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;

import static be.cytomine.service.meta.ConfigurationService.*;


@Slf4j
public class LdapClient {

    private String url;

    private String principal;

    private String credentials;

    public LdapClient(String url, String principal, String credentials) {
        this.url = url;
        this.principal = principal;
        this.credentials = credentials;
    }

    public LdapClient(LdapConfigurationInterface config) {
        this.url = config.getServer();
        this.principal = config.getPrincipal();
        this.credentials = config.getPassword();

        if (StringUtils.isBlank(this.url)) {
            throw new ForbiddenException("No LDAP server defined");
        }
        if (StringUtils.isBlank(this.principal)) {
            throw new ForbiddenException("No LDAP principal defined");
        }
        if (StringUtils.isBlank(this.credentials)) {
            throw new ForbiddenException("No LDAP password defined");
        }
    }

    public boolean isInLDAP(String search, String name, List<String> attrIDs) throws NamingException {
        return getUserInfo(search, name, attrIDs)!=null;
    }

    public Map<String, Object> getUserInfo(String search, String name, List<String> attrIDs) throws NamingException {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(attrIDs.toArray(new String[attrIDs.size()]));
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String filter = "(cn="+name+")";
        Hashtable env = new Hashtable();
        env.put(DirContext.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(DirContext.PROVIDER_URL, url );
        env.put(DirContext.SECURITY_AUTHENTICATION, "simple");
        env.put(DirContext.SECURITY_PRINCIPAL, principal);
        env.put(DirContext.SECURITY_CREDENTIALS, credentials);
        //env.put("java.naming.ldap.factory.socket", "MySSLSocketFactory");
        log.debug("Initialising LDAP context");
        DirContext dirContext = null;
        dirContext  = new InitialDirContext(env);
        Map<String, Object> properties = null;
        log.debug("Searching LDAP for {} with filter {}", search,filter);
        NamingEnumeration e = dirContext.search(search, filter,ctls);
        while (e.hasMore()) {
            properties = new LinkedHashMap<>();
            SearchResult entry = (SearchResult) e.next();
            log.debug("Found entry: {}", entry.getName());
            Iterator<? extends Attribute> iterator = entry.getAttributes().getAll().asIterator();
            while (iterator.hasNext()) {
                Attribute attribute = iterator.next();
                log.debug("Attribute: {} = {}", attribute.getID(), attribute.get());
                properties.put(attribute.getID(), attribute.get());
            }
        }
        if (properties != null) {
            log.debug("User properties found: {}", properties);
        } else {
            log.debug("No user properties found for search: {}", search);
        }
        return properties;
    }

    public boolean hasAttributeValue(String dn, String attributeName, String value) throws NamingException {
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes(new String[0]);       // Return no attrs
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE); // Search object only
        String comparisonFilter = "(" + attributeName + "="+value+")";
        Hashtable env = new Hashtable();
        env.put(DirContext.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(DirContext.PROVIDER_URL, url );
        env.put(DirContext.SECURITY_AUTHENTICATION, "simple");
        env.put(DirContext.SECURITY_PRINCIPAL, principal);
        env.put(DirContext.SECURITY_CREDENTIALS, credentials);
        //env.put("java.naming.ldap.factory.socket", "MySSLSocketFactory");
        DirContext dirContext = null;
        dirContext  = new InitialDirContext(env);
        NamingEnumeration<SearchResult> results = dirContext.search(dn, comparisonFilter, ctls);
        Boolean match = results.hasMore();
        log.debug("Attribute value match for DN '{}', attributeName '{}' and value '{}': {}", dn, attributeName, value, match);
        return match;
    }

    public boolean hasValidCredential(String dn, String attributeName, String value) throws NamingException {
       return hasAttributeValue(dn, attributeName, value);
    }

}
