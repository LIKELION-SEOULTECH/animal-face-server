package com.likelion.animalface.domain.animal.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalType {
    DOG("강아지상"),
    CAT("고양이상"),
    FOX("여우상");

    private final String name;
}
