-- This service starts no saga and compensates no step of one: it only reacts to
-- events other contexts publish. The two tables were provisioned with the shared
-- messaging schema and never held a row. The outbox keeps its saga_id, which is a
-- correlation value copied from an inbound event, not a log of this service's own saga.
DROP TABLE IF EXISTS saga_step;
DROP TABLE IF EXISTS saga;
