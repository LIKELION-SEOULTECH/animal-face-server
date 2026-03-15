package com.likelion.animalface.domain.user.repositry;

import com.likelion.animalface.domain.animal.entity.AnimalResult;
import com.likelion.animalface.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    Optional<User> findByUsernameAndPhone(String username, String phone);

    boolean existsByUsername(String username);
}
