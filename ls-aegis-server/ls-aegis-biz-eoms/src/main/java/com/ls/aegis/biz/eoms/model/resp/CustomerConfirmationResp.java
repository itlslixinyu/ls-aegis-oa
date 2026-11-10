package com.ls.aegis.biz.eoms.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/** Customer acknowledgement only; it never confirms settlement or records a receipt. */
@Data
public class CustomerConfirmationResp {
    private boolean confirmed;
    private String orderId;
    private String customerId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmedAt;
    private String reason;
}
