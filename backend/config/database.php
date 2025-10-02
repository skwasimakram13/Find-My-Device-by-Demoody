<?php
// Database configuration
define('DB_HOST', 'localhost');
define('DB_NAME', 'findmydevice');
define('DB_USER', 'root');
define('DB_PASS', '');

function get_database_connection() {
    static $pdo = null;
    
    if ($pdo === null) {
        try {
            $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";
            $options = [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ];
            
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (PDOException $e) {
            error_log('Database connection failed: ' . $e->getMessage());
            throw new Exception('Database connection failed');
        }
    }
    
    return $pdo;
}

// Initialize database tables
function init_database() {
    $db = get_database_connection();
    
    // Users table
    $db->exec("CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )");
    
    // Devices table
    $db->exec("CREATE TABLE IF NOT EXISTS devices (
        id INT AUTO_INCREMENT PRIMARY KEY,
        device_id VARCHAR(255) UNIQUE NOT NULL,
        user_id INT DEFAULT NULL,
        device_name VARCHAR(255) NOT NULL,
        device_token VARCHAR(255) NOT NULL,
        model VARCHAR(255) DEFAULT '',
        os_version VARCHAR(50) DEFAULT '',
        fcm_token TEXT DEFAULT NULL,
        is_active BOOLEAN DEFAULT TRUE,
        last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
    )");
    
    // Locations table
    $db->exec("CREATE TABLE IF NOT EXISTS locations (
        id INT AUTO_INCREMENT PRIMARY KEY,
        device_id VARCHAR(255) NOT NULL,
        lat DECIMAL(10, 8) NOT NULL,
        lng DECIMAL(11, 8) NOT NULL,
        accuracy DECIMAL(8, 2) DEFAULT -1,
        provider VARCHAR(50) DEFAULT 'unknown',
        recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE,
        INDEX idx_device_time (device_id, recorded_at)
    )");
    
    // Commands table
    $db->exec("CREATE TABLE IF NOT EXISTS commands (
        id INT AUTO_INCREMENT PRIMARY KEY,
        device_id VARCHAR(255) NOT NULL,
        type ENUM('LOCK', 'ALARM', 'GET_LOCATION', 'SHOW_MESSAGE', 'WIPE') NOT NULL,
        payload JSON DEFAULT NULL,
        status ENUM('pending', 'acknowledged', 'executed', 'failed') DEFAULT 'pending',
        message TEXT DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        executed_at TIMESTAMP NULL,
        FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE,
        INDEX idx_device_status (device_id, status)
    )");
    
    // Logs table
    $db->exec("CREATE TABLE IF NOT EXISTS logs (
        id INT AUTO_INCREMENT PRIMARY KEY,
        device_id VARCHAR(255) NOT NULL,
        event_type VARCHAR(100) NOT NULL,
        message TEXT DEFAULT NULL,
        event_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE,
        INDEX idx_device_event (device_id, event_at)
    )");
}

// Call init_database() when this file is included
init_database();
?>