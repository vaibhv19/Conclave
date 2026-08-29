import { create } from 'zustand';
import { api } from '../services/api';
import { useRoomStore } from './roomStore';

export const useChatStore = create((set, get) => ({
    messages: [],
    workflowState: { currentDraft: '', reviewComments: '', lastUpdatedAt: null },
    tokenUsage: { promptTokens: 0, completionTokens: 0 },
    thinkingMessageId: null,
    loading: false,

    addMessage: (message) => set((state) => {
        // If a message with same ID already exists, do not duplicate
        if (state.messages.some(m => m.id === message.id)) {
            return {};
        }
        return { messages: [...state.messages, message] };
    }),

    updateMessage: (messageId, content) => set((state) => ({
        messages: state.messages.map(m =>
            m.id === messageId ? { ...m, content, isThinking: false } : m
        )
    })),

    setWorkflowState: (workflowState) => set({ workflowState }),

    setTokenUsage: (tokenUsage) => set({ tokenUsage }),

    clearChat: () => set({
        messages: [],
        workflowState: { currentDraft: '', reviewComments: '', lastUpdatedAt: null },
        tokenUsage: { promptTokens: 0, completionTokens: 0 },
        thinkingMessageId: null
    }),

    sendMessage: async (roomId, content, isIntervention = false) => {
        set({ loading: true });
        try {
            await api('/api/chat/message', {
                method: 'POST',
                body: JSON.stringify({ roomId, content, isIntervention })
            });
            set({ loading: false });
        } catch (err) {
            set({ loading: false });
            throw err;
        }
    },

    // Handlers for WS events
    handleTurnStarted: (event, activeRoomId) => {
        const tempId = event.messageId || `temp-${Date.now()}`;
        set((state) => ({
            thinkingMessageId: tempId,
            messages: [
                ...state.messages,
                {
                    id: tempId,
                    senderType: 'AI',
                    roleName: event.roleName,
                    modelId: event.modelId,
                    isMocked: event.isMocked,
                    content: '',
                    isThinking: true,
                    createdAt: new Date().toISOString()
                }
            ]
        }));
    },

    handleContentChunk: (event) => {
        const { thinkingMessageId } = get();
        const realId = event.messageId;
        if (!realId && !thinkingMessageId) return;

        set((state) => {
            let updated = false;
            const messages = state.messages.map(m => {
                if (m.id === realId || (m.id === thinkingMessageId && !updated)) {
                    updated = true;
                    const chunkText = event.chunk || event.delta || '';
                    return {
                        ...m,
                        id: realId || m.id,
                        content: (m.content || '') + chunkText,
                        isThinking: false
                    };
                }
                return m;
            });
            return { messages };
        });
    },

    handleTurnCompleted: (event, activeRoomId) => {
        const { thinkingMessageId } = get();
        const realId = event.messageId;

        set((state) => {
            let updated = false;
            const messages = state.messages.map(m => {
                if (m.id === realId || (m.id === thinkingMessageId && !updated)) {
                    updated = true;
                    return {
                        ...m,
                        id: realId || m.id,
                        content: event.summary,
                        isThinking: false
                    };
                }
                return m;
            });
            return {
                thinkingMessageId: null,
                messages,
                tokenUsage: {
                    promptTokens: event.usage?.promptTokens || 0,
                    completionTokens: event.usage?.completionTokens || 0
                }
            };
        });

        // Fetch updated Room status, role assignments, and WorkflowState draft summaries
        if (activeRoomId) {
            useRoomStore.getState().fetchRoom(activeRoomId).then(room => {
                if (room && room.workflowState) {
                    set({
                        workflowState: {
                            currentDraft: room.workflowState.currentDraft || '',
                            reviewComments: room.workflowState.reviewComments || '',
                            lastUpdatedAt: room.workflowState.lastUpdatedAt
                        }
                    });
                }
            }).catch(err => console.error('Error syncing Room state', err));
        }
    },

    handleSystemIntervention: (event, activeRoomId) => {
        // Force state pipeline status to display PAUSED
        if (activeRoomId) {
            useRoomStore.getState().fetchRoom(activeRoomId).then(room => {
                if (room && room.workflowState) {
                    set({
                        workflowState: {
                            currentDraft: room.workflowState.currentDraft || event.summary || '',
                            reviewComments: room.workflowState.reviewComments || event.comments || '',
                            lastUpdatedAt: room.workflowState.lastUpdatedAt || new Date().toISOString()
                        }
                    });
                }
            }).catch(err => console.error('Error syncing Room state on intervention', err));
        }
    }
}));
