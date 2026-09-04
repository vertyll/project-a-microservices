-- Record of every message this service was asked to send.
--
-- Kept after delivery: when a user reports never receiving an activation mail, the answer is
-- either here or in the SMTP server's log, and only one of those is ours to read.

CREATE TABLE email_log (
    id BIGSERIAL PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    variables VARCHAR(4000) NULL,
    reply_to VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL
);
CREATE INDEX idx_email_log_recipient ON email_log (recipient);
CREATE INDEX idx_email_log_status ON email_log (status);
CREATE INDEX idx_email_log_created_at ON email_log (created_at);
