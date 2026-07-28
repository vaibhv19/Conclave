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
        <aside className={`relative flex flex-col h-full bg-slate-900/40 border border-slate-800/60 rounded-2xl shadow-xl backdrop-blur-sm transition-all duration-300 ${
            collapsed ? 'w-12' : 'w-full md:w-80'
        }`}>
            {/* Collapse toggle button */}
            <button
                type="button"
                onClick={() => setCollapsed(!collapsed)}
                className="absolute top-4 -left-3 h-6 w-6 rounded-full bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-400 hover:text-slate-200 flex items-center justify-center shadow-md focus:outline-none transition-colors z-20"
                aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            >
                {collapsed ? '→' : '←'}
            </button>

            {/* Collapsed State Icon Bar */}
            {collapsed && (
                <div className="flex flex-col items-center py-6 space-y-6 select-none w-full">
                    {/* Icons representing segments */}
                    <div className="text-slate-650 cursor-default" title="Room Objective">
                        🎯
                    </div>
                    <div className={`text-slate-650 cursor-default p-1.5 rounded-lg border transition-colors ${draftUpdated ? 'bg-purple-950/30 text-purple-400 border-purple-900/50' : 'border-transparent'}`} title="Consensus Draft">
                        📝
                    </div>
                    <div className={`text-slate-650 cursor-default p-1.5 rounded-lg border transition-colors ${commentsUpdated ? 'bg-amber-950/30 text-amber-400 border-amber-900/50' : 'border-transparent'}`} title="Reviews">
                        💬
                    </div>
                    <div className="text-slate-650 cursor-default" title="Token Audit">
                        📊
                    </div>
                </div>
            )}

            {/* Expanded Sidebar Panel */}
            {!collapsed && (
                <div className="flex-1 flex flex-col min-h-0 p-5 overflow-hidden">
                    {/* Objectives Section */}
                    <div className="mb-4 flex-none">
                        <h3 className="text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-1.5 flex items-center gap-1.5">
                            🎯 Room Objective
                        </h3>
                        <p className="text-xs text-slate-400 leading-normal bg-slate-950/40 border border-slate-850 p-2.5 rounded-xl">
                            {objective || 'No objective specified.'}
                        </p>
                    </div>

                    {/* Consensus Draft Section */}
                    <div className="flex-1 flex flex-col min-h-0 mb-4">
                        <h3 className="text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-1.5 flex items-center gap-1.5">
                            📝 Consensus Draft
                        </h3>
                        <div className={`flex-1 overflow-y-auto font-mono text-xs text-slate-350 leading-relaxed bg-slate-950/40 border p-3 rounded-xl transition-all duration-300 ${
                            draftUpdated 
                                ? 'border-purple-500 bg-purple-950/10 shadow-lg shadow-purple-950/10' 
                                : 'border-slate-850'
                        }`}>
                            {workflowState.currentDraft ? (
                                workflowState.currentDraft
                            ) : (
                                <span className="text-slate-650 italic">Draft remains uninitialized. Send a message to start consensus.</span>
                            )}
                        </div>
                    </div>

                    {/* Critic Review Section */}
                    <div className="h-36 flex flex-col min-h-0 mb-4 flex-none">
                        <h3 className="text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-1.5 flex items-center gap-1.5">
                            💬 Critic Audit Reviews
                        </h3>
                        <div className={`flex-1 overflow-y-auto font-sans text-xs text-slate-450 leading-normal bg-slate-950/40 border p-3 rounded-xl transition-all duration-300 ${
                            commentsUpdated 
                                ? 'border-amber-500 bg-amber-950/10 shadow-lg shadow-amber-950/10' 
                                : 'border-slate-850'
                        }`}>
                            {workflowState.reviewComments ? (
                                workflowState.reviewComments
                            ) : (
                                <span className="text-slate-650 italic">No review audits generated yet.</span>
                            )}
                        </div>
                    </div>

                    {/* Token usage summary */}
                    <div className="mt-auto pt-4 border-t border-slate-850 flex-none">
                        <h3 className="text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-2">
                            📊 Audited Token Metrics
                        </h3>
                        <div className="grid grid-cols-2 gap-3">
                            <div className="bg-slate-950/60 p-2.5 rounded-xl border border-slate-850 text-center">
                                <span className="text-[9px] text-slate-550 uppercase font-bold tracking-widest block">Prompt</span>
                                <strong className="text-sm font-mono text-indigo-400 mt-0.5 block">{tokenUsage.promptTokens}</strong>
                            </div>
                            <div className="bg-slate-950/60 p-2.5 rounded-xl border border-slate-850 text-center">
                                <span className="text-[9px] text-slate-550 uppercase font-bold tracking-widest block">Completion</span>
                                <strong className="text-sm font-mono text-purple-400 mt-0.5 block">{tokenUsage.completionTokens}</strong>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </aside>
    );
}
