import { useState, useEffect, useRef } from 'react';
import { useRoomStore } from '../store/roomStore';
import { useChatStore } from '../store/chatStore';
import { connectWebSocket, disconnectWebSocket, setConnectionStateCallback } from '../services/websocket';
import { api } from '../services/api';

import MessageBubble from '../components/MessageBubble';
import ChatBar from '../components/ChatBar';
import Sidebar from '../components/Sidebar';
import TurnIndicator from '../components/TurnIndicator';
import AlertBanner from '../components/AlertBanner';

export default function RoomView() {
    const { activeRoom, fetchRoom, clearActiveRoom } = useRoomStore();
    const { messages, workflowState, tokenUsage, sendMessage, clearChat, setWorkflowState } = useChatStore();

    const [wsConnected, setWsConnected] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);
    const messagesEndRef = useRef(null);

    const roomId = activeRoom?.roomId;
    const isPaused = activeRoom?.status === 'PAUSED';

    // Connect WebSocket on mount, disconnect on unmount
    useEffect(() => {
        if (roomId) {
            // Load initial state details
            fetchRoom(roomId).then(room => {
                if (room && room.workflowState) {
                    setWorkflowState({
                        currentDraft: room.workflowState.currentDraft || '',
                        reviewComments: room.workflowState.reviewComments || '',
                        lastUpdatedAt: room.workflowState.lastUpdatedAt
                    });
                }
            }).catch(err => console.error('Error fetching room on mount', err));

            // Set up connection callback and connect
            setConnectionStateCallback((connected) => {
                setWsConnected(connected);
            });
            connectWebSocket(roomId);
        }

        return () => {
            disconnectWebSocket();
            clearChat();
        };
    }, [roomId]);

    // Scroll to bottom when messages list updates
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleSend = async (content, isIntervention = false) => {
        if (!content.trim() || actionLoading) return;
        
        setActionLoading(true);
        try {
            await sendMessage(roomId, content.trim(), isIntervention);
            // Refresh room to capture updated Draft context immediately
            await fetchRoom(roomId);
        } catch (err) {
            console.error('Failed to send message:', err);
        } finally {
            setActionLoading(false);
        }
    };

    const handlePause = async () => {
        setActionLoading(true);
        try {
            await api('/api/chat/pipeline/pause', {
                method: 'POST',
                body: JSON.stringify({ roomId })
            });
            await fetchRoom(roomId);
        } catch (err) {
            console.error('Failed to pause pipeline:', err);
        } finally {
            setActionLoading(false);
        }
    };

    const handleResume = async () => {
        setActionLoading(true);
        try {
            await api('/api/chat/pipeline/resume', {
                method: 'POST',
                body: JSON.stringify({ roomId })
            });
            await fetchRoom(roomId);
        } catch (err) {
            console.error('Failed to resume pipeline:', err);
        } finally {
            setActionLoading(false);
        }
    };

    const getRoleColor = (roleName) => {
        const assignment = activeRoom?.roleAssignments?.find(
            ra => ra.roleName.toLowerCase() === roleName?.toLowerCase()
        );
        return assignment?.uiColorHex || '#3b82f6';
    };

    const thinkingMsg = messages.find(m => m.isThinking);
    const activePipelineRole = activeRoom?.roleAssignments?.[activeRoom?.currentPipelineIndex]?.roleName || '';

    return (
        <div className={`min-h-screen bg-brand-bg text-zinc-100 flex flex-col font-sans selection:bg-brand-accent/20 relative overflow-hidden h-screen transition-all duration-300 ${
            isPaused ? 'ring-1 ring-amber-500/30' : ''
        }`}>
            {/* Alert Banner for PAUSED interventions */}
            <AlertBanner 
                isPaused={isPaused} 
                currentRole={activePipelineRole} 
                onResume={handleResume} 
                actionLoading={actionLoading} 
            />

            {/* Header Console */}
            <header className="border-b border-brand-border bg-brand-card flex-none h-14">
                <div className="h-full px-6 flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                        <button
                            onClick={clearActiveRoom}
                            className="text-[10px] font-mono font-bold uppercase tracking-wider py-1.5 px-3 rounded bg-brand-surface border border-brand-border hover:bg-zinc-800 transition-colors text-zinc-400 hover:text-zinc-200 focus:outline-none"
                        >
                            &larr; Exit Console
                        </button>
                        <div className="flex items-center space-x-2.5">
                            <h2 className="text-sm font-bold text-white font-mono flex items-center gap-2 select-none">
                                {activeRoom?.name}
                                <span 
                                    className={`h-1.5 w-1.5 rounded-full ${wsConnected ? 'bg-emerald-500' : 'bg-amber-500'}`} 
                                    title={wsConnected ? 'WebSocket Connected' : 'WebSocket Standby'} 
                                />
                            </h2>
                            <span className="text-[10px] text-zinc-600 font-mono select-all hidden sm:inline">
                                [PID: {roomId?.substring(0, 8)}]
                            </span>
                        </div>
                    </div>

                    {/* Pipeline Controls */}
                    <div className="flex items-center space-x-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border transition-colors select-none ${
                            activeRoom?.status === 'ACTIVE'
                                ? 'bg-emerald-950/20 text-emerald-400 border-emerald-900/30'
                                : activeRoom?.status === 'PAUSED'
                                ? 'bg-amber-950/20 text-amber-400 border-amber-900/30'
                                : 'bg-zinc-900 text-zinc-500 border-brand-border'
                        }`}>
                            STATUS::{activeRoom?.status}
                        </span>

                        <div className="flex items-center gap-1 bg-brand-surface border border-brand-border p-1 rounded">
                            <button
                                onClick={handlePause}
                                disabled={actionLoading || activeRoom?.status !== 'ACTIVE'}
                                className="px-2 py-1 rounded text-[10px] font-mono font-bold uppercase text-zinc-400 hover:text-red-400 disabled:opacity-20 disabled:pointer-events-none transition-colors focus:outline-none"
                            >
                                Pause
                            </button>
                            <button
                                onClick={handleResume}
                                disabled={actionLoading || (activeRoom?.status !== 'PAUSED' && activeRoom?.status !== 'INITIALIZED')}
                                className="px-2 py-1 rounded text-[10px] font-mono font-bold uppercase bg-brand-accent hover:bg-brand-accentHover text-white disabled:opacity-20 disabled:pointer-events-none transition-colors focus:outline-none"
                            >
                                Resume
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* Splitter Panel Layout (IDE-like split) */}
            <div className="flex-1 flex flex-row overflow-hidden w-full min-h-0 relative">
                {/* 1. Left Sidebar Panel */}
                <Sidebar 
                    objective={activeRoom?.objective} 
                    workflowState={workflowState} 
                    tokenUsage={tokenUsage} 
                    roleAssignments={activeRoom?.roleAssignments} 
                />

                {/* 2. Main Chat Workspace */}
                <main className="flex-1 flex flex-col bg-brand-bg overflow-hidden min-h-0 relative">
                    {/* Message stream */}
                    <div className="flex-1 overflow-y-auto p-6 space-y-6">
                        {messages.length === 0 ? (
                            <div className="h-full flex flex-col items-center justify-center text-center text-zinc-500 px-4 select-none">
                                <svg className="h-8 w-8 text-zinc-700 mb-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                </svg>
                                <h4 className="text-xs font-mono font-bold text-zinc-400">STRATEGY STREAM READY</h4>
                                <p className="text-[10px] text-brand-textMuted max-w-sm mt-1">
                                    Initiate command sequences by mentioning target roles (e.g. <span className="font-mono text-zinc-300">@Lead-Writer</span>).
                                </p>
                            </div>
                        ) : (
                            messages.map((msg, idx) => (
                                <MessageBubble 
                                    key={msg.id || idx} 
                                    message={msg} 
                                    roleColor={getRoleColor(msg.roleName)} 
                                />
                            ))
                        )}
                        
                        {/* Stream thinking indicator */}
                        {thinkingMsg && (
                            <TurnIndicator 
                                roleName={thinkingMsg.roleName} 
                                roleColor={getRoleColor(thinkingMsg.roleName)} 
                            />
                        )}

                        <div ref={messagesEndRef} />
                    </div>

                    {/* Chat Input Bar */}
                    <ChatBar 
                        onSubmit={handleSend} 
                        roleAssignments={activeRoom?.roleAssignments || []} 
                        isPaused={isPaused} 
                        loading={actionLoading} 
                    />
                </main>
            </div>
        </div>
    );
}
