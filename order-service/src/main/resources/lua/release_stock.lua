-- Give back the stock held by one order. Idempotent: the hold key is deleted on first
-- release, so duplicate cancel/expire/fail signals never add stock twice.
-- KEYS[1] = stock counter, KEYS[2] = hold of this order
-- returns 1 = released, 0 = nothing held
local qty = redis.call('GET', KEYS[2])
if not qty then
    return 0
end
redis.call('DEL', KEYS[2])
-- if the counter was evicted, do not recreate it with a partial value: the next warm-up
-- reloads it from the database, which is the source of truth
if redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('INCRBY', KEYS[1], tonumber(qty))
end
return 1
