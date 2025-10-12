-- 推荐库设置
-- CREATE DATABASE hotel CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- ========= users =========
CREATE TABLE if not exists users (
                       id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                       email        VARCHAR(255) NOT NULL,
                       name         VARCHAR(120) NOT NULL,
                       role         ENUM('guest','manager','admin') NOT NULL,
                       phone        INT NOT NULL,
                       created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= clients =========
CREATE TABLE if not exists clients (
                         id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                         name          VARCHAR(120) NOT NULL,
                         type          ENUM('OTA','HOTEL_WEBSITE','Internal') NOT NULL,
                         api_key_hash  VARCHAR(200) NOT NULL,
                         created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         UNIQUE KEY uq_clients_apikey (api_key_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= hotels =========
CREATE TABLE if not exists hotels (
                        id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        name          VARCHAR(120) NOT NULL,
                        brand         VARCHAR(120),
                        address_line1 VARCHAR(200) NOT NULL,
                        address_line2 VARCHAR(200),
                        city          VARCHAR(120) NOT NULL,
                        state         VARCHAR(120),
                        country       VARCHAR(120) NOT NULL,
                        postal_code   VARCHAR(20),
                        star_rating   DECIMAL(2,1) CHECK (star_rating BETWEEN 0 AND 5),
                        created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= room_types =========
CREATE TABLE if not exists room_types (
                            id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            hotel_id     BIGINT       NOT NULL,
                            code         VARCHAR(50)  NOT NULL,
                            name         VARCHAR(120) NOT NULL,
                            description  TEXT,
                            capacity     INT          NOT NULL,
                            bed_type     VARCHAR(50),
                            bed_num      INT,
                            bed_size_m2  DECIMAL(6,2),
                            base_rate    DECIMAL(12,2) NOT NULL,
                            ranking      INT,
                            total_rooms  INT          NOT NULL,
                            created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_room_types_hotel_code (hotel_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= rooms =========
CREATE TABLE if not exists rooms (
                       id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                       hotel_id     BIGINT     NOT NULL,
                       room_type_id BIGINT     NOT NULL,
                       room_number  VARCHAR(20)  NOT NULL,
                       floor        INT,
                       status       ENUM('available','maintenance','out_of_service') NOT NULL DEFAULT 'available',
                       created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       UNIQUE KEY uq_rooms_hotel_roomno (hotel_id, room_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= room_type_inventory =========
CREATE TABLE if not exists room_type_inventory (
                                     id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                     hotel_id     BIGINT       NOT NULL,
                                     room_type_id BIGINT       NOT NULL,
                                     stay_date    DATE         NOT NULL,
                                     total        INT          NOT NULL DEFAULT 0,
                                     reserved     INT          NOT NULL DEFAULT 0,
                                     blocked      INT          NOT NULL DEFAULT 0,
                                     available    INT          NOT NULL DEFAULT 0,
                                     updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     UNIQUE KEY uq_inv_hotel_type_date (hotel_id, room_type_id, stay_date),
                                     CHECK (total >= 0 AND reserved >= 0 AND blocked >= 0 AND available >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= room_type_daily_price =========
CREATE TABLE if not exists room_type_daily_price (
                                       id                     BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                       hotel_id               BIGINT       NOT NULL,
                                       room_type_id           BIGINT       NOT NULL,
                                       stay_date              DATE         NOT NULL,
                                       price                  DECIMAL(12,2) NOT NULL,
                                       occupancy_percentage   DECIMAL(5,2),
                                       computed_from          JSON,
                                       updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       UNIQUE KEY uq_price_hotel_type_date (hotel_id, room_type_id, stay_date),
                                       CHECK (occupancy_percentage IS NULL OR (occupancy_percentage >= 0 AND occupancy_percentage <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= loyalty_accounts =========
CREATE TABLE if not exists loyalty_accounts (
                                  id                 BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                  user_id            BIGINT       NOT NULL,
                                  tier               ENUM('silver','gold','platinum') NOT NULL,
                                  points             INT          NOT NULL DEFAULT 0,
                                  lifetime_nights    INT          NOT NULL DEFAULT 0,
                                  lifetime_spend_usd DECIMAL(12,2) NOT NULL DEFAULT 0,
                                  annual_nights      INT          NOT NULL DEFAULT 0,
                                  annual_spend_usd   DECIMAL(12,2) NOT NULL DEFAULT 0,
                                  last_stay_at       DATE,
                                  UNIQUE KEY uq_loyalty_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= reservations =========
CREATE TABLE if not exists reservations (
                              id                      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                              client_id               BIGINT,
                              user_id                 BIGINT,
                              hotel_id                BIGINT     NOT NULL,
                              room_type_id            BIGINT     NOT NULL,
                              room_id                 BIGINT,
                              status                  ENUM('PENDING','CONFIRMED','CANCELED','CHECKED_IN','CHECKED_OUT','NO_SHOW')
                                                                   NOT NULL DEFAULT 'PENDING',
                              check_in_date           DATE         NOT NULL,
                              check_out_date          DATE         NOT NULL,
                              nights                  INT          NOT NULL,
                              num_guests              INT          NOT NULL,
                              currency                CHAR(3)      NOT NULL,
                              price_total             DECIMAL(12,2) NOT NULL,
                              source_reservation_code VARCHAR(100),
                              upgrade_status          ENUM('NOT_ELIGIBLE','ELIGIBLE','QUEUED','APPLIED','DECLINED')
                                  NOT NULL DEFAULT 'NOT_ELIGIBLE',
                              notes                   TEXT,
                              created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              upgraded_at             DATETIME,
                              canceled_at             DATETIME,
                              UNIQUE KEY uq_res_client_src_code (client_id, source_reservation_code),
                              CHECK (nights > 0),
                              CHECK (num_guests > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= reservations_status_history =========
CREATE TABLE if not exists reservations_status_history (
                                             id                   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                             reservation_id       BIGINT     NOT NULL,
                                             from_status          ENUM('PENDING','CONFIRMED','CANCELED','CHECKED_IN','CHECKED_OUT','NO_SHOW') NOT NULL,
                                             to_status            ENUM('PENDING','CONFIRMED','CANCELED','CHECKED_IN','CHECKED_OUT','NO_SHOW') NOT NULL,
                                             changed_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             changed_by_user_id   BIGINT,
                                             changed_by_client_id BIGINT,
                                             reason               TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========= reservations_nightly_prices =========
CREATE TABLE if not exists reservations_nightly_prices (
                                             id             BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                             reservation_id BIGINT       NOT NULL,
                                             stay_date      DATE         NOT NULL,
                                             room_type_id   BIGINT,
                                             price          DECIMAL(12,2) NOT NULL,
                                             discount       DECIMAL(12,2) NOT NULL DEFAULT 0,
                                             tax            DECIMAL(12,2) NOT NULL DEFAULT 0,
                                             currency       CHAR(3)      NOT NULL,
                                             UNIQUE KEY uq_resnight_res_date (reservation_id, stay_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
