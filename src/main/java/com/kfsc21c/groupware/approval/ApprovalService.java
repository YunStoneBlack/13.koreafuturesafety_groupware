package com.kfsc21c.groupware.approval;

import com.kfsc21c.groupware.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;

    private ApprovalRequest get(Long id) {
        return approvalRequestRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new IllegalArgumentException("결재 요청을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public void submit(Long id, User requester) {
        ApprovalRequest req = get(id);
        if (!req.getRequester().getId().equals(requester.getId())) {
            throw new AccessDeniedException("본인이 작성한 요청만 제출할 수 있습니다");
        }
        if (req.getStatus() != ApprovalStatus.DRAFT) {
            throw new IllegalStateException("초안 상태에서만 제출할 수 있습니다");
        }
        req.setStatus(ApprovalStatus.SUBMITTED);
        approvalRequestRepository.save(req);
    }

    @Transactional
    public void decide(Long id, User approver, ApprovalStatus decision, String comment) {
        if (decision != ApprovalStatus.APPROVED && decision != ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("승인 또는 반려만 가능합니다");
        }
        ApprovalRequest req = get(id);
        if (req.getStatus() != ApprovalStatus.SUBMITTED) {
            throw new IllegalStateException("제출된 요청만 승인/반려할 수 있습니다");
        }
        req.setStatus(decision);
        req.setApprover(approver);
        req.setApproverComment(comment);
        req.setDecidedAt(LocalDateTime.now());
        approvalRequestRepository.save(req);
    }

    @Transactional
    public void deleteDraft(Long id, User requester) {
        ApprovalRequest req = get(id);
        if (!req.getRequester().getId().equals(requester.getId())) {
            throw new AccessDeniedException("본인이 작성한 요청만 삭제할 수 있습니다");
        }
        if (req.getStatus() != ApprovalStatus.DRAFT) {
            throw new IllegalStateException("초안 상태에서만 삭제할 수 있습니다");
        }
        approvalRequestRepository.delete(req);
    }
}
