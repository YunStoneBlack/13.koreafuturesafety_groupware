package com.kfsc21c.groupware.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    @Query("SELECT a FROM ApprovalRequest a JOIN FETCH a.requester LEFT JOIN FETCH a.approver ORDER BY a.createdAt DESC")
    List<ApprovalRequest> findAllWithUsers();

    @Query("SELECT a FROM ApprovalRequest a JOIN FETCH a.requester LEFT JOIN FETCH a.approver WHERE a.id = :id")
    Optional<ApprovalRequest> findByIdWithUsers(Long id);
}
