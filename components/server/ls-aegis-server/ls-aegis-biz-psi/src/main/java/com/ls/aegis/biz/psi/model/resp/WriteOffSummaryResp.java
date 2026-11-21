package com.ls.aegis.biz.psi.model.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WriteOffSummaryResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long writeOffId;
    private LocalDateTime writeOffDate;
    private BigDecimal amount;
    private Boolean reversed;
    private String reason;
}
