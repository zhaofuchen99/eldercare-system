-- =====================================================================
-- 给测试会员加积分（用于测试体检预约消费 / 取消退还 / 积分明细）
-- 目标账号：id=2（13800138000）
-- 作用：把该会员积分置为 5000，差额补一条 ADMIN_ADJUST 获得流水，
--       remain_amount 与 user.points 保持一致（对账不出现缺口）。
-- 幂等：当前积分 ≥ 5000 时不调整。可重复执行。
-- 执行方式：在 Navicat / IDEA Database / 命令行 mysql 中整段执行。
-- =====================================================================

START TRANSACTION;

SET @target = 5000;
SET @cur = (SELECT COALESCE(points, 0) FROM `user` WHERE `id` = 2);
SET @delta = @target - @cur;

-- 调整余额（当前积分不足目标值时才更新）
UPDATE `user` SET `points` = @target WHERE `id` = 2 AND @delta > 0;

-- 补差额获得流水（仅当有差额时插入）
INSERT INTO `point_transaction`
  (`user_id`, `type`, `change_amount`, `balance_after`, `remain_amount`,
   `expire_time`, `description`, `ref_id`, `create_time`, `update_time`, `deleted`)
SELECT
  2, 'ADMIN_ADJUST', @delta, @target, @delta,
  DATE_ADD(NOW(), INTERVAL 1 YEAR), '测试调增积分', NULL, NOW(), NOW(), 0
FROM DUAL
WHERE @delta > 0;

COMMIT;

-- 执行后查看结果
SELECT id, phone, points FROM `user` WHERE id = 2;
SELECT type, change_amount, balance_after, description, create_time
FROM `point_transaction` WHERE user_id = 2 ORDER BY id DESC LIMIT 5;
