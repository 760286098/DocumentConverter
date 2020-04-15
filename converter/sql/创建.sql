/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50722
 Source Host           : localhost:3306
 Source Schema         : converter

 Target Server Type    : MySQL
 Target Server Version : 50722
 File Encoding         : 65001

 Date: 02/04/2020 19:25:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for convert_info
-- ----------------------------
DROP TABLE IF EXISTS `convert_info`;
CREATE TABLE `convert_info`
(
    `id`             int(11)                                                 NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `source_path`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '源文件路径',
    `target_path`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '目的文件路径',
    `file_size`      varchar(15) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '源文件大小',
    `join_time`      bigint(20)                                              NOT NULL COMMENT '任务加入队列时间',
    `start_time`     bigint(20)                                              NOT NULL COMMENT '任务正式开始时间',
    `end_time`       bigint(20)                                              NOT NULL COMMENT '任务结束时间',
    `convert_status` varchar(15) CHARACTER SET utf8 COLLATE utf8_general_ci  NOT NULL COMMENT '任务状态（实际只储存已完成或出错任务）',
    `retry`          int(11)                                                 NOT NULL COMMENT '任务重试次数',
    `exceptions`     text CHARACTER SET utf8 COLLATE utf8_general_ci         NULL COMMENT '任务产生的异常',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci
  ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
