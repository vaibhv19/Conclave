export default function AlertBanner({ isPaused = false, currentRole = '', onResume, actionLoading = false }) {
    if (!isPaused) return null;

    return (
        <div className="w-full flex-none animate-fade-in relative z-25">
            {/* Warning Stripes Layout */}
            <div className="h-1 w-full bg-repeating-stripes-amber" />
            
            {/* Main Alert Body */}
            <div className="bg-amber-500/10 border-b border-amber-500/30 px-6 py-3.5 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center space-x-3 text-sm text-amber-500 font-semibold tracking-wide">
                    {/* Alert Warning Icon */}
                    <svg className="h-5 w-5 text-amber-500 animate-pulse flex-none" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                    <span>
                        <strong className="uppercase font-extrabold mr-1">[PAUSED]</strong> 
                        Pipeline execution halted {currentRole ? `at @${currentRole}` : ''}. Provide feedback below to inject corrections and resume.
                    </span>
                </div>

                <div className="flex items-center gap-2 flex-none">
                    <button
                        type="button"
                        onClick={onResume}
                        disabled={actionLoading}
                        className="px-4 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-bold shadow-md hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-30 disabled:pointer-events-none"
                    >
                        {actionLoading ? (
                            <svg className="animate-spin h-3.5 w-3.5 text-slate-950" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                        ) : (
                            'Force Resume'
                        )}
                    </button>
                </div>
            </div>

            {/* CSS styles injected dynamically for custom repeating warning stripe gradients */}
            <style>{`
                .bg-repeating-stripes-amber {
                    background: repeating-linear-gradient(
                        -45deg,
                        #f59e0b,
                        #f59e0b 8px,
                        #1e1b4b 8px,
                        #1e1b4b 16px
                    );
                }
            `}</style>
        </div>
    );
}
