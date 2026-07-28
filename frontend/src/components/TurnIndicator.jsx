export default function TurnIndicator({ roleName, roleColor = '#3B82F6' }) {
    return (
        <div className="flex items-center space-x-2.5 bg-brand-card border border-brand-border px-3.5 py-2 rounded-lg max-w-xs mb-5 select-none border-l-[3px]"
             style={{ borderLeftColor: roleColor }}>
            {/* Pulsing indicator dot */}
            <div className="relative flex h-1.5 w-1.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full opacity-75" style={{ backgroundColor: roleColor }} />
                <span className="relative inline-flex rounded-full h-1.5 w-1.5" style={{ backgroundColor: roleColor }} />
            </div>

            <div className="flex items-center space-x-1 text-[10px] text-zinc-400 font-mono">
                <span>@{roleName} is streaming</span>
                <span className="inline-flex space-x-0.5">
                    <span className="h-0.5 w-0.5 bg-zinc-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="h-0.5 w-0.5 bg-zinc-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="h-0.5 w-0.5 bg-zinc-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                </span>
            </div>
        </div>
    );
}
