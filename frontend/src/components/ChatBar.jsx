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
        <div className="relative w-full bg-brand-card border-t border-brand-border p-4 space-y-3 flex-none select-none">
            {/* Mention Popover Dropdown */}
            {showMentionPopover && filteredRoles.length > 0 && (
                <div 
                    ref={popoverRef}
                    className="absolute bottom-full left-4 mb-2 w-64 bg-brand-card border border-brand-border rounded shadow-2xl z-50 p-1.5 max-h-48 overflow-y-auto"
                >
                    <div className="text-[8px] font-mono font-bold uppercase tracking-widest text-brand-textMuted px-2.5 py-1.5 border-b border-brand-border mb-1">
                        Mention AI Role
                    </div>
                    {filteredRoles.map((role, idx) => (
                        <button
                            key={role.roleName}
                            type="button"
                            onClick={() => handleSelectRole(role.roleName)}
                            className={`w-full text-left px-2.5 py-1.5 rounded text-[11px] font-mono font-semibold flex items-center justify-between transition-colors focus:outline-none ${
                                idx === selectedIndex
                                    ? 'bg-brand-surface text-brand-accent border border-brand-border'
                                    : 'text-zinc-400 hover:bg-brand-bg hover:text-zinc-200 border border-transparent'
                            }`}
                        >
                            <span className="flex items-center gap-2">
                                <span className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: role.uiColorHex }} />
                                @{role.roleName}
                            </span>
                            <span className="text-[9px] text-zinc-600 font-mono">
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
                    className={`conclave-input resize-none ${
                        isPaused ? 'conclave-input-warning' : ''
                    }`}
                />
            </div>

            {/* Action panel */}
            <div className="flex items-center justify-between">
                {/* Mention shortcut lists */}
                <div className="flex flex-wrap gap-1 items-center">
                    <span className="text-[8px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mr-1 select-none">
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
                            className="text-[9px] font-mono font-bold uppercase py-0.5 px-1.5 rounded bg-brand-surface border border-brand-border text-zinc-400 hover:text-brand-accent hover:border-brand-borderLight transition-all focus:outline-none"
                        >
                            @{ra.roleName}
                        </button>
                    ))}
                </div>

                {/* Submit button */}
                <div className="flex gap-2">
                    <button
                        type="button"
                        onClick={handleSend}
                        disabled={loading || !text.trim()}
                        className={`px-4 py-1.5 text-[10px] font-mono font-bold uppercase tracking-wider rounded transition-colors focus:outline-none disabled:opacity-20 disabled:pointer-events-none ${
                            isPaused
                                ? 'bg-amber-500 hover:bg-amber-600 text-zinc-950'
                                : 'bg-brand-accent hover:bg-brand-accentHover text-white'
                        }`}
                    >
                        {loading ? (
                            <svg className="animate-spin h-3.5 w-3.5 text-current" fill="none" viewBox="0 0 24 24">
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
