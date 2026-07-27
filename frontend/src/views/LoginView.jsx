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
        <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center font-sans selection:bg-purple-500 selection:text-white relative p-4 overflow-hidden">
            {/* Background blur effects */}
            <div className="absolute top-0 left-1/4 w-96 h-96 bg-purple-900/20 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-blue-900/20 rounded-full blur-3xl pointer-events-none" />

            {/* Glassmorphic card */}
            <div className="w-full max-w-md bg-slate-900/50 border border-slate-800/60 rounded-3xl p-8 shadow-2xl backdrop-blur-md relative z-10">
                <div className="text-center mb-8">
                    <div className="h-12 w-12 rounded-2xl bg-gradient-to-tr from-purple-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-purple-950/50 mx-auto mb-4">
                        <span className="font-extrabold text-xl text-white">C</span>
                    </div>
                    <h2 className="text-3xl font-extrabold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white via-slate-100 to-slate-400">
                        Welcome Back
                    </h2>
                    <p className="text-sm text-slate-400 mt-2">
                        Sign in to enter the Conclave AI workspace
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    {error && (
                        <div className="bg-red-950/50 border border-red-800/30 text-red-400 px-4 py-3 rounded-xl text-sm relative">
                            <span className="block sm:inline">{error}</span>
                            <button
                                type="button"
                                onClick={clearError}
                                className="absolute top-0 bottom-0 right-0 px-4 py-3 text-red-400 hover:text-red-300 font-bold"
                            >
                                &times;
                            </button>
                        </div>
                    )}

                    <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                            Email Address
                        </label>
                        <input
                            type="email"
                            required
                            disabled={loading}
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="you@example.com"
                            className="w-full px-4 py-3 rounded-xl bg-slate-950/50 border border-slate-800 text-slate-100 placeholder-slate-600 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-all duration-200"
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                            Password
                        </label>
                        <input
                            type="password"
                            required
                            disabled={loading}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            className="w-full px-4 py-3 rounded-xl bg-slate-950/50 border border-slate-800 text-slate-100 placeholder-slate-600 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-all duration-200"
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 font-bold text-white shadow-lg shadow-purple-900/25 hover:scale-[1.02] active:scale-[0.98] transition-all duration-150 flex items-center justify-center"
                    >
                        {loading ? (
                            <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                        ) : (
                            'Sign In'
                        )}
                    </button>
                </form>

                <div className="mt-8 text-center text-xs text-slate-500">
                    Don't have an account?{' '}
                    <button
                        onClick={onSwitchView}
                        disabled={loading}
                        className="text-purple-400 hover:text-purple-300 font-semibold"
                    >
                        Sign Up
                    </button>
                </div>
            </div>
        </div>
    );
}
