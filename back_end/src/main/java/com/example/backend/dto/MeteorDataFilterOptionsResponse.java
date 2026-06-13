package com.example.backend.dto;

import java.util.List;

public record MeteorDataFilterOptionsResponse(
        List<MeteorDataTaskOption> tasks,
        List<MeteorDataStationOption> stations,
        List<String> elements
) {
}

