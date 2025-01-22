package org.myProject.focus.flow.service.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.store.entities.enums.Layouts;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskStateDto {
    @NonNull
    Long id;
    
    @NonNull
    String name;

    @NonNull
    @JsonProperty("type_of_layout")
    Layouts typeOfLayout;

    @JsonProperty("left_task_state_id")
    Long leftTaskStateId;

    @JsonProperty("right_task_state_id")
    Long rightTaskStateId;

    @NonNull
    @JsonProperty("created_at")
    LocalDateTime createdAt;

    @NonNull
    List<TaskDto> tasks;

}
