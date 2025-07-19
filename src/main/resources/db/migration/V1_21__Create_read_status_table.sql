-- Create read_status table to track each user's last read message in a conversation
CREATE TABLE read_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL, -- 用户在此对话中已读的最后一条消息ID
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(user_id, conversation_id) -- 同样，一个用户在一个对话中只有一个已读状态。通过用户在此对话中已读的最后一条消息ID来实现统计会话中有多少未读的消息。
);

-- Add indexes for better query performance
CREATE INDEX idx_read_status_user ON read_status(user_id);
CREATE INDEX idx_read_status_conversation ON read_status(conversation_id);
CREATE INDEX idx_read_status_last_read_message ON read_status(last_read_message_id);

-- Add foreign key constraints
ALTER TABLE read_status
    ADD CONSTRAINT fk_read_status_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE read_status
    ADD CONSTRAINT fk_read_status_conversation
    FOREIGN KEY (conversation_id) REFERENCES conversations(id);

ALTER TABLE read_status
    ADD CONSTRAINT fk_read_status_message
    FOREIGN KEY (last_read_message_id) REFERENCES messages(id); 