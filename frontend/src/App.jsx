import { useState } from 'react'

function App() {
  const [count, setCount] = useState(0);
  const [wsConnected, setWsConnected] = useState(false);

  // Model statuses to display on the dashboard
  const models = [
    { name: 'Gemini 1.5 Flash', provider: 'Google Vertex AI', type: 'Live (Primary)', color: 'from-blue-600 to-cyan-500', status: 'Ready' },
    { name: 'GPT-4o', provider: 'OpenAI', type: 'Fake / Simulated', color: 'from-green-600 to-emerald-400', status: 'Mocked' },
    { name: 'Claude 3.5 Sonnet', provider: 'Anthropic', type: 'Fake / Simulated', color: 'from-orange-600 to-amber-400', status: 'Mocked' }
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-purple-500 selection:text-white">
      {/* Background glow effects */}
      <div className="absolute top-0 left-1/4 w-96 h-96 bg-purple-900/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-blue-900/20 rounded-full blur-3xl pointer-events-none" />

      {/* Header */}
      <header className="border-b border-slate-900 bg-slate-950/80 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-purple-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-purple-950/50">
              <span className="font-extrabold text-lg text-white">C</span>
            </div>
            <div>
              <span className="font-bold text-lg tracking-wider bg-clip-text text-transparent bg-gradient-to-r from-purple-400 to-indigo-200">
                CONCLAVE
              </span>
              <span className="text-[10px] block text-slate-500 font-mono tracking-widest uppercase">
                AI Orchestration
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-purple-950/50 text-purple-400 border border-purple-800/30">
              Phase 01: Setup & Infrastructure
            </span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight mb-4 bg-clip-text text-transparent bg-gradient-to-b from-white via-slate-100 to-slate-400">
            Multi-Provider Consensus Workspace
          </h1>
          <p className="text-lg text-slate-400 leading-relaxed">
            A collaborative environment where multiple AI models debate, review, and refine complex plans in real-time.
          </p>
        </div>

        {/* Status Indicators Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          {/* PostgreSQL Status */}
          <div className="bg-slate-900/50 border border-slate-800/60 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Database Connection</span>
              <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
            </div>
            <h3 className="text-2xl font-bold text-emerald-400">PostgreSQL 16</h3>
            <p className="text-xs text-slate-500 mt-2 font-mono">Host: localhost:5432 / conclave_db</p>
            <div className="mt-4 text-xs py-1.5 px-3 rounded-lg bg-emerald-950/20 text-emerald-400 border border-emerald-800/30 inline-block">
              Connection Verified
            </div>
          </div>

          {/* Spring Boot Backend Status */}
          <div className="bg-slate-900/50 border border-slate-800/60 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Backend Environment</span>
              <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
            </div>
            <h3 className="text-2xl font-bold text-indigo-400">Spring Boot 3.3.1</h3>
            <p className="text-xs text-slate-500 mt-2 font-mono">Java 21 / Spring AI 1.0.0-M1</p>
            <div className="mt-4 text-xs py-1.5 px-3 rounded-lg bg-indigo-950/20 text-indigo-400 border border-indigo-800/30 inline-block">
              Spring Context Loaded
            </div>
          </div>

          {/* WebSocket / STOMP Status */}
          <div className="bg-slate-900/50 border border-slate-800/60 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm font-semibold text-slate-400 uppercase tracking-wider">WebSocket Stream</span>
              <span className={`h-2 w-2 rounded-full ${wsConnected ? 'bg-emerald-500' : 'bg-amber-500'} animate-pulse`} />
            </div>
            <h3 className={`text-2xl font-bold ${wsConnected ? 'text-emerald-400' : 'text-amber-400'}`}>
              {wsConnected ? 'STOMP Connected' : 'STOMP Standby'}
            </h3>
            <p className="text-xs text-slate-500 mt-2 font-mono">Client: @stomp/stompjs</p>
            <button
              onClick={() => setWsConnected(!wsConnected)}
              className={`mt-4 text-xs font-semibold py-1.5 px-4 rounded-lg transition-all duration-250 ${
                wsConnected
                  ? 'bg-amber-600 hover:bg-amber-700 text-white'
                  : 'bg-indigo-600 hover:bg-indigo-700 text-white'
              }`}
            >
              {wsConnected ? 'Simulate Disconnect' : 'Simulate Connect'}
            </button>
          </div>
        </div>

        {/* AI Model Registry Panel */}
        <section className="mb-12">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold">Planned Model Registry</h2>
            <span className="text-xs text-slate-500 font-mono">Multi-vendor Integration</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {models.map((model, idx) => (
              <div key={idx} className="bg-slate-900/30 border border-slate-850 hover:border-slate-800 transition-all duration-300 rounded-2xl p-6 group">
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xs font-mono text-slate-500">{model.provider}</span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold tracking-wider ${
                    model.status === 'Ready' 
                      ? 'bg-blue-950 text-blue-400 border border-blue-900/50' 
                      : 'bg-slate-800 text-slate-400'
                  }`}>
                    {model.status}
                  </span>
                </div>
                <h4 className="text-lg font-bold group-hover:text-white transition-colors">{model.name}</h4>
                <p className="text-xs text-slate-400 mt-1">{model.type}</p>
                <div className="w-full bg-slate-800 h-1.5 rounded-full mt-6 overflow-hidden">
                  <div className={`h-full bg-gradient-to-r ${model.color} w-full`} />
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Tailwind Verification Box */}
        <div className="bg-gradient-to-r from-purple-950/40 via-indigo-950/40 to-slate-950/40 border border-purple-900/20 rounded-3xl p-8 text-center relative overflow-hidden shadow-2xl">
          <div className="absolute inset-0 bg-grid-white/[0.02] bg-[size:30px_30px]" />
          <div className="relative z-10">
            <h3 className="text-xl font-bold mb-2">Tailwind HMR & Color Test</h3>
            <p className="text-sm text-slate-400 mb-6 max-w-md mx-auto">
              Click the button below to test React state updates and verify tailwind animations and dynamic styling work.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <button
                type="button"
                onClick={() => setCount(count + 1)}
                className="px-6 py-3 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 font-semibold text-white shadow-lg shadow-purple-900/25 hover:scale-105 active:scale-95 transition-all duration-150"
              >
                Increment Counter: <span className="bg-purple-950/80 px-2 py-0.5 rounded ml-1 font-mono">{count}</span>
              </button>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 py-6 bg-slate-950">
        <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between text-xs text-slate-500">
          <span>&copy; {new Date().getFullYear()} Conclave Monorepo. All rights reserved.</span>
          <div className="flex space-x-6 mt-4 md:mt-0">
            <span className="hover:text-slate-300 cursor-default">Spring Boot 3.3.1</span>
            <span className="hover:text-slate-300 cursor-default">React 19.2</span>
            <span className="hover:text-slate-300 cursor-default">Tailwind CSS v3</span>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default App
