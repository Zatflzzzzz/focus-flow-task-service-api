package org.myProject.focus.flow.service.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.store.entities.enums.Category;
import org.myProject.focus.flow.service.store.entities.enums.Priority;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskDto {
    @NonNull
    Long id;

    @NonNull
    String title;

    @NonNull
    String description;

    @NonNull
    LocalDateTime deadline;

    @NonNull
    Category category;

    @NonNull
    Priority priority;

    @JsonProperty("higher_priority_task_id")
    Long higherPriorityTaskId;

    @JsonProperty("lower_priority_task_id")
    Long lowerPriorityTaskId;

    @NonNull
    @JsonProperty("updated_at")
    LocalDateTime updatedAt;

    @NonNull
    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
