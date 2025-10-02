<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'config/database.php';
require_once 'includes/functions.php';

// Get the request URI and method
$request_uri = $_SERVER['REQUEST_URI'];
$request_method = $_SERVER['REQUEST_METHOD'];

// Remove query string and decode URI
$path = parse_url($request_uri, PHP_URL_PATH);
$path = urldecode($path);

// Remove base path if running in subdirectory
$base_path = '/api';
if (strpos($path, $base_path) === 0) {
    $path = substr($path, strlen($base_path));
}

// Route the request
route_request($path, $request_method);

function route_request($path, $method) {
    // Remove leading slash
    $path = ltrim($path, '/');
    $segments = explode('/', $path);
    
    try {
        switch ($segments[0]) {
            case 'register_device':
                if ($method === 'POST') {
                    handle_device_registration();
                } else {
                    send_error('Method not allowed', 405);
                }
                break;
                
            case 'devices':
                if (count($segments) >= 2) {
                    $device_id = $segments[1];
                    
                    if (count($segments) >= 3) {
                        switch ($segments[2]) {
                            case 'location':
                                if ($method === 'POST') {
                                    handle_location_upload($device_id);
                                } else {
                                    send_error('Method not allowed', 405);
                                }
                                break;
                                
                            case 'commands':
                                if (count($segments) >= 4 && $segments[3] === 'poll') {
                                    if ($method === 'GET') {
                                        handle_command_poll($device_id);
                                    } else {
                                        send_error('Method not allowed', 405);
                                    }
                                } elseif (count($segments) >= 5 && $segments[4] === 'ack') {
                                    if ($method === 'POST') {
                                        handle_command_ack($device_id, $segments[3]);
                                    } else {
                                        send_error('Method not allowed', 405);
                                    }
                                } elseif ($method === 'POST') {
                                    handle_command_create($device_id);
                                } else {
                                    send_error('Method not allowed', 405);
                                }
                                break;
                                
                            case 'sim_change':
                                if ($method === 'POST') {
                                    handle_sim_change($device_id);
                                } else {
                                    send_error('Method not allowed', 405);
                                }
                                break;
                                
                            case 'status':
                                if ($method === 'GET') {
                                    handle_device_status($device_id);
                                } else {
                                    send_error('Method not allowed', 405);
                                }
                                break;
                                
                            default:
                                send_error('Endpoint not found', 404);
                        }
                    } else {
                        send_error('Invalid device endpoint', 400);
                    }
                } else {
                    send_error('Device ID required', 400);
                }
                break;
                
            default:
                send_error('Endpoint not found', 404);
        }
    } catch (Exception $e) {
        error_log('API Error: ' . $e->getMessage());
        send_error('Internal server error', 500);
    }
}

function handle_device_registration() {
    $input = get_json_input();
    
    if (!$input || !isset($input['device_id']) || !isset($input['device_name'])) {
        send_error('Missing required fields', 400);
        return;
    }
    
    $device_id = $input['device_id'];
    $device_name = $input['device_name'];
    $auth_token = $input['auth_token'] ?? generate_token();
    $model = $input['model'] ?? '';
    $os_version = $input['os_version'] ?? '';
    $fcm_token = $input['fcm_token'] ?? null;
    
    $db = get_database_connection();
    
    // Check if device already exists
    $stmt = $db->prepare("SELECT id FROM devices WHERE device_id = ?");
    $stmt->execute([$device_id]);
    
    if ($stmt->fetch()) {
        // Update existing device
        $stmt = $db->prepare("UPDATE devices SET device_name = ?, model = ?, os_version = ?, fcm_token = ?, last_seen = NOW() WHERE device_id = ?");
        $stmt->execute([$device_name, $model, $os_version, $fcm_token, $device_id]);
    } else {
        // Create new device
        $device_token = generate_token();
        $stmt = $db->prepare("INSERT INTO devices (device_id, device_name, device_token, model, os_version, fcm_token, is_active, created_at, last_seen) VALUES (?, ?, ?, ?, ?, ?, 1, NOW(), NOW())");
        $stmt->execute([$device_id, $device_name, $device_token, $model, $os_version, $fcm_token]);
    }
    
    send_success(['device_token' => $device_token ?? $auth_token]);
}

