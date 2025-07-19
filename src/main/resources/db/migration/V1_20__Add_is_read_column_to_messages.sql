-- Add is_read column to messages table
ALTER TABLE messages ADD COLUMN is_read BOOLEAN DEFAULT FALSE NOT NULL;

-- Create index for is_read column
CREATE INDEX idx_message_is_read ON messages(is_read);
 
-- Update existing messages where status is READ to have is_read = true
UPDATE messages SET is_read = TRUE WHERE status = 'READ'; 