package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.AccountStatus;
import com.talenthub.recruitment.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);

    @Query(value = """
        SELECT * FROM users u
        WHERE (:roleId IS NULL OR u.role_id = :roleId)
          AND (:status IS NULL OR u.status = CAST(:status AS account_status))
          AND (:keyword IS NULL OR :keyword = '' OR
               LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               u.phone LIKE CONCAT('%', :keyword, '%'))
        ORDER BY u.created_at DESC
        """, nativeQuery = true)
    List<User> search(
            @Param("keyword") String keyword,
            @Param("roleId") Integer roleId,
            @Param("status") String status
    );
}