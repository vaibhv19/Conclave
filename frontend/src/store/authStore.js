import { create } from 'zustand';

export const useAuthStore = create((set, get) => ({
    token: null,
    user: null,
    loading: false,
    error: null,

    init: () => {
        const token = localStorage.getItem('conclave_token');
        const userJson = localStorage.getItem('conclave_user');
        if (token && userJson) {
            try {
                const user = JSON.parse(userJson);
                set({ token, user, error: null });
            } catch (e) {
                localStorage.removeItem('conclave_token');
                localStorage.removeItem('conclave_user');
            }
        }
    },

    login: async (email, password) => {
        set({ loading: true, error: null });
        try {
            const response = await fetch('http://localhost:8080/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Login failed');
            }

            const data = await response.json();
            localStorage.setItem('conclave_token', data.token);
            localStorage.setItem('conclave_user', JSON.stringify(data.user));

            set({ token: data.token, user: data.user, loading: false });
            return data.user;
        } catch (err) {
            set({ error: err.message, loading: false });
            throw err;
        }
    },

    register: async (name, email, password) => {
        set({ loading: true, error: null });
        try {
            const response = await fetch('http://localhost:8080/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Registration failed');
            }

            const data = await response.json();
            localStorage.setItem('conclave_token', data.token);
            localStorage.setItem('conclave_user', JSON.stringify(data.user));

            set({ token: data.token, user: data.user, loading: false });
            return data.user;
        } catch (err) {
            set({ error: err.message, loading: false });
            throw err;
        }
    },

    logout: () => {
        localStorage.removeItem('conclave_token');
        localStorage.removeItem('conclave_user');
        set({ token: null, user: null, error: null });

        // Lazily import websocket to avoid circular dependency, then disconnect if active
        import('../services/websocket').then(({ disconnectWebSocket }) => {
            disconnectWebSocket();
        }).catch(err => console.error('Error during websocket disconnect', err));
    },

    clearError: () => set({ error: null })
}));