function handle_location_upload($device_id) {
    $auth_header = get_auth_header();
    if (!$auth_header || !verify_device_token($device_id, $auth_header)) {
        send_error('Unauthorized', 401);
        return;
    }
    
    $input = get_json_input();
    
    if (!$input || !isset($input['lat']) || !isset($input['lng'])) {
        send_error('Missing location data', 400);
        return;
    }
    
    $lat = floatval($input['lat']);
    $lng = floatval($input['lng']);
    $accuracy = floatval($input['accuracy'] ?? -1);
    $provider = $input['provider'] ?? 'unknown';
    $timestamp = $input['timestamp'] ?? time();
    
    $db = get_database_connection();
    
    // Insert location
    $stmt = $db->prepare("INSERT INTO locations (device_id, lat, lng, accuracy, provider, recorded_at) VALUES (?, ?, ?, ?, ?, FROM_UNIXTIME(?))");
    $stmt->execute([$device_id, $lat, $lng, $accuracy, $provider, $timestamp]);
    
    // Update device last_seen
    $stmt = $db->prepare("UPDATE devices SET last_seen = NOW() WHERE device_id = ?");
    $stmt->execute([$device_id]);
    
    send_success();
}

function handle_command_poll($device_id) {
    $auth_header = get_auth_header();
    if (!$auth_header || !verify_device_token($device_id, $auth_header)) {
        send_error('Unauthorized', 401);
        return;
    }
    
    $since_id = intval($_GET['since_id'] ?? 0);
    
    $db = get_database_connection();
    
    $stmt = $db->prepare("SELECT id, type, payload, created_at FROM commands WHERE device_id = ? AND id > ? AND status = 'pending' ORDER BY id ASC LIMIT 10");
    $stmt->execute([$device_id, $since_id]);
    
    $commands = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $row['payload'] = json_decode($row['payload'], true);
        $commands[] = $row;
    }
    
    send_success($commands);
}

function handle_command_ack($device_id, $command_id) {
    $auth_header = get_auth_header();
    if (!$auth_header || !verify_device_token($device_id, $auth_header)) {
        send_error('Unauthorized', 401);
        return;
    }
    
    $input = get_json_input();
    $status = $input['status'] ?? 'executed';
    $message = $input['message'] ?? '';
    $executed_at = $input['executed_at'] ?? time();
    
    $db = get_database_connection();
    
    $stmt = $db->prepare("UPDATE commands SET status = ?, message = ?, executed_at = FROM_UNIXTIME(?) WHERE id = ? AND device_id = ?");
    $stmt->execute([$status, $message, $executed_at, $command_id, $device_id]);
    
    send_success();
}

function handle_command_create($device_id) {
    // This would typically be called from the web dashboard
    $input = get_json_input();
    
    if (!$input || !isset($input['type'])) {
        send_error('Missing command type', 400);
        return;
    }
    
    $type = $input['type'];
    $payload = json_encode($input['payload'] ?? []);
    
    $db = get_database_connection();
    
    $stmt = $db->prepare("INSERT INTO commands (device_id, type, payload, status, created_at) VALUES (?, ?, ?, 'pending', NOW())");
    $stmt->execute([$device_id, $type, $payload]);
    
    $command_id = $db->lastInsertId();
    
    send_success(['command_id' => $command_id]);
}

function handle_sim_change($device_id) {
    $auth_header = get_auth_header();
    if (!$auth_header || !verify_device_token($device_id, $auth_header)) {
        send_error('Unauthorized', 401);
        return;
    }
    
    $input = get_json_input();
    
    $db = get_database_connection();
    
    // Log the SIM change event
    $stmt = $db->prepare("INSERT INTO logs (device_id, event_type, message, event_at) VALUES (?, 'SIM_CHANGE', ?, NOW())");
    $stmt->execute([$device_id, json_encode($input)]);
    
    // If location is provided, store it
    if (isset($input['location'])) {
        $location = $input['location'];
        $stmt = $db->prepare("INSERT INTO locations (device_id, lat, lng, accuracy, provider, recorded_at) VALUES (?, ?, ?, ?, 'sim_change', NOW())");
        $stmt->execute([$device_id, $location['lat'], $location['lng'], $location['accuracy'] ?? -1]);
    }
    
    send_success();
}

function handle_device_status($device_id) {
    $auth_header = get_auth_header();
    if (!$auth_header || !verify_device_token($device_id, $auth_header)) {
        send_error('Unauthorized', 401);
        return;
    }
    
    $db = get_database_connection();
    
    // Get device info
    $stmt = $db->prepare("SELECT device_name, model, os_version, is_active, last_seen FROM devices WHERE device_id = ?");
    $stmt->execute([$device_id]);
    $device = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$device) {
        send_error('Device not found', 404);
        return;
    }
    
    // Get latest location
    $stmt = $db->prepare("SELECT lat, lng, accuracy, provider, recorded_at FROM locations WHERE device_id = ? ORDER BY recorded_at DESC LIMIT 1");
    $stmt->execute([$device_id]);
    $location = $stmt->fetch(PDO::FETCH_ASSOC);
    
    $device['last_location'] = $location;
    
    send_success($device);
}
?>