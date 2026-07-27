import { useState, useEffect } from 'react';
import { useAuthStore } from './store/authStore';
import { useRoomStore } from './store/roomStore';
import LoginView from './views/LoginView';
import RegisterView from './views/RegisterView';
import SetupView from './views/SetupView';
import RoomView from './views/RoomView';

export default function App() {
    const { token, init } = useAuthStore();
    const { activeRoom } = useRoomStore();
    const [currentAuthView, setCurrentAuthView] = useState('login'); // 'login' or 'register'

    // Initialize session credentials on mount
    useEffect(() => {
        init();
    }, [init]);

    // 1. Not Authenticated View Flow
    if (!token) {
        if (currentAuthView === 'register') {
            return <RegisterView onSwitchView={() => setCurrentAuthView('login')} />;
        }
        return <LoginView onSwitchView={() => setCurrentAuthView('register')} />;
    }

    // 2. Authenticated but no Room selected/configured
    if (!activeRoom) {
        return <SetupView />;
    }

    // 3. Active consensus workspace
    return <RoomView />;
}
