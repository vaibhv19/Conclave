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
        return `<pre class="bg-brand-bg p-3 rounded-lg text-xs overflow-x-auto text-zinc-300 font-mono my-2 border border-brand-border">${code.trim()}</pre>`;
    });

    // Inline Code: `code`
    html = html.replace(/`([^`]+?)`/g, '<code class="bg-brand-surface px-1.5 py-0.5 rounded text-[11px] font-mono text-brand-accent border border-brand-border">$1</code>');

    // Bold: **text**
    html = html.replace(/\*\*([\s\S]+?)\*\*/g, '<strong class="text-white font-semibold">$1</strong>');

    // Newlines
    html = html.replace(/\n/g, '<br />');

    return <div dangerouslySetInnerHTML={{ __html: html }} className="text-zinc-300 text-xs leading-relaxed space-y-1" />;
}

export default function MessageBubble({ message, roleColor = '#8b5cf6' }) {
    const isUser = message.senderType === 'USER';
    const [hovered, setHovered] = useState(false);

    // Calculate heuristic latency based on content length to make the tactical UI feel alive
    const latency = ((message.content?.length || 0) / 110 + 0.8).toFixed(1) + 's';

    // Choose SVG role icons based on role name
    const getRoleIcon = (role) => {
        const r = role?.toLowerCase() || '';
        if (r.includes('writer') || r.includes('author')) {
            return (
                <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg>
            );
        }
        if (r.includes('critic') || r.includes('reviewer') || r.includes('auditor')) {
            return (
                <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
            );
        }
        // default bot icon
        return (
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
        );
    };

    return (
        <div className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} mb-5 w-full`}>
            {/* Header / Meta bar */}
            <div className="flex items-center space-x-2.5 mb-1.5 relative">
                {!isUser && (
                    <span 
                        className="flex items-center gap-1.5 text-[9px] font-mono font-bold uppercase tracking-wider py-0.5 px-2 rounded border"
                        style={{
                            backgroundColor: `${roleColor}10`,
                            color: roleColor,
                            borderColor: `${roleColor}30`
                        }}
                    >
                        {getRoleIcon(message.roleName)}
                        {message.roleName}
                    </span>
                )}
                {isUser && (
                    <span className="text-[9px] font-mono font-bold uppercase tracking-wider py-0.5 px-2 rounded bg-zinc-900 text-zinc-400 border border-brand-border">
                        USER
                    </span>
                )}

                {/* Model ID Tag */}
                {message.modelId && (
                    <span className="text-[9px] text-zinc-600 font-mono tracking-tight">
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
                            className="p-0.5 rounded text-zinc-600 hover:text-zinc-400 transition-colors focus:outline-none"
                            aria-label="Audit metadata info"
                        >
                            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </button>

                        {/* Hover Popup */}
                        {hovered && (
                            <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-44 bg-brand-card border border-brand-border p-2.5 rounded shadow-xl z-50 font-mono">
                                <div className="text-[8px] uppercase font-bold tracking-widest text-brand-accent mb-1.5 border-b border-brand-border pb-1">
                                    Agent Telemetry
                                </div>
                                <div className="space-y-1 text-[8px] text-brand-textMuted">
                                    <div className="flex justify-between">
                                        <span>Latency:</span>
                                        <strong className="text-zinc-300">{latency}</strong>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Adapter:</span>
                                        <strong className="text-zinc-300">{message.isMocked ? 'Simulated' : 'VertexAI'}</strong>
                                    </div>
                                    <div className="flex justify-between">
                                        <span>Engine:</span>
                                        <strong className="text-zinc-300 truncate max-w-[70px]">{message.modelId}</strong>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Bubble body */}
            <div 
                className={`max-w-2xl px-4 py-3 rounded-lg text-xs leading-relaxed border ${
                    isUser
                        ? 'bg-brand-surface/40 text-zinc-200 border-brand-border'
                        : 'bg-brand-card text-zinc-350 border-brand-border border-l-[3px]'
                }`}
                style={{
                    borderLeftColor: isUser ? undefined : roleColor,
                }}
            >
                {message.isThinking ? (
                    <div className="flex items-center space-x-2 py-0.5">
                        <span className="text-[10px] text-zinc-500 font-mono italic">Evaluating draft context</span>
                        <div className="flex space-x-1">
                            <span className="h-1 w-1 bg-brand-accent rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                            <span className="h-1 w-1 bg-brand-accent rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                            <span className="h-1 w-1 bg-brand-accent rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                        </div>
                    </div>
                ) : (
                    renderMarkdown(message.content)
                )}
            </div>
        </div>
    );
}
