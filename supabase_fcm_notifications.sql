-- ==============================================================================
-- NAS Rally - FCM In-App & Out-of-App Push Notification Database Setup
-- ==============================================================================

-- 1. Create dedicated Queue Table for In-App Messaging & FCM Dispatch
CREATE TABLE IF NOT EXISTS public.fcm_notifications_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'pending',
    retry_count INT4 NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fcm_queue_status_created
ON public.fcm_notifications_queue(status, created_at);

-- Enable Row Level Security (RLS) on fcm_notifications_queue
ALTER TABLE public.fcm_notifications_queue ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow service_role full access on fcm_notifications_queue"
ON public.fcm_notifications_queue
FOR ALL TO service_role
USING (true)
WITH CHECK (true);

-- 2. Unified Push Trigger Function for both notifications and fcm_notifications_queue
CREATE OR REPLACE FUNCTION public.notify_push()
RETURNS TRIGGER AS $$
DECLARE
    v_notification_type TEXT;
BEGIN
    IF TG_TABLE_NAME = 'notifications' THEN
        v_notification_type := 'out_of_app';
    ELSE
        v_notification_type := 'in_app';
    END IF;

    PERFORM net.http_post(
        url := 'https://api-nas-rally.mayflower-paradise.us/functions/v1/push',
        headers := jsonb_build_object(
            'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoic2VydmljZV9yb2xlIiwiaXNzIjoic3VwYWJhc2UiLCJpYXQiOjE3ODAzNTU1MTUsImV4cCI6MTkzODAzNTUxNX0.iLKkNlhS25LlJLgz5IxnbYDX-wwnBQdtaWM_OJl3k7w',
            'Content-Type', 'application/json'
        ),
        body := jsonb_build_object(
            'type', 'INSERT',
            'table', TG_TABLE_NAME,
            'notification_type', v_notification_type,
            'record', row_to_json(NEW)
        )
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Attach notify_push trigger to public.notifications
DROP TRIGGER IF EXISTS tr_push_notifications ON public.notifications;
CREATE TRIGGER tr_push_notifications
AFTER INSERT ON public.notifications
FOR EACH ROW EXECUTE FUNCTION public.notify_push();

-- Attach notify_push trigger to public.fcm_notifications_queue
DROP TRIGGER IF EXISTS tr_push_fcm_queue ON public.fcm_notifications_queue;
CREATE TRIGGER tr_push_fcm_queue
AFTER INSERT ON public.fcm_notifications_queue
FOR EACH ROW EXECUTE FUNCTION public.notify_push();

-- 3. Message Trigger Function: Inserts into public.notifications AND public.fcm_notifications_queue
CREATE OR REPLACE FUNCTION public.notify_new_message()
RETURNS TRIGGER AS $$
DECLARE
    sender_name TEXT;
    group_name TEXT;
BEGIN
    SELECT name INTO sender_name FROM public.profiles WHERE id = NEW.sender_id;
    SELECT name INTO group_name FROM public.groups WHERE id = NEW.group_id;

    BEGIN
        -- A. Out-of-App Notification Insertion
        INSERT INTO public.notifications (user_id, title, body, data)
        SELECT
            gm.user_id,
            COALESCE(group_name, 'New Message'),
            COALESCE(sender_name, 'Someone') || ': ' || substring(COALESCE(NEW.content, '') from 1 for 100),
            jsonb_build_object(
                'type', 'message',
                'group_id', NEW.group_id,
                'message_id', NEW.id,
                'notification_type', 'out_of_app'
            )
        FROM public.group_members gm
        WHERE gm.group_id = NEW.group_id AND gm.user_id != NEW.sender_id;

        -- B. In-App Notification Insertion
        INSERT INTO public.fcm_notifications_queue (user_id, title, body, data)
        SELECT
            gm.user_id,
            COALESCE(group_name, 'New Message'),
            COALESCE(sender_name, 'Someone') || ': ' || substring(COALESCE(NEW.content, '') from 1 for 100),
            jsonb_build_object(
                'type', 'message',
                'group_id', NEW.group_id,
                'message_id', NEW.id,
                'sender_name', COALESCE(sender_name, 'Someone'),
                'group_name', COALESCE(group_name, 'New Message'),
                'notification_type', 'in_app'
            )
        FROM public.group_members gm
        WHERE gm.group_id = NEW.group_id AND gm.user_id != NEW.sender_id;

    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'Failed to insert notification: %', SQLERRM;
    END;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Attach trigger on public.messages
DROP TRIGGER IF EXISTS tr_notify_new_message ON public.messages;
CREATE TRIGGER tr_notify_new_message
AFTER INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION public.notify_new_message();
