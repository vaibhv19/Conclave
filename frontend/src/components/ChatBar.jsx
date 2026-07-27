import { useState, useRef, useEffect } from 'react';

export default function ChatBar({ onSubmit, roleAssignments = [], isPaused = false, loading = false }) {
    const [text, setText] = useState('');
    const [showMentionPopover, setShowMentionPopover] = useState(false);
    const [mentionFilter, setMentionFilter] = useState('');
    const [selectedIndex, setSelectedIndex] = useState(0);
    const textareaRef = useRef(null);
    const popoverRef = useRef(null);

    // Track input updates to check for '@'
    const handleChange = (e) => {
        const value = e.target.value;
        setText(value);

        // Find if user is currently typing a mention
        const selectionEnd = e.target.selectionEnd;
        const textBeforeCursor = value.substring(0, selectionEnd);
        const lastAtIndex = textBeforeCursor.lastIndexOf('@');

        if (lastAtIndex !== -1 && lastAtIndex >= textBeforeCursor.lastIndexOf(' ')) {
            const query = textBeforeCursor.substring(lastAtIndex + 1);
            setMentionFilter(query);
            setShowMentionPopover(true);
            setSelectedIndex(0);
        } else {
            setShowMentionPopover(false);
        }
    };

    const handleSelectRole = (roleName) => {
        const selectionEnd = textareaRef.current?.selectionEnd || 0;
        const textBeforeCursor = text.substring(0, selectionEnd);
        const lastAtIndex = textBeforeCursor.lastIndexOf('@');

        if (lastAtIndex !== -1) {
            const textAfterCursor = text.substring(selectionEnd);
            const newText = textBeforeCursor.substring(0, lastAtIndex) + `@${roleName} ` + textAfterCursor;
            setText(newText);
        } else {
            setText(prev => prev + `@${roleName} `);
        }

        setShowMentionPopover(false);
        textareaRef.current?.focus();
    };

    const handleKeyDown = (e) => {
        if (showMentionPopover) {
            const filtered = getFilteredRoles();
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                setSelectedIndex((prev) => (prev + 1) % Math.max(1, filtered.length));
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                setSelectedIndex((prev) => (prev - 1 + filtered.length) % Math.max(1, filtered.length));
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (filtered[selectedIndex]) {
                    handleSelectRole(filtered[selectedIndex].roleName);
                }
            } else if (e.key === 'Escape') {
                e.preventDefault();
                setShowMentionPopover(false);
            }
            return;
        }

        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleSend = () => {
        if (!text.trim() || loading) return;
        onSubmit(text.trim(), isPaused); // If paused, it implicitly triggers an intervention
        setText('');
        setShowMentionPopover(false);
    };

    const getFilteredRoles = () => {
        return roleAssignments.filter(ra =>
            ra.roleName.toLowerCase().includes(mentionFilter.toLowerCase())
        );
    };

    // Close popover when clicking outside
    useEffect(() => {
        const handleOutsideClick = (e) => {
            if (popoverRef.current && !popoverRef.current.contains(e.target)) {
                setShowMentionPopover(false);
            }
        };
        document.addEventListener('mousedown', handleOutsideClick);
        return () => document.removeEventListener('mousedown', handleOutsideClick);
    }, []);

    const filteredRoles = getFilteredRoles();

    return (
        <div className="relative w-full bg-slate-950/40 border-t border-slate-900/60 p-4 space-y-3 flex-none">
            {/* Mention Popover Dropdown */}
            {showMentionPopover && filteredRoles.length > 0 && (
                <div 
                    ref={popoverRef}
                    className="absolute bottom-full left-4 mb-2 w-64 bg-slate-900 border border-slate-800 rounded-xl shadow-2xl z-50 p-2 max-h-48 overflow-y-auto backdrop-blur-md"
                >
                    <div className="text-[9px] uppercase font-bold tracking-widest text-slate-500 px-3 py-1.5 border-b border-slate-850 mb-1">
                        Mention AI Role
                    </div>
                    {filteredRoles.map((role, idx) => (
                        <button
                            key={role.roleName}
                            type="button"
                            onClick={() => handleSelectRole(role.roleName)}
                            className={`w-full text-left px-3 py-2 rounded-lg text-xs font-semibold flex items-center justify-between transition-colors ${
                                idx === selectedIndex
                                    ? 'bg-purple-950/40 text-purple-400 border border-purple-900/30'
                                    : 'text-slate-350 hover:bg-slate-950/50 hover:text-slate-200 border border-transparent'
                            }`}
                        >
                            <span className="flex items-center gap-2">
                                <span className="h-2 w-2 rounded-full" style={{ backgroundColor: role.uiColorHex }} />
                                @{role.roleName}
                            </span>
                            <span className="text-[10px] text-slate-550 font-mono">
                                {role.modelId}
                            </span>
                        </button>
                    ))}
                </div>
            )}

            {/* Input field */}
            <div className="flex gap-3 relative">
                <textarea
                    ref={textareaRef}
                    value={text}
                    onChange={handleChange}
                    onKeyDown={handleKeyDown}
                    disabled={loading}
                    placeholder={
                        isPaused 
                            ? "Pipeline HALTED. Provide feedback here to Inject & Resume..." 
                            : "Type a prompt (type '@' to select role)..."
                    }
                    rows={2}
                    className={`w-full px-4 py-3 rounded-xl bg-slate-950/50 border text-slate-100 placeholder-slate-650 focus:outline-none focus:ring-1 text-sm transition-all duration-200 resize-none ${
                        isPaused 
                            ? 'border-amber-500/30 focus:border-amber-500 focus:ring-amber-500' 
                            : 'border-slate-800 focus:border-purple-500 focus:ring-purple-500'
                    }`}
                />
            </div>

            {/* Action panel */}
            <div className="flex items-center justify-between">
                {/* Mention shortcut lists */}
                <div className="flex flex-wrap gap-1.5 items-center">
                    <span className="text-[9px] uppercase font-bold tracking-widest text-slate-500 mr-1 select-none">
                        Shortcut:
                    </span>
                    {roleAssignments.map((ra) => (
                        <button
                            key={ra.roleName}
                            type="button"
                            disabled={loading}
                            onClick={() => {
                                setText((prev) => `${prev} @${ra.roleName}`.trim() + ' ');
                                textareaRef.current?.focus();
                            }}
                            className="text-[10px] font-mono font-semibold py-1 px-2.5 rounded bg-slate-900 border border-slate-800 text-slate-400 hover:text-purple-400 hover:border-purple-900/50 transition-all focus:outline-none focus:ring-1 focus:ring-purple-800"
                        >
                            @{ra.roleName}
                        </button>
                    ))}
                </div>

                {/* Submit button */}
                <div className="flex gap-2">
                    {/* Send button */}
                    <button
                        type="button"
                        onClick={handleSend}
                        disabled={loading || !text.trim()}
                        className={`px-5 py-2 text-xs font-bold rounded-lg shadow-md transition-all focus:outline-none focus:ring-1 disabled:opacity-30 disabled:pointer-events-none hover:scale-[1.02] active:scale-[0.98] ${
                            isPaused
                                ? 'bg-amber-500 hover:bg-amber-400 text-slate-950 focus:ring-amber-400'
                                : 'bg-gradient-to-r from-blue-600 to-indigo-650 hover:from-blue-500 hover:to-indigo-500 text-white focus:ring-indigo-500'
                        }`}
                    >
                        {loading ? (
                            <svg className="animate-spin h-4 w-4 text-current" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                        ) : isPaused ? (
                            'Inject & Resume'
                        ) : (
                            'Send Command'
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}
