-- 参数列表
local voucherId = ARGV[1]
local userId = ARGV[2]
local nowTime = ARGV[3]         -- 当前时间戳
local beginTime = ARGV[4]      -- 秒杀开始时间戳
local endTime = ARGV[5]        -- 秒杀结束时间戳
local orderId = ARGV[6]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 1. 判断秒杀是否开始
if tonumber(nowTime) < tonumber(beginTime) then
    return 3  -- 未开始
end

-- 2. 判断秒杀是否结束
if tonumber(nowTime) > tonumber(endTime) then
    return 4  -- 已结束
end

-- 3. 判断库存是否充足
if tonumber(redis.call('get', stockKey)) <= 0 then
    return 1  -- 库存不足
end

-- 4. 判断用户是否已经下单
if redis.call('sismember', orderKey, userId) == 1 then
    return 2  -- 重复下单
end

-- 5. 扣库存 + 下单
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)

return 0  -- 成功