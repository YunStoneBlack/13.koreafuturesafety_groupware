package com.kfsc21c.groupware.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee ORDER BY u.username")
    List<User> findAllWithEmployee();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee WHERE u.id = :id")
    Optional<User> findByIdWithEmployee(Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee WHERE u.username = :username")
    Optional<User> findByUsernameWithEmployee(String username);
}
