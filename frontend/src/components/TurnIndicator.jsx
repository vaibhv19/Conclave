export default function TurnIndicator({ roleName, roleColor = '#3B82F6' }) {
    return (
        <div className="flex items-center space-x-3 bg-slate-900/30 border border-slate-800/80 px-4 py-3 rounded-2xl animate-pulse duration-1000 max-w-xs mb-6 select-none border-l-4"
             style={{ borderLeftColor: roleColor }}>
            {/* Pulsing indicator dot */}
            <div className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full opacity-75" style={{ backgroundColor: roleColor }} />
                <span className="relative inline-flex rounded-full h-2 w-2" style={{ backgroundColor: roleColor }} />
            </div>

            <div className="flex items-center space-x-1.5 text-xs text-slate-400 font-mono">
                <span>@{roleName} is streaming</span>
                <span className="inline-flex space-x-0.5">
                    <span className="h-1 w-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="h-1 w-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="h-1 w-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                </span>
            </div>
        </div>
    );
}
