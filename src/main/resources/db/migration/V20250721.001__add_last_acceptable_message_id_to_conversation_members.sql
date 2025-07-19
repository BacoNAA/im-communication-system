-- Add last_acceptable_message_id column to conversation_members table
ALTER TABLE `conversation_members`
ADD COLUMN `last_acceptable_message_id` BIGINT NULL COMMENT '可接受的最新消息ID，超过此ID的消息不会显示给该用户' AFTER `last_read_message_id`; 