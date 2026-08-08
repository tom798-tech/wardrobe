package com.tom.wardrobe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_outbox")
public class OrderOutboxEvent {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENT = 1;
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("aggregate_type")
    private String aggregateType;

    @TableField("aggregate_id")
    private Long aggregateId;

    @TableField("event_type")
    private String eventType;

    private String payload;

    private Integer status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    @TableField("last_error")
    private String lastError;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
