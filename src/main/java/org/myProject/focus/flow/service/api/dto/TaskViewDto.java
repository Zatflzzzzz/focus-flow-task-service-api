package org.myProject.focus.flow.service.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.store.entities.enums.Layouts;
import org.myProject.focus.flow.service.store.entities.enums.Priority;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskViewDto {
    @NonNull
    Long id;
    
    @NonNull
    String name;

    @NonNull
    @JsonProperty("type_of_layout")
    Layouts typeOfLayout;

    @NonNull
    Long ordinal;

    @NonNull
    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
