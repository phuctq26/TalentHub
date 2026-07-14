package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.talenthub.recruitment.entity.enums.AccountStatus;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    @Query(value = "SELECT u.* FROM users u JOIN role r ON r.id = u.role_id WHERE r.name = :roleName AND CAST(u.status AS TEXT) = :statusStr ORDER BY u.full_name ASC", nativeQuery = true)
    List<User> findByRoleNameAndStatus(@Param("roleName") String roleName, @Param("statusStr") String statusStr);
    @Query(
        value = """
            SELECT u.* FROM users u
            LEFT JOIN role r ON r.id = u.role_id
            WHERE (:roleId IS NULL OR u.role_id = :roleId)
              AND (:statusStr IS NULL OR CAST(u.status AS TEXT) = :statusStr)
              AND (:keyword IS NULL OR :keyword = '' OR
                   LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   u.phone LIKE CONCAT('%', :keyword, '%'))
            ORDER BY u.created_at DESC
            """,
        countQuery = """
            SELECT COUNT(u.id) FROM users u
            LEFT JOIN role r ON r.id = u.role_id
            WHERE (:roleId IS NULL OR u.role_id = :roleId)
              AND (:statusStr IS NULL OR CAST(u.status AS TEXT) = :statusStr)
              AND (:keyword IS NULL OR :keyword = '' OR
                   LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   u.phone LIKE CONCAT('%', :keyword, '%'))
            """,
        nativeQuery = true
    )
    Page<User> search(
            @Param("keyword") String keyword,
            @Param("roleId") Integer roleId,
            @Param("statusStr") String statusStr,
            Pageable pageable
    );

    @Query(
        value = "SELECT COUNT(u.id) FROM users u JOIN role r ON r.id = u.role_id WHERE r.name = 'ADMIN' AND CAST(u.status AS TEXT) IN :statuses",
        nativeQuery = true
    )
    long countActiveOrLockedAdmins(@Param("statuses") List<String> statuses);

    @Query(value = "SELECT COUNT(*) FROM users WHERE CAST(status AS TEXT) = 'LOCKED'", nativeQuery = true)
    long countLockedAccounts();

    @Query(value = "SELECT r.name, COUNT(u.id) FROM role r LEFT JOIN users u ON u.role_id = r.id GROUP BY r.name", nativeQuery = true)
    List<Object[]> countUsersGroupByRole();
}