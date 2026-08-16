# CHRONOS v0.1 architecture decisions

1. Modular monolith with explicit ports/adapters.
2. PostgreSQL is authoritative; Kafka and Redis are rebuildable.
3. Ingest writes EventStore and Outbox in one DB transaction.
4. gRPC is used for device/gateway ingest; REST is used for operator/query access.
5. Historical world state = latest snapshot <= target time + subsequent event replay.
6. Current world state is a Redis projection maintained from Kafka.
7. Domain module has no Spring dependency.
