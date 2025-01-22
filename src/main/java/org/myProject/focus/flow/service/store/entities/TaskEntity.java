package org.myProject.focus.flow.service.store.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.store.entities.enums.Category;
import org.myProject.focus.flow.service.store.entities.enums.Priority;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "task")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;

    String title;

    String description;

    LocalDateTime deadline;

    Category category;

    Priority priority;

    @OneToOne
    TaskEntity higherPriorityTask;

    @OneToOne
    TaskEntity lowerPriorityTask;

    @ManyToOne
    TaskStateEntity taskState;

    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    public Optional<TaskEntity> getHigherPriorityTask() {
        return Optional.ofNullable(higherPriorityTask);
    }

    public Optional<TaskEntity> getLowerPriorityTask() {
        return Optional.ofNullable(lowerPriorityTask);
    }
}
