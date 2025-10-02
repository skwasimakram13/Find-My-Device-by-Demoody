// Dashboard JavaScript
const API_BASE_URL = 'http://localhost/findmydevice/backend';
let map;
let devices = [];
let selectedDevice = null;
let deviceMarkers = {};

// Initialize the dashboard
document.addEventListener('DOMContentLoaded', function() {
    initMap();
    loadDevices();
    
    // Refresh devices every 30 seconds
    setInterval(loadDevices, 30000);
});

function initMap() {
    // Initialize Leaflet map
    map = L.map('map').setView([40.7128, -74.0060], 10); // Default to NYC
    
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);
}

async function loadDevices() {
    try {
        // In a real implementation, you would have authentication
        // For demo purposes, we'll load all devices
        const response = await fetch(`${API_BASE_URL}/devices`);
        
        if (!response.ok) {
            throw new Error('Failed to load devices');
        }
        
        const data = await response.json();
        devices = data.data || [];
        
        updateDeviceList();
        updateMap();
        
    } catch (error) {
        console.error('Error loading devices:', error);
        showError('Failed to load devices');
    }
}

function updateDeviceList() {
    const deviceList = document.getElementById('deviceList');
    
    if (devices.length === 0) {
        deviceList.innerHTML = '<p class="text-muted">No devices found</p>';
        return;
    }
    
    let html = '';
    devices.forEach(device => {
        const isOnline = isDeviceOnline(device.last_seen);
        const statusClass = isOnline ? 'status-online' : 'status-offline';
        const statusText = isOnline ? 'Online' : 'Offline';
        
        html += `
            <div class="card device-card ${selectedDevice === device.device_id ? 'border-primary' : ''}" 
                 onclick="selectDevice('${device.device_id}')">
                <div class="card-body">
                    <h6 class="card-title">${escapeHtml(device.device_name)}</h6>
                    <p class="card-text">
                        <span class="${statusClass}">● ${statusText}</span><br>
                        <small class="last-seen">Last seen: ${formatDateTime(device.last_seen)}</small>
                    </p>
                </div>
            </div>
        `;
    });
    
    deviceList.innerHTML = html;
}

function updateMap() {
    // Clear existing markers
    Object.values(deviceMarkers).forEach(marker => {
        map.removeLayer(marker);
    });
    deviceMarkers = {};
    
    let bounds = [];
    
    devices.forEach(device => {
        if (device.last_location) {
            const lat = parseFloat(device.last_location.lat);
            const lng = parseFloat(device.last_location.lng);
            
            if (!isNaN(lat) && !isNaN(lng)) {
                const isOnline = isDeviceOnline(device.last_seen);
                const iconColor = isOnline ? 'green' : 'red';
                
                const marker = L.marker([lat, lng]).addTo(map);
                marker.bindPopup(`
                    <strong>${escapeHtml(device.device_name)}</strong><br>
                    Status: ${isOnline ? 'Online' : 'Offline'}<br>
                    Last seen: ${formatDateTime(device.last_seen)}<br>
                    Accuracy: ${device.last_location.accuracy}m
                `);
                
                deviceMarkers[device.device_id] = marker;
                bounds.push([lat, lng]);
            }
        }
    });
    
    // Fit map to show all devices
    if (bounds.length > 0) {
        map.fitBounds(bounds, { padding: [20, 20] });
    }
}

function selectDevice(deviceId) {
    selectedDevice = deviceId;
    const device = devices.find(d => d.device_id === deviceId);
    
    updateDeviceList();
    
    if (device) {
        updateDeviceInfo(device);
        
        // Center map on selected device
        if (device.last_location) {
            const lat = parseFloat(device.last_location.lat);
            const lng = parseFloat(device.last_location.lng);
            
            if (!isNaN(lat) && !isNaN(lng)) {
                map.setView([lat, lng], 15);
                
                // Open popup for selected device
                if (deviceMarkers[deviceId]) {
                    deviceMarkers[deviceId].openPopup();
                }
            }
        }
    }
}

