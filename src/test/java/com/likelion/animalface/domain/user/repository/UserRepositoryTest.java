package com.likelion.animalface.domain.user.repository;

import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 슬라이스 테스트
 *
 * <p>H2 인메모리 DB 위에서 쿼리 메서드의 정확성을 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(User.create("testuser", "encoded_pw", "01012345678"));
    }

    // ── findByUsername ───────────────────────────────────

    @Test
    @DisplayName("findByUsername: 존재하는 username → 사용자 반환")
    void findByUsername_success() {
        Optional<User> result = userRepository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getPhone()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("findByUsername: 존재하지 않는 username → 빈 Optional")
    void findByUsername_notFound() {
        Optional<User> result = userRepository.findByUsername("ghost");

        assertThat(result).isEmpty();
    }

    // ── findByPhone ──────────────────────────────────────

    @Test
    @DisplayName("findByPhone: 존재하는 phone → 사용자 반환")
    void findByPhone_success() {
        Optional<User> result = userRepository.findByPhone("01012345678");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByPhone: 미등록 phone → 빈 Optional")
    void findByPhone_notFound() {
        Optional<User> result = userRepository.findByPhone("01099999999");

        assertThat(result).isEmpty();
    }

    // ── findByUsernameAndPhone ───────────────────────────

    @Test
    @DisplayName("findByUsernameAndPhone: username + phone 모두 일치 → 사용자 반환")
    void findByUsernameAndPhone_match() {
        Optional<User> result = userRepository.findByUsernameAndPhone("testuser", "01012345678");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findByUsernameAndPhone: phone 불일치 → 빈 Optional")
    void findByUsernameAndPhone_phoneMismatch() {
        Optional<User> result = userRepository.findByUsernameAndPhone("testuser", "01099999999");

        assertThat(result).isEmpty();
    }

    // ── existsByUsername ─────────────────────────────────

    @Test
    @DisplayName("existsByUsername: 존재하는 username → true")
    void existsByUsername_true() {
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
    }

    @Test
    @DisplayName("existsByUsername: 존재하지 않는 username → false")
    void existsByUsername_false() {
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }
}
