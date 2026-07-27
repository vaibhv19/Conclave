import { useState } from 'react';
import { useRoomStore } from '../store/roomStore';
import { useAuthStore } from '../store/authStore';

const MODEL_OPTIONS = [
    { value: 'GEMINI_PRO', label: 'Gemini 1.5 Pro (Google Vertex AI)' },
    { value: 'FAKE_OPENAI', label: 'GPT-4o (Simulated / Mocked)' },
    { value: 'FAKE_CLAUDE', label: 'Claude 3.5 Sonnet (Simulated / Mocked)' }
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
        { roleName: 'Lead-Writer', modelId: 'FAKE_OPENAI', uiColorHex: '#6366f1' },
        { roleName: 'Code-Critic', modelId: 'FAKE_CLAUDE', uiColorHex: '#f59e0b' }
    ]);

    const { createRoom, loading, error, clearError } = useRoomStore();
    const { logout, user } = useAuthStore();

    const handleAddRole = () => {
        setRoles([
            ...roles,
            { roleName: `Reviewer-${roles.length + 1}`, modelId: 'FAKE_OPENAI', uiColorHex: PRESET_COLORS[roles.length % PRESET_COLORS.length] }
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
        <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-purple-500 selection:text-white relative overflow-hidden pb-12">
            {/* Background glow effects */}
            <div className="absolute top-0 left-1/4 w-[500px] h-[500px] bg-purple-900/10 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute bottom-0 right-1/4 w-[500px] h-[500px] bg-blue-900/10 rounded-full blur-3xl pointer-events-none" />

            {/* Header */}
            <header className="border-b border-slate-900 bg-slate-950/80 backdrop-blur-md sticky top-0 z-50">
                <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-purple-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-purple-950/50">
                            <span className="font-extrabold text-lg text-white">C</span>
                        </div>
                        <div>
                            <span className="font-bold text-lg tracking-wider bg-clip-text text-transparent bg-gradient-to-r from-purple-400 to-indigo-200">
                                CONCLAVE
                            </span>
                            <span className="text-[10px] block text-slate-500 font-mono tracking-widest uppercase">
                                Setup Wizard
                            </span>
                        </div>
                    </div>

                    <div className="flex items-center space-x-4">
                        <span className="text-sm text-slate-400">
                            Hi, <strong className="text-slate-200">{user?.name}</strong>
                        </span>
                        <button
                            onClick={logout}
                            className="text-xs font-semibold py-1.5 px-3 rounded-lg border border-slate-800 hover:bg-slate-900 transition-colors"
                        >
                            Sign Out
                        </button>
                    </div>
                </div>
            </header>

            {/* Content */}
            <main className="flex-1 max-w-4xl mx-auto px-6 py-12 w-full relative z-10">
                <div className="mb-8">
                    <h1 className="text-3xl font-extrabold tracking-tight mb-2 bg-clip-text text-transparent bg-gradient-to-b from-white to-slate-400">
                        Create Workspace
                    </h1>
                    <p className="text-slate-400 text-sm">
                        Define your project objective and orchestrate target AI model roles.
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-8">
                    {error && (
                        <div className="bg-red-950/50 border border-red-800/30 text-red-400 px-4 py-3 rounded-xl text-sm relative">
                            <span>{error}</span>
                            <button
                                type="button"
                                onClick={clearError}
                                className="absolute top-0 bottom-0 right-0 px-4 py-3 text-red-400 font-bold"
                            >
                                &times;
                            </button>
                        </div>
                    )}

                    {/* Room Basic Info */}
                    <div className="bg-slate-900/40 border border-slate-800/60 rounded-3xl p-6 shadow-xl backdrop-blur-sm space-y-6">
                        <h2 className="text-lg font-bold border-b border-slate-800/60 pb-3 text-purple-400">
                            1. Room Specification
                        </h2>

                        <div>
                            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                                Workspace Title
                            </label>
                            <input
                                type="text"
                                required
                                disabled={loading}
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="e.g. Slogan Draft Campaign"
                                className="w-full px-4 py-3 rounded-xl bg-slate-950/50 border border-slate-800 text-slate-100 placeholder-slate-600 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-all duration-200"
                            />
                        </div>

                        <div>
                            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                                Room Objective
                            </label>
                            <textarea
                                required
                                disabled={loading}
                                rows={3}
                                value={objective}
                                onChange={(e) => setObjective(e.target.value)}
                                placeholder="State the objective that the models will collaborate to draft and review..."
                                className="w-full px-4 py-3 rounded-xl bg-slate-950/50 border border-slate-800 text-slate-100 placeholder-slate-600 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-all duration-200 resize-none"
                            />
                        </div>
                    </div>

                    {/* Role Assignments Setup */}
                    <div className="bg-slate-900/40 border border-slate-800/60 rounded-3xl p-6 shadow-xl backdrop-blur-sm space-y-6">
                        <div className="flex items-center justify-between border-b border-slate-800/60 pb-3">
                            <h2 className="text-lg font-bold text-purple-400">
                                2. AI Role Assignments
                            </h2>
                            <button
                                type="button"
                                onClick={handleAddRole}
                                disabled={loading}
                                className="text-xs font-semibold bg-purple-950/50 text-purple-400 border border-purple-800/30 py-1.5 px-3 rounded-lg hover:bg-purple-900/50 transition-colors"
                            >
                                + Add Agent Role
                            </button>
                        </div>

                        <div className="space-y-4">
                            {roles.map((role, index) => (
                                <div key={index} className="flex flex-col md:flex-row items-stretch md:items-center gap-4 bg-slate-950/40 border border-slate-850 p-4 rounded-2xl relative group">
                                    {/* Role Name */}
                                    <div className="flex-1">
                                        <label className="block text-[10px] font-semibold uppercase tracking-widest text-slate-500 mb-1.5">
                                            Role Name (no @ prefix)
                                        </label>
                                        <input
                                            type="text"
                                            required
                                            disabled={loading}
                                            value={role.roleName}
                                            onChange={(e) => handleRoleChange(index, 'roleName', e.target.value.replace(/[^a-zA-Z0-9_-]/g, ''))}
                                            placeholder="e.g. Copywriter"
                                            className="w-full px-3 py-2 rounded-lg bg-slate-900/50 border border-slate-800 text-slate-200 placeholder-slate-650 focus:outline-none focus:border-purple-500 text-sm transition-all duration-150"
                                        />
                                    </div>

                                    {/* Model ID Dropdown */}
                                    <div className="flex-1">
                                        <label className="block text-[10px] font-semibold uppercase tracking-widest text-slate-500 mb-1.5">
                                            Assigned Model Engine
                                        </label>
                                        <select
                                            value={role.modelId}
                                            disabled={loading}
                                            onChange={(e) => handleRoleChange(index, 'modelId', e.target.value)}
                                            className="w-full px-3 py-2 rounded-lg bg-slate-900/50 border border-slate-800 text-slate-300 focus:outline-none focus:border-purple-500 text-sm transition-all duration-150"
                                        >
                                            {MODEL_OPTIONS.map(opt => (
                                                <option key={opt.value} value={opt.value} className="bg-slate-950 text-slate-300">
                                                    {opt.label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    {/* Color Select */}
                                    <div>
                                        <label className="block text-[10px] font-semibold uppercase tracking-widest text-slate-500 mb-1.5">
                                            UI Theme Color
                                        </label>
                                        <div className="flex items-center space-x-2 py-1">
                                            {PRESET_COLORS.map(color => (
                                                <button
                                                    key={color}
                                                    type="button"
                                                    disabled={loading}
                                                    onClick={() => handleRoleChange(index, 'uiColorHex', color)}
                                                    className={`h-6 w-6 rounded-full border transition-transform duration-100 ${
                                                        role.uiColorHex.toLowerCase() === color.toLowerCase()
                                                            ? 'scale-125 border-white shadow-md'
                                                            : 'border-slate-800 hover:scale-110'
                                                    }`}
                                                    style={{ backgroundColor: color }}
                                                />
                                            ))}
                                        </div>
                                    </div>

                                    {/* Delete Button */}
                                    {roles.length > 1 && (
                                        <div className="flex items-end justify-end md:justify-center md:pt-4">
                                            <button
                                                type="button"
                                                onClick={() => handleRemoveRole(index)}
                                                disabled={loading}
                                                className="text-xs text-red-500/80 hover:text-red-400 font-semibold p-2"
                                            >
                                                Remove
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Submit Button */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-4 rounded-2xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 font-bold text-white shadow-xl shadow-purple-900/25 hover:scale-[1.01] active:scale-[0.99] transition-all duration-150 flex items-center justify-center text-md"
                    >
                        {loading ? (
                            <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                        ) : (
                            'Initialize Consensus Workspace'
                        )}
                    </button>
                </form>
            </main>
        </div>
    );
}
