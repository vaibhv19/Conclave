import { useState } from 'react';
import { useAuthStore } from '../store/authStore';

export default function LoginView({ onSwitchView }) {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const { login, loading, error, clearError } = useAuthStore();

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!email.trim() || !password.trim()) return;

        try {
            await login(email.trim(), password);
        } catch (err) {
            // Error is handled in store
        }
    };

    return (
        <div className="min-h-screen bg-brand-bg text-zinc-100 flex flex-col items-center justify-center font-sans p-4 relative select-none">
            {/* Minimal Desktop-style Login Panel */}
            <div className="w-full max-w-sm bg-brand-card border border-brand-border rounded-xl p-8 shadow-2xl relative">
                <div className="text-center mb-8">
                    {/* Branded Logo Square */}
                    <div className="h-10 w-10 rounded-lg bg-brand-accent flex items-center justify-center mx-auto mb-4 select-none pointer-events-none">
                        <span className="font-mono font-black text-lg text-white">C</span>
                    </div>
                    <h2 className="text-xl font-bold tracking-tight text-white">
                        Welcome Back
                    </h2>
                    <p className="text-xs text-brand-textMuted mt-1">
                        Sign in to access the AI strategy room
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5">
                    {error && (
                        <div className="bg-red-950/20 border border-red-900/40 text-red-400 px-3.5 py-2.5 rounded-lg text-xs relative flex items-center justify-between">
                            <span className="font-mono">{error}</span>
                            <button
                                type="button"
                                onClick={clearError}
                                className="text-red-400 hover:text-red-300 font-bold focus:outline-none ml-2"
                                aria-label="Close error"
                            >
                                &times;
                            </button>
                        </div>
                    )}

                    <div className="space-y-1.5">
                        <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-brand-textMuted">
                            Email Address
                        </label>
                        <input
                            type="email"
                            required
                            disabled={loading}
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="you@example.com"
                            className="w-full px-3 py-2 text-xs rounded-lg bg-brand-surface border border-brand-border text-zinc-100 placeholder-zinc-700 focus:outline-none focus:border-brand-borderLight transition-colors duration-150 font-mono"
                        />
                    </div>

                    <div className="space-y-1.5">
                        <label className="block text-[10px] font-mono font-bold uppercase tracking-wider text-brand-textMuted">
                            Password
                        </label>
                        <input
                            type="password"
                            required
                            disabled={loading}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            className="w-full px-3 py-2 text-xs rounded-lg bg-brand-surface border border-brand-border text-zinc-100 placeholder-zinc-700 focus:outline-none focus:border-brand-borderLight transition-colors duration-150 font-mono"
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-2 rounded-lg bg-brand-accent hover:bg-brand-accentHover text-xs font-semibold text-white transition-colors duration-150 flex items-center justify-center focus:outline-none focus:ring-1 focus:ring-brand-accent/50"
                    >
                        {loading ? (
                            <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                        ) : (
                            'Sign In'
                        )}
                    </button>
                </form>

                <div className="mt-6 text-center text-xs text-brand-textMuted font-mono">
                    New to Conclave?{' '}
                    <button
                        onClick={onSwitchView}
                        disabled={loading}
                        className="text-brand-accent hover:text-brand-accentHover font-semibold transition-colors duration-150 focus:outline-none"
                    >
                        Sign Up
                    </button>
                </div>
            </div>
        </div>
    );
}
