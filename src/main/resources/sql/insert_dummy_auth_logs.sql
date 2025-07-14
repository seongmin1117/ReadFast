DELIMITER $$

DROP PROCEDURE IF EXISTS insert_dummy_auth_logs$$

CREATE PROCEDURE insert_dummy_auth_logs(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 1;

    WHILE i <= total DO
            INSERT INTO `read-fast`.auth_log_entity (
                id,
                date,
                device,
                endpoint,
                result,
                user_id
            )
            VALUES (
                       i,
                       NOW() - INTERVAL FLOOR(RAND() * 7) DAY - INTERVAL FLOOR(RAND() * 24) HOUR,
                       ELT(FLOOR(1 + (RAND() * 5)),
                           'iPhone 15', 'Galaxy S24', 'MacBook Pro', 'iPad Air', 'Windows 11'),
                       ELT(FLOOR(1 + (RAND() * 4)),
                           '/auth/login', '/auth/verify', '/auth/register', '/auth/refresh'),
                       ELT(FLOOR(1 + (RAND() * 2)), 'SUCCESS', 'FAIL'),
                       CONCAT('user', LPAD(FLOOR(1 + (RAND() * 100)), 3, '0'))
                   );

            SET i = i + 1;
        END WHILE;
END$$

DELIMITER ;

-- ✅ 실행: 100개 더미 데이터 삽입
CALL insert_dummy_auth_logs(100);
