package com.chronos.infrastructure.kafka;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class OutboxPublisher {
    private final JdbcClient jdbc; private final KafkaTemplate<String,String> kafka;
    public OutboxPublisher(JdbcClient jdbc, KafkaTemplate<String,String> kafka) { this.jdbc=jdbc; this.kafka=kafka; }

    @Scheduled(fixedDelayString = "${chronos.outbox.poll-ms:250}")
    @Transactional
    public void publish() {
        List<Row> rows = jdbc.sql("""
            SELECT outbox_id,topic,event_key,payload::text AS payload
              FROM chronos.event_outbox
             WHERE published_at IS NULL
             ORDER BY outbox_id
             LIMIT 100
             FOR UPDATE SKIP LOCKED
            """).query((rs,n)->new Row(rs.getLong("outbox_id"),rs.getString("topic"),rs.getString("event_key"),rs.getString("payload"))).list();
        for (Row row: rows) {
            try {
                kafka.send(row.topic(), row.key(), row.payload()).get();
                jdbc.sql("UPDATE chronos.event_outbox SET published_at=NOW() WHERE outbox_id=:id AND published_at IS NULL")
                        .param("id", row.id()).update();
            } catch (Exception e) {
                jdbc.sql("UPDATE chronos.event_outbox SET retry_count=retry_count+1,last_error=:err WHERE outbox_id=:id")
                        .param("err", abbreviate(e.toString(), 2000)).param("id", row.id()).update();
                break;
            }
        }
    }
    private static String abbreviate(String s,int max){ return s.length()<=max?s:s.substring(0,max); }
    private record Row(long id,String topic,String key,String payload){}
}
