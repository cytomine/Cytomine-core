package be.cytomine.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;

@Transactional
public abstract class CRUDAuthorizationTest extends CRDAuthorizationTest {


    @Test
    @WithMockUser(username = SUPERADMIN)
    public void admin_can_edit_domain() {
        expectOK (() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_ADMIN)
    public void user_with_admin_permission_can_edit_domain() {
        expectOK (() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_DELETE)
    public void user_with_delete_permission_can_edit_domain() {
        expectOK (() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_CREATE)
    public void user_with_create_permission_can_edit_domain() {
        expectOK (() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_WRITE)
    public void user_with_write_permission_can_edit_domain() {
        expectOK (() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_ACL_READ)
    public void user_with_read_permission_can_edit_domain() {
        expectForbidden (() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = USER_NO_ACL)
    public void user_without_permission_cannot_edit_domain() {
        expectForbidden (() -> when_i_edit_domain());
    }

    @Test
    @WithMockUser(username = GUEST)
    public void guest_cannot_edit_domain() {
        expectForbidden (() -> when_i_edit_domain());
    }


    protected abstract void when_i_edit_domain();
}
