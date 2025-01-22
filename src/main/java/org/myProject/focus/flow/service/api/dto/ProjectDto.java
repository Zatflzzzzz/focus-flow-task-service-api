package org.myProject.focus.flow.service.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProjectDto {
    @NonNull
    Long id;

    @NonNull
    @JsonProperty("user_id")
    Long userId;

    @NonNull
    String name;

    @NonNull
    @JsonProperty("create_at")
    LocalDateTime createdAt;

    @NonNull
    @JsonProperty("update_at")
    LocalDateTime updatedAt;
}
