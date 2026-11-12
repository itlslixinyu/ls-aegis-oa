package com.ls.aegis.biz.eoms.domain.h5;

import java.util.Date;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/** Public pool summary; customer contacts and attachments stay behind the claimed task. */
@Data
public class EngineerPoolItem {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applyId;
    private String repairNo;
    private String faultType;
    private String faultDesc;
    private String urgentLevel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;
}
