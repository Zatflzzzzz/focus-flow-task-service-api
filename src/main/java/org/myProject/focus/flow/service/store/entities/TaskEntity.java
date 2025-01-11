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
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "task")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;

    @Column(nullable = false, unique = true)
    String title;

    String description;

    LocalDateTime deadline;

    Category category;

    Priority priority;

    LocalDateTime updatedAt;

    LocalDateTime createdAt;
}
