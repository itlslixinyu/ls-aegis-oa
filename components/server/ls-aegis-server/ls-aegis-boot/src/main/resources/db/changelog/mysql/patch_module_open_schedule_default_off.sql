-- liquibase formatted sql

-- changeset ls:module_open_schedule_default_off
-- comment 应用管理、任务调度产品开关默认改为关闭；任务调度侧栏仅由本开关控制（不再被 snail-job 连带隐藏）
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `default_value` = '0',
    `value` = '0',
    `description` = '默认关闭。开启后显示能力开放/应用管理菜单；关闭则隐藏菜单并拦截相关接口。'
WHERE `code` = 'MODULE_OPEN_ENABLED';

UPDATE `sys_option`
SET `default_value` = '0',
    `value` = '0',
    `description` = '默认关闭。开启后显示任务调度侧栏菜单；关闭则隐藏菜单并拦截相关接口。实际执行任务仍须 snail-job.enabled=true 并启动调度中心。'
WHERE `code` = 'MODULE_SCHEDULE_ENABLED';
