import { useState } from 'react';
import { useRoomStore } from '../store/roomStore';
import { useAuthStore } from '../store/authStore';

const MODEL_OPTIONS = [
    { value: 'LLAMA3', label: 'Llama 3 (Local Ollama)' },
    { value: 'MISTRAL', label: 'Mistral (Local Ollama)' },
    { value: 'GEMMA', label: 'Gemma (Local Ollama)' }
];

const PRESET_COLORS = [
    '#6366f1', // Indigo
    '#3b82f6', // Blue
    '#10b981', // Green
    '#f59e0b', // Amber
    '#ef4444', // Red
    '#ec4899', // Pink
    '#8b5cf6'  // Purple
];

export default function SetupView() {
    const [name, setName] = useState('');
    const [objective, setObjective] = useState('');
    const [roles, setRoles] = useState([
        { roleName: 'Lead-Writer', modelId: 'LLAMA3', uiColorHex: '#6366f1' },
        { roleName: 'Code-Critic', modelId: 'MISTRAL', uiColorHex: '#f59e0b' }
    ]);

    const { createRoom, loading, error, clearError } = useRoomStore();
    const { logout, user } = useAuthStore();

    const handleAddRole = () => {
        setRoles([
            ...roles,
            { roleName: `Reviewer-${roles.length + 1}`, modelId: 'LLAMA3', uiColorHex: PRESET_COLORS[roles.length % PRESET_COLORS.length] }
        ]);
    };

    const handleRemoveRole = (index) => {
        if (roles.length <= 1) return; // Must have at least one role
        setRoles(roles.filter((_, i) => i !== index));
    };

    const handleRoleChange = (index, field, value) => {
        const updatedRoles = [...roles];
        updatedRoles[index][field] = value;
        setRoles(updatedRoles);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!name.trim() || !objective.trim()) return;

        // Simple validation
        const emptyRoles = roles.some(r => !r.roleName.trim());
        if (emptyRoles) return;

        try {
            await createRoom(name.trim(), objective.trim(), roles);
        } catch (err) {
            // Handled in store
        }
    };

    return (
        <div className="min-h-screen bg-brand-bg text-brand-textPrimary flex flex-col font-sans select-none pb-16">
            {/* Minimal Desktop-style Header */}
            <header className="border-b border-brand-border bg-[#121214] sticky top-0 z-50">
                <div className="max-w-5xl mx-auto px-6 py-3 flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        {/* Branded Logo representation */}
                        <svg className="h-6 w-6 text-brand-accent" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="50" cy="50" r="42" stroke="currentColor" strokeWidth="2" strokeDasharray="4 4" className="opacity-30" />
                            <path d="M50 12 C57 37 73 43 88 50 C73 57 57 63 50 88 C43 57 27 50 12 50 C27 43 43 37 50 12 Z" fill="currentColor" />
                        </svg>

                        <div className="flex items-baseline space-x-2">
                            <span className="font-mono text-xs font-bold uppercase tracking-wider text-white">
                                CONCLAVE
                            </span>
                            <span className="text-[10px] text-brand-textMuted font-mono uppercase tracking-widest">
                                / initializer
                            </span>
                        </div>
                    </div>

                    <div className="flex items-center space-x-4">
                        <span className="text-xs text-brand-textMuted font-mono">
                            OPERATOR: <strong className="text-zinc-200">{user?.name}</strong>
                        </span>
                        <button
                            onClick={logout}
                            className="text-[10px] font-mono font-bold uppercase tracking-wider py-1.5 px-2.5 rounded bg-brand-surface border border-brand-border hover:bg-zinc-800 transition-colors focus:outline-none"
                        >
                            Sign Out
                        </button>
                    </div>
                </div>
            </header>

            {/* Content Container */}
            <main className="flex-1 max-w-3xl mx-auto px-6 pt-16 pb-12 w-full">
                <div className="mb-12 border-b border-brand-border pb-6">
                    <h1 className="text-lg font-bold tracking-tight text-white font-mono uppercase">
                        Create Workspace
                    </h1>
                    <p className="text-brand-textMuted text-xs mt-1">
                        Configure room metadata and AI consensus pipeline specifications.
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-12">
                    {error && (
                        <div className="bg-red-950/20 border border-red-900/40 text-red-400 px-4 py-2.5 rounded text-xs relative flex items-center justify-between font-mono">
                            <span>{error}</span>
                            <button
                                type="button"
                                onClick={clearError}
                                className="text-red-400 hover:text-red-300 font-bold focus:outline-none"
                                aria-label="Close error"
                            >
                                &times;
                            </button>
                        </div>
                    )}

                    {/* Room Specification Section - Continuous flow, no outer box wrapper */}
                    <div className="space-y-6">
                        <div className="flex items-center space-x-2 border-b border-brand-border pb-3 mb-4">
                            <span className="text-xs text-brand-accent font-mono">01 //</span>
                            <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-zinc-300">
                                ROOM PARAMETERS
                            </h2>
                        </div>

                        <div className="space-y-2">
                            <label className="block text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted">
                                WORKSPACE TITLE
                            </label>
                            <input
                                type="text"
                                required
                                disabled={loading}
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="e.g. Slogan Draft Campaign"
                                className="conclave-input"
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="block text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted">
                                PROJECT OBJECTIVE
                            </label>
                            <textarea
                                required
                                disabled={loading}
                                rows={3}
                                value={objective}
                                onChange={(e) => setObjective(e.target.value)}
                                placeholder="State the objective that the models will collaborate to draft and review..."
                                className="conclave-input"
                            />
                        </div>
                    </div>

                    {/* Role Assignments Section - Clean lists/rows, no outer boxes */}
                    <div className="space-y-6 border-t border-brand-border pt-12">
                        <div className="flex items-center justify-between border-b border-brand-border pb-3 mb-4">
                            <div className="flex items-center space-x-2">
                                <span className="text-xs text-brand-accent font-mono">02 //</span>
                                <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-zinc-300">
                                    AI EXECUTION PIPELINE
                                </h2>
                            </div>
                            <button
                                type="button"
                                onClick={handleAddRole}
                                disabled={loading}
                                className="text-[9px] font-mono font-bold uppercase tracking-wider bg-transparent border border-brand-border text-brand-textSecondary py-1.5 px-3 rounded hover:bg-brand-surface hover:text-white transition-colors focus:outline-none"
                            >
                                + Add Agent Role
                            </button>
                        </div>

                        <div className="divide-y divide-brand-border/60">
                            {roles.map((role, index) => (
                                <div key={index} className="flex flex-col md:flex-row items-start md:items-center gap-6 py-5 first:pt-0 last:pb-0 relative">
                                    {/* Role Name */}
                                    <div className="flex-1 w-full">
                                        <label className="block text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mb-2">
                                            ROLE NAME
                                        </label>
                                        <input
                                            type="text"
                                            required
                                            disabled={loading}
                                            value={role.roleName}
                                            onChange={(e) => handleRoleChange(index, 'roleName', e.target.value.replace(/[^a-zA-Z0-9_-]/g, ''))}
                                            placeholder="e.g. Copywriter"
                                            className="conclave-input"
                                        />
                                    </div>

                                    {/* Model ID Dropdown */}
                                    <div className="flex-1 w-full">
                                        <label className="block text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mb-2">
                                            MODEL ENGINE
                                        </label>
                                        <select
                                            value={role.modelId}
                                            disabled={loading}
                                            onChange={(e) => handleRoleChange(index, 'modelId', e.target.value)}
                                            className="conclave-input"
                                        >
                                            {MODEL_OPTIONS.map(opt => (
                                                <option key={opt.value} value={opt.value} className="bg-brand-bg text-brand-textSecondary">
                                                    {opt.label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    {/* Color Select */}
                                    <div className="w-full md:w-auto">
                                        <label className="block text-[9px] font-mono font-bold uppercase tracking-widest text-brand-textMuted mb-2">
                                            THEME COLOR
                                        </label>
                                        <div className="flex items-center space-x-1.5 py-1">
                                            {PRESET_COLORS.map(color => (
                                                <button
                                                    key={color}
                                                    type="button"
                                                    disabled={loading}
                                                    onClick={() => handleRoleChange(index, 'uiColorHex', color)}
                                                    className={`h-4 w-4 rounded-full border transition-all duration-100 ${
                                                        role.uiColorHex.toLowerCase() === color.toLowerCase()
                                                            ? 'scale-110 border-white ring-1 ring-brand-accent/50'
                                                            : 'border-transparent hover:scale-105'
                                                    }`}
                                                    style={{ backgroundColor: color }}
                                                    aria-label={`Select color ${color}`}
                                                />
                                            ))}
                                        </div>
                                    </div>

                                    {/* Delete Button */}
                                    {roles.length > 1 && (
                                        <div className="flex items-center justify-end w-full md:w-auto md:self-end md:pb-1 pt-2 md:pt-0">
                                            <button
                                                type="button"
                                                onClick={() => handleRemoveRole(index)}
                                                disabled={loading}
                                                className="text-[9px] font-mono font-bold uppercase tracking-wider text-red-500 hover:text-red-400 transition-colors p-1"
                                            >
                                                [Remove]
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Submit Button */}
                    <div className="border-t border-brand-border pt-8">
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-2.5 rounded bg-brand-accent hover:bg-brand-accentHover font-mono font-bold uppercase tracking-wider text-xs text-white transition-colors flex items-center justify-center focus:outline-none"
                        >
                            {loading ? (
                                <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                </svg>
                            ) : (
                                'INITIALIZE CONSENSUS WORKSPACE'
                            )}
                        </button>
                    </div>
                </form>
            </main>
        </div>
    );
}
