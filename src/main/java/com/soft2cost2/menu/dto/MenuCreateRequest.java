package com.soft2cost2.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @Positive Long parentMenuId,

        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "[A-Za-z0-9_]+")
        String menuCode,

        @NotBlank @Size(max = 100)
        String menuName,

        @NotBlank
        @Pattern(regexp = "GROUP|SCREEN|POPUP")
        String menuType,

        @Size(max = 100)
        String screenId,

        @Size(max = 300)
        String formPath,

        @Size(max = 100)
        String iconName,

        @NotNull @PositiveOrZero
        Integer sortOrder,

        @NotBlank
        @Pattern(regexp = "Y|N")
        String sensitiveYn
) {
}
