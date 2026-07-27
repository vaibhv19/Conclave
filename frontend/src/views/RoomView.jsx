import { useState, useEffect, useRef } from 'react';
import { useRoomStore } from '../store/roomStore';
import { useChatStore } from '../store/chatStore';
import { connectWebSocket, disconnectWebSocket, setConnectionStateCallback } from '../services/websocket';
import { api } from '../services/api';

export default function RoomView() {
    const { activeRoom, fetchRoom, clearActiveRoom } = useRoomStore();
    const { messages, workflowState, tokenUsage, sendMessage, clearChat, setWorkflowState } = useChatStore();

    const [input, setInput] = useState('');
    const [wsConnected, setWsConnected] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);
    const messagesEndRef = useRef(null);

    const roomId = activeRoom?.roomId;

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

    const handleSend = async (isIntervention = false) => {
        if (!input.trim() || actionLoading) return;
        
        setActionLoading(true);
        try {
            await sendMessage(roomId, input.trim(), isIntervention);
            setInput('');
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
            // Refresh room to sync status
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
            // Refresh room to sync status
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
        return assignment?.uiColorHex || '#a855f7'; // fallback purple
    };

    return (
        <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-purple-500 selection:text-white relative overflow-hidden h-screen">
            {/* Background glow effects */}
            <div className="absolute top-0 left-1/3 w-[500px] h-[500px] bg-purple-900/5 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute bottom-0 right-1/3 w-[500px] h-[500px] bg-blue-900/5 rounded-full blur-3xl pointer-events-none" />

            {/* Header */}
            <header className="border-b border-slate-900 bg-slate-950/80 backdrop-blur-md sticky top-0 z-50 flex-none">
                <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                        <button
                            onClick={clearActiveRoom}
                            className="text-xs font-semibold py-1.5 px-3 rounded-lg border border-slate-800 hover:bg-slate-900 transition-colors text-slate-400"
                        >
                            &larr; Exit Room
                        </button>
                        <div>
                            <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                                {activeRoom?.name}
                                <span className={`h-2.5 w-2.5 rounded-full ${wsConnected ? 'bg-emerald-500' : 'bg-amber-500'} animate-pulse`} title={wsConnected ? 'WebSocket Connected' : 'WebSocket Standby'} />
                            </h1>
                            <p className="text-xs text-slate-500 font-mono mt-0.5 truncate max-w-md">
                                Objective: {activeRoom?.objective}
                            </p>
                        </div>
                    </div>

                    {/* Pipeline State Indicators */}
                    <div className="flex items-center space-x-3">
                        <span className={`px-2.5 py-1 rounded-full text-xs font-bold border ${
                            activeRoom?.status === 'ACTIVE'
                                ? 'bg-emerald-950/40 text-emerald-400 border-emerald-800/40'
                                : activeRoom?.status === 'PAUSED'
                                ? 'bg-amber-950/40 text-amber-400 border-amber-800/40'
                                : 'bg-slate-900 text-slate-400 border-slate-800'
                        }`}>
                            Pipeline: {activeRoom?.status}
                        </span>

                        <div className="flex items-center gap-1.5 bg-slate-900/80 border border-slate-800 p-1 rounded-lg">
                            <button
                                onClick={handlePause}
                                disabled={actionLoading || activeRoom?.status !== 'ACTIVE'}
                                className="px-3 py-1.5 rounded text-xs font-semibold bg-slate-950 hover:bg-red-950/20 text-slate-400 hover:text-red-400 disabled:opacity-30 disabled:pointer-events-none transition-colors"
                            >
                                Pause
                            </button>
                            <button
                                onClick={handleResume}
                                disabled={actionLoading || (activeRoom?.status !== 'PAUSED' && activeRoom?.status !== 'INITIALIZED')}
                                className="px-3 py-1.5 rounded text-xs font-semibold bg-purple-600 hover:bg-purple-500 text-white disabled:opacity-30 disabled:pointer-events-none transition-all"
                            >
                                Resume
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* View Splitter Container */}
            <div className="flex-1 flex flex-col md:flex-row overflow-hidden w-full max-w-7xl mx-auto px-6 py-6 gap-6 relative z-10">
                
                {/* 1. Sidebar - WorkflowState & Token Usage */}
                <aside className="w-full md:w-80 flex flex-col gap-4 overflow-y-auto flex-none h-1/3 md:h-full pb-4 pr-1">
                    
                    {/* Draft State summary card */}
                    <div className="bg-slate-900/40 border border-slate-800/60 rounded-2xl p-5 shadow-xl backdrop-blur-sm flex-1 flex flex-col min-h-0">
                        <h3 className="text-sm font-semibold uppercase tracking-wider text-purple-400 border-b border-slate-800 pb-2 mb-3">
                            Consensus Draft
                        </h3>
                        <div className="flex-1 overflow-y-auto font-sans text-sm text-slate-300 leading-relaxed pr-1 whitespace-pre-wrap">
                            {workflowState.currentDraft ? (
                                workflowState.currentDraft
                            ) : (
                                <span className="text-slate-650 italic text-xs">No draft context established yet. Mention an agent to generate.</span>
                            )}
                        </div>
                    </div>

                    {/* Review Comments summary card */}
                    <div className="bg-slate-900/40 border border-slate-800/60 rounded-2xl p-5 shadow-xl backdrop-blur-sm h-40 flex flex-col min-h-0">
                        <h3 className="text-sm font-semibold uppercase tracking-wider text-purple-400 border-b border-slate-800 pb-2 mb-3">
                            Critic Reviews
                        </h3>
                        <div className="flex-1 overflow-y-auto font-sans text-xs text-slate-400 leading-normal pr-1 whitespace-pre-wrap">
                            {workflowState.reviewComments ? (
                                workflowState.reviewComments
                            ) : (
                                <span className="text-slate-650 italic text-xs">No review audits generated yet.</span>
                            )}
                        </div>
                    </div>

                    {/* Token usage panel */}
                    <div className="bg-slate-900/40 border border-slate-800/60 rounded-2xl p-5 shadow-xl backdrop-blur-sm flex-none">
                        <h3 className="text-sm font-semibold uppercase tracking-wider text-purple-400 border-b border-slate-800 pb-2 mb-3">
                            Audited Token Metrics
                        </h3>
                        <div className="grid grid-cols-2 gap-4">
                            <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-850">
                                <span className="text-[10px] text-slate-500 uppercase font-bold tracking-widest block">Prompt</span>
                                <strong className="text-lg font-mono text-indigo-400 mt-1 block">{tokenUsage.promptTokens}</strong>
                            </div>
                            <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-850">
                                <span className="text-[10px] text-slate-500 uppercase font-bold tracking-widest block">Completion</span>
                                <strong className="text-lg font-mono text-purple-400 mt-1 block">{tokenUsage.completionTokens}</strong>
                            </div>
                        </div>
                    </div>
                </aside>

                {/* 2. Main Chat Workspace */}
                <main className="flex-1 flex flex-col bg-slate-900/30 border border-slate-800/60 rounded-2xl shadow-2xl backdrop-blur-sm overflow-hidden h-2/3 md:h-full">
                    {/* Message stream panel */}
                    <div className="flex-1 overflow-y-auto p-6 space-y-6">
                        {messages.length === 0 ? (
                            <div className="h-full flex flex-col items-center justify-center text-center text-slate-500 px-4">
                                <svg className="h-10 w-10 text-slate-700 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                </svg>
                                <h4 className="text-sm font-bold text-slate-400">Consensus Stream Active</h4>
                                <p className="text-xs text-slate-600 max-w-sm mt-1">
                                    Type a prompt below. Make sure to mention a configured agent role (e.g. <strong className="text-slate-500">@Lead-Writer</strong>) to begin.
                                </p>
                            </div>
                        ) : (
                            messages.map((msg, idx) => (
                                <div
                                    key={msg.id || idx}
                                    className={`flex flex-col ${msg.senderType === 'USER' ? 'items-end' : 'items-start'}`}
                                >
                                    <div className="flex items-center space-x-2 mb-1.5">
                                        <span
                                            className="text-xs font-bold font-mono px-2 py-0.5 rounded"
                                            style={{
                                                backgroundColor: msg.senderType === 'USER' ? '#334155' : getRoleColor(msg.roleName) + '20',
                                                color: msg.senderType === 'USER' ? '#f1f5f9' : getRoleColor(msg.roleName),
                                                border: `1px solid ${msg.senderType === 'USER' ? '#475569' : getRoleColor(msg.roleName) + '40'}`
                                            }}
                                        >
                                            {msg.senderType === 'USER' ? 'USER' : `@${msg.roleName}`}
                                        </span>
                                        {msg.modelId && (
                                            <span className="text-[10px] text-slate-550 font-mono">
                                                ({msg.modelId})
                                            </span>
                                        )}
                                    </div>

                                    <div className={`max-w-2xl px-5 py-3.5 rounded-2xl leading-relaxed text-sm ${
                                        msg.senderType === 'USER'
                                            ? 'bg-gradient-to-br from-purple-650 to-indigo-650 text-white rounded-tr-none shadow-md'
                                            : 'bg-slate-950/60 border border-slate-850 text-slate-200 rounded-tl-none relative'
                                    }`}>
                                        {msg.isThinking ? (
                                            <div className="flex items-center space-x-2 py-1">
                                                <span className="text-xs text-slate-500 font-mono italic">Thinking</span>
                                                <div className="flex space-x-1">
                                                    <span className="h-1.5 w-1.5 bg-purple-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                                                    <span className="h-1.5 w-1.5 bg-purple-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                                    <span className="h-1.5 w-1.5 bg-purple-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                                                </div>
                                            </div>
                                        ) : (
                                            <p className="whitespace-pre-wrap">{msg.content}</p>
                                        )}
                                    </div>
                                </div>
                            ))
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Bottom chat input deck */}
                    <div className="p-4 border-t border-slate-900 bg-slate-950/40 flex-none space-y-3">
                        <div className="flex gap-3">
                            <textarea
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                disabled={actionLoading}
                                placeholder="Type a prompt (e.g. 'Build consensus on slogan ideas @Lead-Writer')..."
                                rows={2}
                                className="flex-1 px-4 py-3 rounded-xl bg-slate-950/50 border border-slate-800 text-slate-100 placeholder-slate-600 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 text-sm transition-all duration-200 resize-none"
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter' && !e.shiftKey) {
                                        e.preventDefault();
                                        handleSend(false);
                                    }
                                }}
                            />
                        </div>

                        <div className="flex items-center justify-between">
                            {/* Mentions shortcut lists */}
                            <div className="flex flex-wrap gap-1.5 items-center">
                                <span className="text-[10px] uppercase font-bold tracking-widest text-slate-500 mr-1">Mentions:</span>
                                {activeRoom?.roleAssignments?.map((ra, idx) => (
                                    <button
                                        key={idx}
                                        type="button"
                                        disabled={actionLoading}
                                        onClick={() => setInput((prev) => `${prev} @${ra.roleName}`.trim())}
                                        className="text-[11px] font-mono font-semibold py-1 px-2.5 rounded bg-slate-900 border border-slate-800 text-slate-400 hover:text-purple-400 hover:border-purple-900/50 transition-all"
                                    >
                                        @{ra.roleName}
                                    </button>
                                ))}
                            </div>

                            {/* Submit buttons */}
                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={() => handleSend(true)}
                                    disabled={actionLoading || !input.trim()}
                                    className="px-4 py-2 text-xs font-bold rounded-lg border border-orange-900/40 text-orange-400 bg-orange-950/10 hover:bg-orange-950/20 disabled:opacity-30 disabled:pointer-events-none transition-colors"
                                >
                                    Intervene
                                </button>
                                <button
                                    type="button"
                                    onClick={() => handleSend(false)}
                                    disabled={actionLoading || !input.trim()}
                                    className="px-5 py-2 text-xs font-bold rounded-lg bg-gradient-to-r from-purple-600 to-indigo-650 hover:from-purple-500 hover:to-indigo-500 text-white shadow-md disabled:opacity-30 disabled:pointer-events-none transition-all"
                                >
                                    Send
                                </button>
                            </div>
                        </div>
                    </div>
                </main>
            </div>
        </div>
    );
}
