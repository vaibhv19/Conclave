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
        <div className={`min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-purple-500 selection:text-white relative overflow-hidden h-screen transition-all duration-300 ${
            isPaused ? 'border-4 border-amber-500/40 shadow-[inset_0_0_50px_rgba(245,158,11,0.05)]' : ''
        }`}>
            {/* Background glow effects */}
            <div className="absolute top-0 left-1/3 w-[500px] h-[500px] bg-purple-900/5 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute bottom-0 right-1/3 w-[500px] h-[500px] bg-blue-900/5 rounded-full blur-3xl pointer-events-none" />

            {/* Alert Banner for PAUSED interventions */}
            <AlertBanner 
                isPaused={isPaused} 
                currentRole={activePipelineRole} 
                onResume={handleResume} 
                actionLoading={actionLoading} 
            />

            {/* Header */}
            <header className="border-b border-slate-900 bg-slate-950/80 backdrop-blur-md sticky top-0 z-40 flex-none">
                <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                        <button
                            onClick={clearActiveRoom}
                            className="text-xs font-semibold py-1.5 px-3 rounded-lg border border-slate-800 hover:bg-slate-900 transition-colors text-slate-450 focus:outline-none focus:ring-1 focus:ring-slate-800"
                        >
                            &larr; Exit Room
                        </button>
                        <div>
                            <h2 className="text-lg font-extrabold tracking-tight text-white flex items-center gap-2 select-none">
                                {activeRoom?.name}
                                <span className={`h-2.5 w-2.5 rounded-full ${wsConnected ? 'bg-emerald-500' : 'bg-amber-500'} animate-pulse`} title={wsConnected ? 'WebSocket Connected' : 'WebSocket Standby'} />
                            </h2>
                            <p className="text-[10px] text-slate-500 font-mono tracking-tight mt-0.5 truncate max-w-sm">
                                ID: {roomId}
                            </p>
                        </div>
                    </div>

                    {/* Pipeline Controls */}
                    <div className="flex items-center space-x-3">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-bold border transition-colors select-none ${
                            activeRoom?.status === 'ACTIVE'
                                ? 'bg-emerald-950/40 text-emerald-400 border-emerald-800/40'
                                : activeRoom?.status === 'PAUSED'
                                ? 'bg-amber-950/40 text-amber-400 border-amber-800/40'
                                : 'bg-slate-900 text-slate-450 border-slate-800'
                        }`}>
                            STATUS: {activeRoom?.status}
                        </span>

                        <div className="flex items-center gap-1.5 bg-slate-900/80 border border-slate-800 p-1 rounded-lg">
                            <button
                                onClick={handlePause}
                                disabled={actionLoading || activeRoom?.status !== 'ACTIVE'}
                                className="px-3 py-1.5 rounded text-xs font-semibold bg-slate-950 hover:bg-red-950/20 text-slate-400 hover:text-red-400 disabled:opacity-30 disabled:pointer-events-none transition-colors focus:outline-none"
                            >
                                Pause
                            </button>
                            <button
                                onClick={handleResume}
                                disabled={actionLoading || (activeRoom?.status !== 'PAUSED' && activeRoom?.status !== 'INITIALIZED')}
                                className="px-3 py-1.5 rounded text-xs font-semibold bg-purple-650 hover:bg-purple-550 text-white disabled:opacity-30 disabled:pointer-events-none transition-all focus:outline-none"
                            >
                                Resume
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* Splitter Panel Layout */}
            <div className="flex-1 flex flex-col md:flex-row overflow-hidden w-full max-w-7xl mx-auto px-6 py-6 gap-6 relative z-10 min-h-0">
                {/* 1. Sidebar Panel */}
                <Sidebar 
                    objective={activeRoom?.objective} 
                    workflowState={workflowState} 
                    tokenUsage={tokenUsage} 
                    roleAssignments={activeRoom?.roleAssignments} 
                />

                {/* 2. Main Chat Panel */}
                <main className="flex-1 flex flex-col bg-slate-900/30 border border-slate-800/60 rounded-2xl shadow-2xl backdrop-blur-sm overflow-hidden min-h-0">
                    {/* Message stream */}
                    <div className="flex-1 overflow-y-auto p-6 space-y-6">
                        {messages.length === 0 ? (
                            <div className="h-full flex flex-col items-center justify-center text-center text-slate-500 px-4 select-none">
                                <svg className="h-12 w-12 text-slate-800 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                </svg>
                                <h4 className="text-sm font-bold text-slate-450">Tactical Consensus Stream Active</h4>
                                <p className="text-xs text-slate-600 max-w-sm mt-1">
                                    Send a command below. Mention a target agent role (e.g. <strong className="text-slate-500">@Lead-Writer</strong>) to initialize consensus.
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
