import { create } from 'zustand';
import { api } from '../services/api';

export const useRoomStore = create((set, get) => ({
    rooms: [],
    activeRoom: null,
    loading: false,
    error: null,

    createRoom: async (name, objective, roleAssignments) => {
        set({ loading: true, error: null });
        try {
            const room = await api('/api/rooms', {
                method: 'POST',
                body: JSON.stringify({ name, objective, roleAssignments })
            });

            set((state) => ({
                rooms: [...state.rooms, room],
                activeRoom: room,
                loading: false
            }));
            return room;
        } catch (err) {
            set({ error: err.message, loading: false });
            throw err;
        }
    },

    fetchRoom: async (roomId) => {
        set({ loading: true, error: null });
        try {
            const room = await api(`/api/rooms/${roomId}`);
            set((state) => ({
                activeRoom: room,
                rooms: state.rooms.map(r => r.roomId === roomId ? room : r),
                loading: false
            }));
            return room;
        } catch (err) {
            set({ error: err.message, loading: false });
            throw err;
        }
    },

    setActiveRoom: (room) => {
        set({ activeRoom: room });
    },

    clearActiveRoom: () => {
        set({ activeRoom: null });
    },

    clearError: () => set({ error: null })
}));
