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
        <div className="min-h-screen bg-[#08080A] text-brand-textPrimary flex flex-col md:flex-row font-sans selection:bg-brand-accent/20 overflow-hidden">
            {/* Left Side: Brand & Product Value Proposition (hidden on mobile/tablet) */}
            <div className="hidden md:flex md:w-[45%] lg:w-[40%] bg-[#0c0c0e] border-r border-brand-border p-12 flex-col justify-between relative overflow-hidden select-none">
                {/* Background Grid Pattern */}
                <div className="absolute inset-0 opacity-5 pointer-events-none bg-[linear-gradient(to_right,#808080_1px,transparent_1px),linear-gradient(to_bottom,#808080_1px,transparent_1px)] bg-[size:24px_24px]" />
                
                {/* Brand Header */}
                <div className="flex items-center space-x-3 z-10">
                    <svg className="h-6 w-6 text-brand-accent" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <circle cx="50" cy="50" r="42" stroke="currentColor" strokeWidth="2" strokeDasharray="4 4" className="opacity-30" />
                        <path d="M50 12 C57 37 73 43 88 50 C73 57 57 63 50 88 C43 57 27 50 12 50 C27 43 43 37 50 12 Z" fill="currentColor" />
                    </svg>
                    <span className="font-mono text-xs font-bold uppercase tracking-widest text-white">CONCLAVE</span>
                </div>

                {/* Features & Proposal */}
                <div className="space-y-6 z-10 max-w-sm">
                    <h1 className="text-xl font-bold tracking-tight text-white font-mono uppercase">
                        AI Consensus & Orchestration
                    </h1>
                    <p className="text-xs text-brand-textSecondary leading-relaxed">
                        Conclave is an AI Strategy Room. Multiple LLM engines debate, review, and converge contexts inside a unified tactical workspace.
                    </p>
                    
                    <div className="space-y-4 pt-6 border-t border-brand-border/60">
                        <div className="flex items-start space-x-3">
                            <span className="text-[10px] font-mono text-brand-accent mt-0.5">01 //</span>
                            <div>
                                <h4 className="text-[10px] font-mono font-bold uppercase text-zinc-350 tracking-wider">Multi-Agent Pipelines</h4>
                                <p className="text-[9px] text-brand-textMuted mt-0.5 leading-relaxed">Chain Google Gemini, OpenAI GPT, and Anthropic Claude in sequence.</p>
                            </div>
                        </div>
                        <div className="flex items-start space-x-3">
                            <span className="text-[10px] font-mono text-brand-accent mt-0.5">02 //</span>
                            <div>
                                <h4 className="text-[10px] font-mono font-bold uppercase text-zinc-350 tracking-wider">Context Unification</h4>
                                <p className="text-[9px] text-brand-textMuted mt-0.5 leading-relaxed">Canonical message models abstracting separate vendor message schemas.</p>
                            </div>
                        </div>
                        <div className="flex items-start space-x-3">
                            <span className="text-[10px] font-mono text-brand-accent mt-0.5">03 //</span>
                            <div>
                                <h4 className="text-[10px] font-mono font-bold uppercase text-zinc-350 tracking-wider">Tactical Suspends</h4>
                                <p className="text-[9px] text-brand-textMuted mt-0.5 leading-relaxed">Halt executions to inject feedback, edits, and directions manually.</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Telemetry Indicator footer */}
                <div className="z-10 font-mono text-[9px] text-zinc-600 flex items-center justify-between border-t border-brand-border/40 pt-4">
                    <span>SYS_STATUS::READY</span>
                    <span>v1.0.1</span>
                </div>
            </div>

            {/* Right Side: Authentication Panel */}
            <div className="flex-1 flex flex-col justify-center items-center p-8 bg-[#121214]">
                <div className="w-full max-w-sm space-y-8">
                    {/* Header info */}
                    <div className="text-left">
                        {/* Show logo icon on mobile/tablet only (since left sidebar is hidden) */}
                        <svg className="h-8 w-8 text-brand-accent mb-4 md:hidden" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="50" cy="50" r="42" stroke="currentColor" strokeWidth="2" strokeDasharray="4 4" className="opacity-30" />
                            <path d="M50 12 C57 37 73 43 88 50 C73 57 57 63 50 88 C43 57 27 50 12 50 C27 43 43 37 50 12 Z" fill="currentColor" />
                        </svg>

                        <h2 className="text-lg font-bold tracking-tight text-white font-mono uppercase">
                            Welcome Back
                        </h2>
                        <p className="text-[11px] text-brand-textMuted mt-1.5">
                            Sign in to orchestrate the AI consensus workspace.
                        </p>
                    </div>

                    {/* Auth Form */}
                    <form onSubmit={handleSubmit} className="space-y-5">
                        {error && (
                            <div className="bg-red-950/20 border border-red-900/40 text-red-400 px-3.5 py-2.5 rounded text-[10px] font-mono relative flex items-center justify-between">
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

                        <div className="space-y-2">
                            <label htmlFor="email" className="block text-[9px] font-mono font-bold uppercase tracking-wider text-brand-textMuted">
                                Email Address
                            </label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                autoComplete="email"
                                required
                                disabled={loading}
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="you@example.com"
                                className="w-full px-3 py-2 text-xs rounded bg-brand-surface border border-brand-border text-brand-textPrimary placeholder-zinc-700 hover:border-zinc-800 focus:outline-none focus:border-brand-borderLight focus:ring-1 focus:ring-brand-borderLight disabled:opacity-40 disabled:cursor-not-allowed transition-all font-mono"
                            />
                        </div>

                        <div className="space-y-2">
                            <label htmlFor="password" className="block text-[9px] font-mono font-bold uppercase tracking-wider text-brand-textMuted">
                                Password
                            </label>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                autoComplete="current-password"
                                required
                                disabled={loading}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                className="w-full px-3 py-2 text-xs rounded bg-brand-surface border border-brand-border text-brand-textPrimary placeholder-zinc-700 hover:border-zinc-800 focus:outline-none focus:border-brand-borderLight focus:ring-1 focus:ring-brand-borderLight disabled:opacity-40 disabled:cursor-not-allowed transition-all font-mono"
                            />
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-2.5 rounded bg-brand-accent hover:bg-brand-accentHover text-xs font-mono font-bold uppercase tracking-wider text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center focus:outline-none"
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

                    <div className="text-center text-[10px] text-brand-textMuted font-mono uppercase tracking-wide">
                        New to Conclave?{' '}
                        <button
                            onClick={onSwitchView}
                            disabled={loading}
                            className="text-brand-accent hover:text-brand-accentHover font-bold transition-colors focus:outline-none"
                        >
                            Sign Up
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
