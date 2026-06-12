package com.example.DocumentManagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TransferOwnershipRequest {

    @NotNull
    private UUID newOwnerUserId;
}
