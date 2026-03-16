package com.likelion.animalface.domain.user.entity;

import com.likelion.animalface.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@Table(name = "users")
@Entity
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String phone;


    @Column(nullable = false)
    private String password;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
