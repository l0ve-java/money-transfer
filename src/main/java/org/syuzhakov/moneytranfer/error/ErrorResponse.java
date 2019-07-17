package org.syuzhakov.moneytranfer.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    @JsonIgnore
    private Integer httpStatus;
    private Integer errorCode;
    private String message;
}
