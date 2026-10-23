package com.platform.backend.modules.menu.presentation.requests.SaveRoleMenusRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SaveRoleMenusRequest {

    /** Every menu item the role will see: the ones not listed stop being visible to it (an empty list hides them all). */
    @NotNull
    private List<@NotNull UUID> menuIds;
}
