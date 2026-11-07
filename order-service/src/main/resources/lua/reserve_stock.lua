-- Atomically take `qty` units from the available-stock counter and record a per-order hold.
-- KEYS[1] = stock counter       stock:{ticketId}
-- KEYS[2] = hold of this order  stock:{ticketId}:hold:{orderId}
-- ARGV[1] = quantity, ARGV[2] = hold ttl in seconds
-- returns  1 = reserved, 0 = out of stock, -1 = counter not warmed, 2 = this order already holds stock
if redis.call('EXISTS', KEYS[2]) == 1 then
    return 2
end
local stock = redis.call('GET', KEYS[1])
if not stock then
    return -1
end
local qty = tonumber(ARGV[1])
if tonumber(stock) < qty then
    return 0
end
redis.call('DECRBY', KEYS[1], qty)
redis.call('SET', KEYS[2], qty, 'EX', tonumber(ARGV[2]))
return 1
