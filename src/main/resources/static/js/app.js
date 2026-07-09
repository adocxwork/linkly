const API_BASE = '/api';

// Utility: Show Toast Message
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    
    toast.textContent = message;
    toast.className = `show ${type}`;
    
    setTimeout(() => {
        toast.className = '';
    }, 3000);
}

// Utility: Get JWT Token
function getToken() {
    return localStorage.getItem('token');
}

// Utility: Set JWT Token
function setToken(token) {
    localStorage.setItem('token', token);
}

// Utility: Logout
function logout() {
    localStorage.removeItem('token');
    window.location.href = '/';
}

// Utility: API Fetch Wrapper
async function apiCall(endpoint, method = 'GET', body = null) {
    const headers = {
        'Content-Type': 'application/json'
    };
    
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const options = {
        method,
        headers
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        
        if (response.status === 401 || response.status === 403) {
            logout();
            throw new Error('Unauthorized');
        }
        
        if (response.status === 204) return null; // No content

        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || 'Something went wrong');
        }
        
        return data;
    } catch (error) {
        showToast(error.message, 'error');
        throw error;
    }
}

// Handle Login Form
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const identifier = document.getElementById('loginIdentifier').value;
        const password = document.getElementById('loginPassword').value;

        try {
            const res = await apiCall('/auth/login', 'POST', { identifier, password });
            setToken(res.token);
            window.location.href = '/dashboard.html';
        } catch (err) {
            console.error(err);
        }
    });
}

// Handle Register Form
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('regName').value;
        const username = document.getElementById('regUsername').value;
        const email = document.getElementById('regEmail').value;
        const password = document.getElementById('regPassword').value;

        try {
            const res = await apiCall('/auth/register', 'POST', { name, username, email, password });
            setToken(res.token);
            window.location.href = '/dashboard.html';
        } catch (err) {
            console.error(err);
        }
    });
}

// Toggle Login/Register Forms
function toggleAuthMode() {
    document.getElementById('loginSection').classList.toggle('hidden');
    document.getElementById('registerSection').classList.toggle('hidden');
}

// Logout Listener
const logoutBtn = document.getElementById('logoutBtn');
if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        logout();
    });
}
