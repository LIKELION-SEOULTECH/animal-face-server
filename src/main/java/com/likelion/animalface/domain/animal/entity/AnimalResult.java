package com.likelion.animalface.domain.animal.entity;

import com.likelion.animalface.global.common.BaseTimeEntity;
import com.likelion.animalface.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "animal_results")
@Entity
public class AnimalResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimalType animalType;

    // S3 버킷 내의 파일 경로 (예: animal/uuid_filename.png)
    @Column(nullable = false)
    private String imageKey;

    private Double score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public static AnimalResult create(User user, String imageKey, AnimalType type, Double score) {
        return AnimalResult.builder()
                .user(user)
                .imageKey(imageKey)
                .animalType(type)
                .score(score)
                .build();
    }
}
