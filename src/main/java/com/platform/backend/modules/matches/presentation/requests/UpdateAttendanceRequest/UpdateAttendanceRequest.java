package com.platform.backend.modules.matches.presentation.requests.UpdateAttendanceRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAttendanceRequest {

    /** {@code CONFIRMED} or {@code DECLINED} (see {@code AttendanceStatus}); the service checks the value. */
    @NotBlank
    @Size(max = 20)
    private String status;
}
