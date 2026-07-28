import { useState, useEffect } from 'react';

export default function Sidebar({ 
    objective = '', 
    workflowState = { currentDraft: '', reviewComments: '' }, 
    tokenUsage = { promptTokens: 0, completionTokens: 0 },
    roleAssignments = []
}) {
    const [collapsed, setCollapsed] = useState(false);
    const [draftUpdated, setDraftUpdated] = useState(false);
    const [commentsUpdated, setCommentsUpdated] = useState(false);

    // Pulse highlight draft on change
    useEffect(() => {
        if (workflowState.currentDraft) {
            setDraftUpdated(true);
            const timer = setTimeout(() => setDraftUpdated(false), 1200);
            return () => clearTimeout(timer);
        }
    }, [workflowState.currentDraft]);

    // Pulse highlight comments on change
    useEffect(() => {
        if (workflowState.reviewComments) {
            setCommentsUpdated(true);
            const timer = setTimeout(() => setCommentsUpdated(false), 1200);
            return () => clearTimeout(timer);
        }
    }, [workflowState.reviewComments]);

    return (
        <aside className={`relative flex flex-col h-full bg-[#121214] border-r border-brand-border transition-all duration-300 select-none ${
            collapsed ? 'w-10' : 'w-full md:w-72'
        }`}>
            {/* Collapse toggle button on top right of panel */}
            <button
                type="button"
                onClick={() => setCollapsed(!collapsed)}
                className="absolute top-3.5 right-3 h-5 w-5 rounded bg-brand-surface border border-brand-border hover:bg-zinc-800 text-zinc-400 hover:text-zinc-200 flex items-center justify-center focus:outline-none transition-colors z-20"
                aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            >
                {collapsed ? '→' : '←'}
            </button>

            {/* Collapsed State Icon Bar */}
            {collapsed && (
                <div className="flex flex-col items-center py-14 space-y-6 w-full">
                    <div className="text-zinc-600 cursor-default" title="Room Objective">🎯</div>
                    <div className={`p-1 rounded border transition-colors ${draftUpdated ? 'border-brand-accent/40 bg-brand-accent/5 text-brand-accent' : 'border-transparent text-zinc-600'}`} title="Consensus Draft">📝</div>
                    <div className={`p-1 rounded border transition-colors ${commentsUpdated ? 'border-brand-accent/40 bg-brand-accent/5 text-brand-accent' : 'border-transparent text-zinc-600'}`} title="Reviews">💬</div>
                    <div className="text-zinc-600 cursor-default" title="Token Audit">📊</div>
                </div>
            )}

            {/* Expanded Sidebar Panel */}
            {!collapsed && (
                <div className="flex-1 flex flex-col min-h-0 p-5 overflow-hidden divide-y divide-brand-border">
                    {/* Environment Metrics Label */}
                    <div className="text-[10px] font-mono font-bold uppercase tracking-widest text-zinc-500 pb-4 flex-none">
                        ENVIRONMENT METRICS
                    </div>

                    {/* Objectives Section - Text only, no border cards */}
                    <div className="py-5 flex-none">
                        <h3 className="text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mb-2">
                            🎯 OBJECTIVE
                        </h3>
                        <p className="text-xs text-zinc-455 leading-relaxed font-sans max-h-24 overflow-y-auto">
                            {objective || 'No objective specified.'}
                        </p>
                    </div>

                    {/* Consensus Draft Section - Borderless editor-like view */}
                    <div className="flex-1 flex flex-col min-h-0 py-5">
                        <h3 className="text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mb-2">
                            📝 CONSENSUS DRAFT
                        </h3>
                        <div className={`flex-1 overflow-y-auto font-mono text-xs text-zinc-300 leading-relaxed transition-colors duration-300 ${
                            draftUpdated ? 'text-brand-accent' : ''
                        }`}>
                            {workflowState.currentDraft ? (
                                workflowState.currentDraft
                            ) : (
                                <span className="text-zinc-600 italic font-sans text-xs">Draft remains uninitialized. Send a message to start consensus.</span>
                            )}
                        </div>
                    </div>

                    {/* Critic Review Section - Borderless review text */}
                    <div className="h-32 flex flex-col min-h-0 py-5 flex-none">
                        <h3 className="text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mb-2">
                            💬 AUDIT REVIEWS
                        </h3>
                        <div className={`flex-1 overflow-y-auto font-sans text-xs text-zinc-400 leading-relaxed transition-colors duration-300 ${
                            commentsUpdated ? 'text-brand-accent' : ''
                        }`}>
                            {workflowState.reviewComments ? (
                                workflowState.reviewComments
                            ) : (
                                <span className="text-zinc-600 italic text-xs">No review audits generated yet.</span>
                            )}
                        </div>
                    </div>

                    {/* Token usage summary - Monospace layout block, no card grids */}
                    <div className="pt-4 mt-auto flex-none">
                        <h3 className="text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mb-3">
                            📊 AUDITED TELEMETRY
                        </h3>
                        <div className="space-y-1.5 font-mono text-[9px]">
                            <div className="flex items-center justify-between py-1 border-b border-brand-border/40">
                                <span className="text-brand-textMuted uppercase tracking-wider">PROMPT_TOKENS</span>
                                <span className="text-zinc-300 font-bold">{tokenUsage.promptTokens}</span>
                            </div>
                            <div className="flex items-center justify-between py-1 border-b border-brand-border/40">
                                <span className="text-brand-textMuted uppercase tracking-wider">COMPL_TOKENS</span>
                                <span className="text-zinc-300 font-bold">{tokenUsage.completionTokens}</span>
                            </div>
                            <div className="flex items-center justify-between py-1">
                                <span className="text-brand-textMuted uppercase tracking-wider">TOTAL_TOKENS</span>
                                <span className="text-zinc-350 font-bold">{tokenUsage.promptTokens + tokenUsage.completionTokens}</span>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </aside>
    );
}
