package be.cytomine.search;

import be.cytomine.security.User;
import be.cytomine.utils.PaginationUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaginationUtilsTests {

    @Test
    public void testPaginationPage() {

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5 ; i++) {
            User user = new User()
            user.setUsername("user"+i)
            user.setId(10L-i)
            users.add(user)
        }

        def results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 0L)
        println results
        assert results.collection.size() == 5
        assert results.size == 5
        assert results.offset == 0
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "asc", 10L, 0L)
        println results
        assert results.collection.size() == 5
        assert results.size == 5
        assert results.offset == 0
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "desc", 3L, 0L)
        println results
        assert results.collection.size() == 3
        assert results.size == 5
        assert results.offset == 0
        assert results.perPage == 3
        assert results.totalPages == 2

        results = PaginationUtils.convertListToPage(users, "username", "desc", 3L, 3L)
        println results
        assert results.collection.size() == 2
        assert results.size == 5
        assert results.offset == 3
        assert results.perPage == 3
        assert results.totalPages == 2

        results = PaginationUtils.convertListToPage(users, "username", "desc", 1L, 0L)
        println results
        assert results.collection.size() == 1
        assert results.size == 5
        assert results.offset == 0
        assert results.perPage == 1
        assert results.totalPages == 5

        results = PaginationUtils.convertListToPage(users, "username", "desc", 100L, 100L)
        println results
        assert results.collection.size() == 0
        assert results.size == 5
        assert results.offset == 100
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 1L)
        println results
        assert results.collection.size() == 4
        assert results.size == 5
        assert results.offset == 1
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 2L)
        println results
        assert results.collection.size() == 3
        assert results.size == 5
        assert results.offset == 2
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 3L)
        println results
        assert results.collection.size() == 2
        assert results.size == 5
        assert results.offset == 3
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 4L)
        println results
        assert results.collection.size() == 1
        assert results.size == 5
        assert results.offset == 4
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 5L)
        println results
        assert results.collection.size() == 0
        assert results.size == 5
        assert results.offset == 5
        assert results.perPage == 5
        assert results.totalPages == 1

        results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 6L)
        println results
        assert results.collection.size() == 0
        assert results.size == 5
        assert results.offset == 6
        assert results.perPage == 5
        assert results.totalPages == 1
    }

    @Test
    public void testPaginationSort() {

        List<User> users = new ArrayList<>()
        for (int i = 0 ; i < 5 ; i++) {
            User user = new User()
            user.setUsername("user"+i)
            user.setId(10L-i)
            users.add(user)
        }

        def results = PaginationUtils.convertListToPage(users, "username", "asc", 5L, 0L)
        println results
        assert results.collection.size() == 5
        assert results.collection[0].username == 'user0'
        assert results.collection[1].username == 'user1'
        assert results.collection[2].username == 'user2'
        assert results.collection[3].username == 'user3'
        assert results.collection[4].username == 'user4'

        results = PaginationUtils.convertListToPage(users, "username", "desc", 5L, 0L)
        println results
        assert results.collection.size() == 5
        assert results.collection[0].username == 'user4'
        assert results.collection[1].username == 'user3'
        assert results.collection[2].username == 'user2'
        assert results.collection[3].username == 'user1'
        assert results.collection[4].username == 'user0'

        results = PaginationUtils.convertListToPage(users, "id", "asc", 5L, 0L)
        println results
        assert results.collection.size() == 5
        assert results.collection[0].username == 'user4'
        assert results.collection[1].username == 'user3'
        assert results.collection[2].username == 'user2'
        assert results.collection[3].username == 'user1'
        assert results.collection[4].username == 'user0'

        results = PaginationUtils.convertListToPage(users, "id", "asc", 3L, 2L)
        println results
        assert results.collection.size() == 3
        assert results.collection[0].username == 'user2'
        assert results.collection[1].username == 'user1'
        assert results.collection[2].username == 'user0'

        results = PaginationUtils.convertListToPage(users, "username", "asc", 3L, 2L)
        println results
        assert results.collection.size() == 3
        assert results.collection[0].username == 'user2'
        assert results.collection[1].username == 'user3'
        assert results.collection[2].username == 'user4'

        results = PaginationUtils.convertListToPage(users, "username", "desc", 3L, 2L)
        println results
        assert results.collection.size() == 3
        assert results.collection[0].username == 'user2'
        assert results.collection[1].username == 'user1'
        assert results.collection[2].username == 'user0'
    }
}