function updateDeviceInfo(device) {
    const deviceInfo = document.getElementById('deviceInfo');
    
    let html = `
        <div class="row">
            <div class="col-sm-6">
                <strong>Device Name:</strong> ${escapeHtml(device.device_name)}<br>
                <strong>Model:</strong> ${escapeHtml(device.model || 'Unknown')}<br>
                <strong>OS Version:</strong> ${escapeHtml(device.os_version || 'Unknown')}<br>
            </div>
            <div class="col-sm-6">
                <strong>Status:</strong> ${isDeviceOnline(device.last_seen) ? 'Online' : 'Offline'}<br>
                <strong>Last Seen:</strong> ${formatDateTime(device.last_seen)}<br>
                <strong>Device ID:</strong> <small>${escapeHtml(device.device_id)}</small><br>
            </div>
        </div>
    `;
    
    if (device.last_location) {
        html += `
            <hr>
            <div class="row">
                <div class="col-sm-6">
                    <strong>Latitude:</strong> ${device.last_location.lat}<br>
                    <strong>Longitude:</strong> ${device.last_location.lng}<br>
                </div>
                <div class="col-sm-6">
                    <strong>Accuracy:</strong> ${device.last_location.accuracy}m<br>
                    <strong>Provider:</strong> ${escapeHtml(device.last_location.provider)}<br>
                </div>
            </div>
        `;
    }
    
    deviceInfo.innerHTML = html;
}

async function sendCommand(commandType) {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/devices/${selectedDevice}/commands`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                type: commandType,
                payload: {}
            })
        });
        
        if (!response.ok) {
            throw new Error('Failed to send command');
        }
        
        const data = await response.json();
        showSuccess(`Command ${commandType} sent successfully (ID: ${data.command_id})`);
        
    } catch (error) {
        console.error('Error sending command:', error);
        showError('Failed to send command');
    }
}

function showMessageModal() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    const modal = new bootstrap.Modal(document.getElementById('messageModal'));
    modal.show();
}

async function sendMessageCommand() {
    const messageText = document.getElementById('messageText').value.trim();
    
    if (!messageText) {
        showError('Please enter a message');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/devices/${selectedDevice}/commands`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                type: 'SHOW_MESSAGE',
                payload: {
                    message: messageText
                }
            })
        });
        
        if (!response.ok) {
            throw new Error('Failed to send message');
        }
        
        const data = await response.json();
        showSuccess(`Message sent successfully (ID: ${data.command_id})`);
        
        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('messageModal'));
        modal.hide();
        
        // Clear message
        document.getElementById('messageText').value = '';
        
    } catch (error) {
        console.error('Error sending message:', error);
        showError('Failed to send message');
    }
}

function confirmWipe() {
    if (!selectedDevice) {
        showError('Please select a device first');
        return;
    }
    
    if (confirm('WARNING: This will completely wipe the selected device. This action cannot be undone. Are you sure?')) {
        sendCommand('WIPE');
    }
}

function refreshDevices() {
    loadDevices();
}

// Utility functions
function isDeviceOnline(lastSeen) {
    const now = new Date();
    const lastSeenDate = new Date(lastSeen);
    const diffMinutes = (now - lastSeenDate) / (1000 * 60);
    
    return diffMinutes < 10; // Consider online if seen within 10 minutes
}

function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showSuccess(message) {
    // Simple toast notification
    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-white bg-success border-0 position-fixed top-0 end-0 m-3';
    toast.style.zIndex = '9999';
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${escapeHtml(message)}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    document.body.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', () => {
        document.body.removeChild(toast);
    });
}

function showError(message) {
    // Simple toast notification
    const toast = document.createElement('div');
    toast.className = 'toast align-items-center text-white bg-danger border-0 position-fixed top-0 end-0 m-3';
    toast.style.zIndex = '9999';
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${escapeHtml(message)}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    document.body.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', () => {
        document.body.removeChild(toast);
    });
}