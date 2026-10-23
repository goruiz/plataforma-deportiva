package com.platform.backend.modules.menu.presentation.requests.ReorderMenusRequest;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Puts the listed items, in this order, inside one parent: reordering what is already there, or moving an item in from
 * another parent. The children the parent already has that are not listed follow after them, in their current order.
 */
@Data
public class ReorderMenusRequest {

    // Null means the top level of the menu.
    private UUID idParentMenu;

    @NotEmpty
    private List<@NotNull UUID> menuIds;
}
