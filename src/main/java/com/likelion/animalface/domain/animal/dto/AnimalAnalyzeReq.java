package com.likelion.animalface.domain.animal.dto;

import com.likelion.animalface.domain.animal.entity.AnimalResult;
import com.likelion.animalface.domain.animal.entity.AnimalType;
import com.likelion.animalface.domain.user.entity.User;

public record AnimalAnalyzeReq(
        String imageKey
) {}