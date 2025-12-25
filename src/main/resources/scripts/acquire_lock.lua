-- KEYS[1] = lock_key (e.g., "lock:ticket:1")
-- ARGV[1] = owner_id (e.g., unique identifier for the lock owner)
-- ARGV[2] = expiration_time (in milliseconds)

-- 1. Try to set the key ONLY if it doesn't exist (NX)
if redis.call('set', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
    return 1 -- Success! You got the lock.
else
    return 0 -- Failed. Someone else has it.
end