import { useAuthStore } from '../store/authStore';

const BASE_URL = 'http://localhost:8080';

/**
 * Custom fetch client that automatically injects JWT Bearer tokens from authStore,
 * and handles auth/unauthorized errors by logging out.
 */
export const api = async (endpoint, options = {}) => {
    const { token, logout } = useAuthStore.getState();

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    // Inject token for all secure endpoints (not the auth ones)
    if (token && !endpoint.startsWith('/api/auth')) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(`${BASE_URL}${endpoint}`, {
            ...options,
            headers,
        });

        // Auto logout if backend reports invalid or expired token
        if (response.status === 401 || response.status === 403) {
            console.warn('API client received unauthorized status. Logging out user.');
            logout();
            throw new Error('Session expired. Please log in again.');
        }

        if (response.status === 204) {
            return null;
        }

        // Return empty or structured JSON payload
        const text = await response.text();
        const data = text ? JSON.parse(text) : {};

        if (!response.ok) {
            throw new Error(data.message || 'API request failed');
        }

        return data;
    } catch (error) {
        console.error(`API Error on ${endpoint}:`, error);
        throw error;
    }
};
