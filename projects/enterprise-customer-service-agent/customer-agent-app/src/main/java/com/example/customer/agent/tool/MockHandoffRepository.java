package com.example.customer.agent.tool;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Repository;

/**
 * 本地人工转接仓储。
 * <p>
 * 使用内存集合记录转人工请求，避免 Day 13 引入外部派单或数据库写入。
 *
 * @author jiangzhibin
 * @since 2026-06-29 15:00:00
 */
@Repository
public class MockHandoffRepository {

    private final AtomicInteger sequence = new AtomicInteger(1000);
    private final Queue<HandoffRecord> records = new ConcurrentLinkedQueue<>();

    /**
     * 创建本地人工转接记录。
     *
     * @param tenantId 租户标识
     * @param conversationId 会话标识
     * @param reason 转接原因
     * @param orderId 可选订单号
     * @return 转接记录
     */
    public HandoffRecord create(String tenantId, String conversationId, String reason, Optional<String> orderId) {
        var record = new HandoffRecord(
                "handoff-" + sequence.incrementAndGet(),
                tenantId,
                conversationId,
                reason,
                orderId,
                HandoffStatus.CREATED,
                Instant.now());
        records.add(record);
        return record;
    }

    /**
     * 返回全部本地转接记录。
     *
     * @return 转接记录快照
     */
    public List<HandoffRecord> findAll() {
        return List.copyOf(records);
    }
}
