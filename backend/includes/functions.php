<?php
function get_json_input() {
    $input = file_get_contents('php://input');
    return json_decode($input, true);
}

function get_auth_header() {
    $headers = getallheaders();
    
    if (isset($headers['Authorization'])) {
        $auth_header = $headers['Authorization'];
        if (strpos($auth_header, 'Bearer ') === 0) {
            return substr($auth_header, 7);
        }
    }
    
    return null;
}

function verify_device_token($device_id, $token) {
    $db = get_database_connection();
    
    $stmt = $db->prepare("SELECT id FROM devices WHERE device_id = ? AND device_token = ? AND is_active = 1");
    $stmt->execute([$device_id, $token]);
    
    return $stmt->fetch() !== false;
}

function generate_token($length = 32) {
    return bin2hex(random_bytes($length / 2));
}

function send_success($data = null) {
    $response = ['status' => 'ok'];
    
    if ($data !== null) {
        if (is_array($data) && !is_numeric(key($data))) {
            // Associative array - merge with response
            $response = array_merge($response, $data);
        } else {
            // Indexed array or other data - put in data field
            $response['data'] = $data;
        }
    }
    
    echo json_encode($response);
    exit();
}

function send_error($message, $code = 400) {
    http_response_code($code);
    echo json_encode([
        'status' => 'error',
        'message' => $message
    ]);
    exit();
}

function log_event($device_id, $event_type, $message = null) {
    try {
        $db = get_database_connection();
        $stmt = $db->prepare("INSERT INTO logs (device_id, event_type, message, event_at) VALUES (?, ?, ?, NOW())");
        $stmt->execute([$device_id, $event_type, $message]);
    } catch (Exception $e) {
        error_log('Failed to log event: ' . $e->getMessage());
    }
}

function clean_old_data() {
    try {
        $db = get_database_connection();
        
        // Clean locations older than 30 days
        $db->exec("DELETE FROM locations WHERE recorded_at < DATE_SUB(NOW(), INTERVAL 30 DAY)");
        
        // Clean executed commands older than 7 days
        $db->exec("DELETE FROM commands WHERE status IN ('executed', 'failed') AND executed_at < DATE_SUB(NOW(), INTERVAL 7 DAY)");
        
        // Clean logs older than 30 days
        $db->exec("DELETE FROM logs WHERE event_at < DATE_SUB(NOW(), INTERVAL 30 DAY)");
        
    } catch (Exception $e) {
        error_log('Failed to clean old data: ' . $e->getMessage());
    }
}

function validate_coordinates($lat, $lng) {
    return is_numeric($lat) && is_numeric($lng) && 
           $lat >= -90 && $lat <= 90 && 
           $lng >= -180 && $lng <= 180;
}

function calculate_distance($lat1, $lng1, $lat2, $lng2) {
    $earth_radius = 6371000; // meters
    
    $lat1_rad = deg2rad($lat1);
    $lat2_rad = deg2rad($lat2);
    $delta_lat = deg2rad($lat2 - $lat1);
    $delta_lng = deg2rad($lng2 - $lng1);
    
    $a = sin($delta_lat / 2) * sin($delta_lat / 2) +
         cos($lat1_rad) * cos($lat2_rad) *
         sin($delta_lng / 2) * sin($delta_lng / 2);
    
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
    
    return $earth_radius * $c;
}

function rate_limit($identifier, $max_requests = 60, $time_window = 3600) {
    // Simple file-based rate limiting
    $rate_limit_file = sys_get_temp_dir() . '/rate_limit_' . md5($identifier);
    
    $current_time = time();
    $requests = [];
    
    if (file_exists($rate_limit_file)) {
        $data = file_get_contents($rate_limit_file);
        $requests = json_decode($data, true) ?: [];
    }
    
    // Remove old requests outside the time window
    $requests = array_filter($requests, function($timestamp) use ($current_time, $time_window) {
        return ($current_time - $timestamp) < $time_window;
    });
    
    // Check if limit exceeded
    if (count($requests) >= $max_requests) {
        return false;
    }
    
    // Add current request
    $requests[] = $current_time;
    
    // Save updated requests
    file_put_contents($rate_limit_file, json_encode($requests));
    
    return true;
}
?>