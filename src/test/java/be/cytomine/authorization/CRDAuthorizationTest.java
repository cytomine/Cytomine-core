package be.cytomine.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

@Transactional
public abstract class CRDAuthorizationTest extends AbstractAuthorizationTest {

    // *****************************
    // GET
    // *****************************

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_can_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_can_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_can_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_can_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_can_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_cannot_get_domain() {
        expectForbidden (() -> when_i_get_domain());
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_with_permission_can_get_domain() {
        expectOK (() -> when_i_get_domain());
    }

    // *****************************
    // ADD
    // *****************************

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_add_domain() {
        expectOK (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_can_add_domain() {
        expectOK (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_can_add_domain() {
        expectOK (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_can_add_domain() {
        expectOK (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_can_add_domain() {
        expectOK (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_can_add_domain() {
        expectForbidden (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_cannot_add_domain() {
        expectForbidden (() -> when_i_add_domain());
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_add_domain() {
        expectForbidden (() -> when_i_add_domain());
    }

    // *****************************
    // DELETE
    // *****************************

    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_delete_domain() {
        expectOK (() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_can_delete_domain() {
        expectOK (() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_can_delete_domain() {
        expectOK (() -> when_i_delete_domain());
    }


    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_can_delete_domain() {
        expectForbidden (() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_can_delete_domain() {
        expectForbidden (() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_can_delete_domain() {
        expectForbidden (() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_cannot_delete_domain() {
        expectForbidden (() -> when_i_delete_domain());
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_delete_domain() {
        expectForbidden (() -> when_i_delete_domain());
    }


    protected abstract void when_i_get_domain();

    protected abstract void when_i_add_domain();

    protected abstract void when_i_delete_domain();
}
