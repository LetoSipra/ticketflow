-- KEYS[1] = lock_key
-- ARGV[1] = request_id

-- 1. Check if the lock exists AND if the value matches MY request_id
if redis.call('get', KEYS[1]) == ARGV[1] then
    -- 2. If yes, delete it
    return redis.call('del', KEYS[1])
else
    -- 3. If no, it's not my lock (or it expired). Do nothing.
    return 0
end