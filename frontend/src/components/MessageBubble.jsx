import { useState } from 'react';

/**
 * Renders basic markdown text (bold, inline code, block code, newlines) safely.
 */
function renderMarkdown(text = '') {
    if (!text) return '';
    
    // Escape HTML to prevent XSS
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    // Code Blocks: ```code```
    html = html.replace(/```([\s\S]+?)```/g, (_, code) => {
        return `<pre class="bg-slate-950/80 p-3.5 rounded-xl text-xs overflow-x-auto text-slate-300 font-mono my-2 border border-slate-850">${code.trim()}</pre>`;
    });

    // Inline Code: `code`
    html = html.replace(/`([^`]+?)`/g, '<code class="bg-slate-950 px-1.5 py-0.5 rounded text-xs font-mono text-purple-400">$1</code>');

    // Bold: **text**
    html = html.replace(/\*\*([\s\S]+?)\*\*/g, '<strong>$1</strong>');

    // Newlines
    html = html.replace(/\n/g, '<br />');

    return <div dangerouslySetInnerHTML={{ __html: html }} />;
}

export default function MessageBubble({ message, roleColor = '#a855f7' }) {
    const isUser = message.senderType === 'USER';
    const [hovered, setHovered] = useState(false);

    // Calculate heuristic latency based on content length to make the tactical UI feel alive
    const latency = ((message.content?.length || 0) / 110 + 0.8).toFixed(1) + 's';

    // Choose SVG role icons based on role name
    const getRoleIcon = (role) => {
        const r = role?.toLowerCase() || '';
        if (r.includes('writer') || r.includes('author')) {
            return (
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
            );
        }
        if (r.includes('critic') || r.includes('reviewer') || r.includes('auditor')) {
            return (
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
            );
        }
        // default bot icon
        return (
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
        );
    };

    return (
        <div className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} mb-6 w-full`}>
            {/* Header / Meta bar */}
            <div className="flex items-center space-x-2 mb-2 relative">
                {!isUser && (
                    <span 
                        className="flex items-center gap-1 text-[11px] font-extrabold uppercase tracking-wider py-0.5 px-2 rounded-md transition-all duration-200"
                        style={{
                            backgroundColor: `${roleColor}15`,
                            color: roleColor,
                            border: `1px solid ${roleColor}30`
                        }}
                    >
                        {getRoleIcon(message.roleName)}
                        {message.roleName}
                    </span>
                )}
                {isUser && (
                    <span className="text-[11px] font-extrabold uppercase tracking-wider py-0.5 px-2 rounded-md bg-slate-800 text-slate-300 border border-slate-700">
                        USER
                    </span>
                )}

                {/* Model ID Tag */}
                {message.modelId && (
                    <span className="text-[10px] text-slate-500 font-mono tracking-tight">
                        {message.modelId}
                    </span>
                )}

                {/* Tactical Hover Information Card */}
                {!isUser && !message.isThinking && (
                    <div 
                        className="relative"
                        onMouseEnter={() => setHovered(true)}
                        onMouseLeave={() => setHovered(false)}
                    >
                        <button 
                            type="button" 
                            className="p-1 rounded text-slate-600 hover:text-slate-400 transition-colors focus:outline-none"
                            aria-label="Audit metadata info"
                        >
                            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </button>

                        {/* Hover Popup */}
                        {hovered && (
                            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 bg-slate-900 border border-slate-800 p-3.5 rounded-xl shadow-xl z-55 backdrop-blur-md animate-fade-in">
                                <div className="text-[10px] uppercase font-bold tracking-widest text-purple-400 mb-2 border-b border-slate-850 pb-1">
                                    Agent Telemetry
                                </div>
                                <div className="space-y-1.5 font-mono text-[10px] text-slate-400">
                                    <div className="flex justify-between">
                                        <span>Latency:</span>
                                        <strong className="text-slate-200">{latency}</strong>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Adapter:</span>
                                        <strong className="text-slate-200">{message.isMocked ? 'Simulated' : 'VertexAI'}</strong>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Engine:</span>
                                        <strong className="text-slate-200 truncate max-w-[80px]">{message.modelId}</strong>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Bubble body */}
            <div 
                className={`max-w-2xl px-5 py-3.5 rounded-2xl text-sm transition-all duration-200 leading-relaxed shadow-lg ${
                    isUser
                        ? 'bg-gradient-to-br from-blue-600 to-indigo-600 text-white rounded-tr-none border border-blue-500/20'
                        : 'bg-slate-900/40 text-slate-200 rounded-tl-none border-l-4'
                }`}
                style={{
                    borderLeftColor: isUser ? undefined : roleColor,
                    borderTopColor: isUser ? undefined : 'rgba(30, 41, 59, 0.2)',
                    borderRightColor: isUser ? undefined : 'rgba(30, 41, 59, 0.2)',
                    borderBottomColor: isUser ? undefined : 'rgba(30, 41, 59, 0.2)',
                    backgroundColor: isUser ? undefined : 'rgba(28, 28, 31, 0.4)'
                }}
            >
                {message.isThinking ? (
                    <div className="flex items-center space-x-2 py-1">
                        <span className="text-xs text-slate-500 font-mono italic">Evaluating draft context</span>
                        <div className="flex space-x-1">
                            <span className="h-1.5 w-1.5 bg-purple-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                            <span className="h-1.5 w-1.5 bg-purple-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                            <span className="h-1.5 w-1.5 bg-purple-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                        </div>
                    </div>
                ) : (
                    renderMarkdown(message.content)
                )}
            </div>
        </div>
    );
}
