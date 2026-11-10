package com.ls.aegis.biz.psi.model.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 核销开口下拉
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenItemOptionResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long value;
    private String label;
    private BigDecimal unwrittenAmount;
    private String sourceNo;
    private String sourceType;
}
