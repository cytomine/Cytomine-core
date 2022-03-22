package be.cytomine.utils;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTests {

    @Test
    public void pagination_from_already_limited_results() {
        assertThat(StringUtils.obscurify("password", 0)).isEqualTo("********");
        assertThat(StringUtils.obscurify("password", 1)).isEqualTo("p******d");
        assertThat(StringUtils.obscurify("password", 2)).isEqualTo("pa****rd");
        assertThat(StringUtils.obscurify("password", 3)).isEqualTo("pas**ord");
        assertThat(StringUtils.obscurify("password", 4)).isEqualTo("password");
        assertThat(StringUtils.obscurify("password", 5)).isEqualTo("password");
        assertThat(StringUtils.obscurify("password", 10)).isEqualTo("password");

        assertThat(StringUtils.obscurify("", 0)).isEqualTo("");
        assertThat(StringUtils.obscurify("", 10)).isEqualTo("");
    }
}
