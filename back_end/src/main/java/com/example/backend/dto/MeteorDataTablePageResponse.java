package com.example.backend.dto;

import java.util.List;

public record MeteorDataTablePageResponse(
        List<MeteorDataTableRecordResponse> records,
        long totalElements,
        int page,
        int size
) {
}

