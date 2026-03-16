package com.likelion.animalface.domain.animal.dto;

import com.likelion.animalface.domain.animal.entity.AnimalResult;

public record AnimalResultRes(
        Long id,
        String animalName,
        String imageUrl
) {
    public static AnimalResultRes from(AnimalResult animalResult, String imageUrl) {
        return new AnimalResultRes(
                animalResult.getId(),
                animalResult.getAnimalType().name(),
                imageUrl
        );
    }
}
