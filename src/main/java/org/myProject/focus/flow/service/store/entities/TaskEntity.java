package org.myProject.focus.flow.service.store.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.store.entities.enums.Category;
import org.myProject.focus.flow.service.store.entities.enums.Priority;

import java.time.LocalDateTime;

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

    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}
