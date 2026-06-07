package com.example.DocumentManagement.dto.response;

import com.example.DocumentManagement.entity.WorkflowHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class WorkflowHistoryResponse {

    private Long id;
    private String fromStatus;
    private String toStatus;
    private String comment;
    private UUID performedById;
    private String performedByName;
    private LocalDateTime createdAt;

    public static WorkflowHistoryResponse from(WorkflowHistory wh) {
        return new WorkflowHistoryResponse(
                wh.getId(),
                wh.getFromStatus() != null ? wh.getFromStatus().name() : null,
                wh.getToStatus().name(),
                wh.getComment(),
                wh.getPerformedBy().getId(),
                wh.getPerformedBy().getFullName(),
                wh.getCreatedAt()
        );
    }
}
