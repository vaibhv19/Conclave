import { useState } from 'react';
import { useAuthStore } from '../store/authStore';

export default function RegisterView({ onSwitchView }) {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const { register, loading, error, clearError } = useAuthStore();

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!name.trim() || !email.trim() || !password.trim()) return;

        try {
            await register(name.trim(), email.trim(), password);
        } catch (err) {
            // Error is handled in store
        }
    };

    return (
        <div className="min-h-screen bg-brand-bg text-brand-textPrimary flex flex-col items-center justify-center font-sans p-4 relative select-none">
            {/* Desktop-grade Register Card */}
            <div className="w-full max-w-sm bg-brand-panel border border-brand-border rounded-xl p-8 shadow-2xl relative">
                <div className="text-center mb-8">
                    <svg className="h-9 w-9 text-brand-accent mx-auto mb-4" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <circle cx="50" cy="50" r="42" stroke="currentColor" strokeWidth="2" strokeDasharray="4 4" className="opacity-30" />
                        <path d="M50 12 C57 37 73 43 88 50 C73 57 57 63 50 88 C43 57 27 50 12 50 C27 43 43 37 50 12 Z" fill="currentColor" />
                    </svg>

                    <h2 className="text-lg font-bold tracking-tight text-white font-mono uppercase">
                        Create Account
                    </h2>
                    <p className="text-[11px] text-brand-textMuted mt-1">
                        Register to start orchestrating AI consensus.
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5">
                    {error && (
                        <div className="bg-red-950/20 border border-red-900/40 text-red-450 px-3.5 py-2.5 rounded text-[10px] font-mono relative flex items-center justify-between">
                            <span className="truncate pr-2">{error}</span>
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

                    <div className="space-y-1.5">
                        <label className="block text-[9px] font-mono font-bold uppercase tracking-wider text-brand-textMuted">
                            Full Name
                        </label>
                        <input
                            type="text"
                            required
                            disabled={loading}
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="John Doe"
                            className="w-full px-3 py-2 text-xs rounded bg-brand-surface border border-brand-border text-brand-textPrimary placeholder-zinc-700 focus:outline-none focus:border-brand-borderLight transition-colors font-mono"
                        />
                    </div>

                    <div className="space-y-1.5">
                        <label className="block text-[9px] font-mono font-bold uppercase tracking-wider text-brand-textMuted">
                            Email Address
                        </label>
                        <input
                            type="email"
                            required
                            disabled={loading}
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="you@example.com"
                            className="w-full px-3 py-2 text-xs rounded bg-brand-surface border border-brand-border text-brand-textPrimary placeholder-zinc-700 focus:outline-none focus:border-brand-borderLight transition-colors font-mono"
                        />
                    </div>

                    <div className="space-y-1.5">
                        <label className="block text-[9px] font-mono font-bold uppercase tracking-wider text-brand-textMuted">
                            Password
                        </label>
                        <input
                            type="password"
                            required
                            disabled={loading}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            className="w-full px-3 py-2 text-xs rounded bg-brand-surface border border-brand-border text-brand-textPrimary placeholder-zinc-700 focus:outline-none focus:border-brand-borderLight transition-colors font-mono"
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-2 rounded bg-brand-accent hover:bg-brand-accentHover text-xs font-mono font-bold uppercase tracking-wider text-white transition-colors flex items-center justify-center focus:outline-none"
                    >
                        {loading ? (
                            <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                        ) : (
                            'Create Account'
                        )}
                    </button>
                </form>

                <div className="mt-6 text-center text-[10px] text-brand-textMuted font-mono uppercase tracking-wide">
                    Already have an account?{' '}
                    <button
                        onClick={onSwitchView}
                        disabled={loading}
                        className="text-brand-accent hover:text-brand-accentHover font-bold transition-colors focus:outline-none"
                    >
                        Sign In
                    </button>
                </div>
            </div>
        </div>
    );
}
