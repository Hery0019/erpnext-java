package hery.itu.erp.erpnext;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class FiltersTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialiseAuFormatFrappe() throws Exception {
        Filters filters = Filters.where("employee", "=", "HR-EMP-00001")
                .gte("from_date", "2025-01-01")
                .in("status", List.of("Draft", "Submitted"))
                .between("posting_date", "2025-01-01", "2025-01-31")
                .like("first_name", "%an%");

        assertThat(mapper.writeValueAsString(filters.asList())).isEqualTo(
                "[[\"employee\",\"=\",\"HR-EMP-00001\"],"
                + "[\"from_date\",\">=\",\"2025-01-01\"],"
                + "[\"status\",\"in\",[\"Draft\",\"Submitted\"]],"
                + "[\"posting_date\",\"between\",[\"2025-01-01\",\"2025-01-31\"]],"
                + "[\"first_name\",\"like\",\"%an%\"]]");
    }

    @Test
    void lesGuillemetsDesValeursSontEchappes() throws Exception {
        Filters filters = Filters.where("supplier", "=", "Société \"Le Bon\" SARL");

        assertThat(mapper.writeValueAsString(filters.asList()))
                .isEqualTo("[[\"supplier\",\"=\",\"Société \\\"Le Bon\\\" SARL\"]]");
    }

    @Test
    void noneEstVide() {
        assertThat(Filters.none().isEmpty()).isTrue();
        assertThat(Filters.where("a", "=", 1).isEmpty()).isFalse();
    }
}
