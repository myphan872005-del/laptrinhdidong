CREATE TABLE IF NOT EXISTS `custom_maps_db`.`users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `display_name` VARCHAR(255) NULL DEFAULT NULL,
  `avatar_url` TEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username` (`username` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 8
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci

CREATE TABLE IF NOT EXISTS `custom_maps_db`.`journeys` (
  `id` BIGINT NOT NULL,
  `user_id` INT NOT NULL,
  `local_id` BIGINT NULL DEFAULT NULL,
  `title` VARCHAR(255) NOT NULL,
  `start_lat` DOUBLE NULL DEFAULT '0',
  `start_lon` DOUBLE NULL DEFAULT '0',
  `start_time` BIGINT NULL DEFAULT NULL,
  `is_deleted` TINYINT(1) NULL DEFAULT '0',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` BIGINT NULL DEFAULT NULL,
  `isPublic` TINYINT(1) NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_local` (`user_id` ASC, `local_id` ASC) VISIBLE,
  UNIQUE INDEX `unique_user_local` (`user_id` ASC, `local_id` ASC) VISIBLE,
  CONSTRAINT `journeys_ibfk_1`
    FOREIGN KEY (`user_id`)
    REFERENCES `custom_maps_db`.`users` (`id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci

CREATE TABLE IF NOT EXISTS `custom_maps_db`.`track_points` (
  `id` BIGINT NOT NULL,
  `journey_id` BIGINT NULL DEFAULT NULL,
  `segment_id` BIGINT NULL DEFAULT NULL,
  `latitude` DOUBLE NOT NULL,
  `longitude` DOUBLE NOT NULL,
  `timestamp` BIGINT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_tp_journey_time` (`journey_id` ASC, `timestamp` ASC) VISIBLE,
  CONSTRAINT `track_points_ibfk_1`
    FOREIGN KEY (`journey_id`)
    REFERENCES `custom_maps_db`.`journeys` (`id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci

CREATE TABLE IF NOT EXISTS `custom_maps_db`.`stop_points` (
  `id` BIGINT NOT NULL,
  `journey_id` BIGINT NULL DEFAULT NULL,
  `local_id` BIGINT NULL DEFAULT NULL,
  `latitude` DOUBLE NOT NULL,
  `longitude` DOUBLE NOT NULL,
  `note` TEXT NULL DEFAULT NULL,
  `thumbnail_uri` VARCHAR(500) NULL DEFAULT NULL,
  `timestamp` BIGINT NULL DEFAULT NULL,
  `is_deleted` TINYINT NULL DEFAULT '0',
  `updated_at` BIGINT NULL DEFAULT NULL,
  `server_updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `local_id` (`local_id` ASC) VISIBLE,
  UNIQUE INDEX `local_id_2` (`local_id` ASC) VISIBLE,
  INDEX `journey_id` (`journey_id` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci

CREATE TABLE IF NOT EXISTS `custom_maps_db`.`stop_point_media` (
  `id` BIGINT NOT NULL,
  `stop_point_id` BIGINT NOT NULL,
  `local_id` BIGINT NULL DEFAULT NULL,
  `file_uri` VARCHAR(500) NOT NULL,
  `media_type` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `local_id` (`local_id` ASC) VISIBLE,
  UNIQUE INDEX `local_id_2` (`local_id` ASC) VISIBLE,
  INDEX `fk_stop_media_local` (`stop_point_id` ASC) VISIBLE,
  CONSTRAINT `fk_stop_media_local`
    FOREIGN KEY (`stop_point_id`)
    REFERENCES `custom_maps_db`.`stop_points` (`local_id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci

CREATE TABLE IF NOT EXISTS `custom_maps_db`.`discovery_posts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `original_journey_id` BIGINT NULL DEFAULT NULL,
  `title` VARCHAR(255) NULL DEFAULT NULL,
  `thumbnail_uri` TEXT NULL DEFAULT NULL,
  `payload` LONGTEXT NULL DEFAULT NULL,
  `likes_count` INT NULL DEFAULT '0',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `user_id` (`user_id` ASC) VISIBLE,
  CONSTRAINT `discovery_posts_ibfk_1`
    FOREIGN KEY (`user_id`)
    REFERENCES `custom_maps_db`.`users` (`id`)
    ON DELETE CASCADE)
ENGINE = InnoDB
AUTO_INCREMENT = 15
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci