package ru.yandex.practicum.dal.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.dal.model.enums.ConditionOperation;
import ru.yandex.practicum.dal.model.enums.ConditionType;

@Entity
@Table(name = "conditions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ConditionType type;

    @Enumerated(EnumType.STRING)
    private ConditionOperation operation;

    private Integer value;
}