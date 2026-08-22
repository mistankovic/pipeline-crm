package com.pipelinecrm.adapter.persistence;

import com.pipelinecrm.adapter.persistence.testing.PostgresBackedTest;
import com.pipelinecrm.application.port.out.CompanyRepository;
import com.pipelinecrm.application.port.out.Transactions;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.identity.CompanyId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The {@link Transactions} port promises the work is atomic. This is where that promise is
 * checked against a real database rather than against a test double that always commits.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
class SpringTransactionsTest extends PostgresBackedTest {

    @Autowired
    private Transactions transactions;

    @Autowired
    private CompanyRepository companies;

    private Company aCompany() {
        return new Company(CompanyId.of(UUID.randomUUID()), "Atomic " + UUID.randomUUID());
    }

    @Test
    void work_that_finishes_is_committed() {
        Company company = aCompany();

        transactions.execute(() -> {
            companies.save(company);
            return null;
        });

        assertThat(companies.findById(company.id())).isPresent();
    }

    @Test
    void work_that_throws_is_rolled_back_in_full() {
        Company first = aCompany();
        Company second = aCompany();

        assertThatThrownBy(() -> transactions.execute(() -> {
            companies.save(first);
            companies.save(second);
            throw new IllegalStateException("something went wrong halfway");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(companies.findById(first.id())).isEmpty();
        assertThat(companies.findById(second.id())).isEmpty();
    }

    @Test
    void the_result_of_the_work_is_returned_to_the_caller() {
        assertThat(transactions.<String>execute(() -> "the answer")).isEqualTo("the answer");
    }
}
