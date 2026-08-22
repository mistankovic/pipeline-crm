import { Link, useRouterState } from "@tanstack/react-router";
import { RedirectToSignIn, UserButton } from "@/lib/auth/gates";
import { useCurrentUserState } from "@/lib/auth/use-current-user";
import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

const NAV = [
  { to: "/", label: "Board" },
  { to: "/companies", label: "Companies" },
  { to: "/contacts", label: "Contacts" },
  { to: "/forecast", label: "Forecast" },
] as const;

export function AppShell({ children }: { children: ReactNode }) {
  const { user, isPending } = useCurrentUserState();
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  if (isPending) {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <header className="border-b border-border px-6 py-3">
          <p className="font-display text-2xl tracking-tight">Pipeline</p>
        </header>
        <div className="m-6 h-64 animate-pulse rounded-xl bg-card" />
      </div>
    );
  }
  if (!user) return <RedirectToSignIn />;

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-30 border-b border-border bg-background/90 backdrop-blur">
        <div className="mx-auto flex max-w-[1600px] items-center gap-4 px-4 py-3 md:px-6">
          <Link to="/" className="font-display text-2xl tracking-tight">
            Pipeline
          </Link>
          <nav className="hidden items-center gap-1 md:flex">
            {NAV.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className={cn(
                  "rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground",
                  pathname === item.to && "bg-accent text-foreground",
                )}
              >
                {item.label}
              </Link>
            ))}
          </nav>
          <div className="ml-auto">
            <UserButton />
          </div>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-4 pb-3 md:hidden">
          {NAV.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={cn(
                "shrink-0 rounded-md px-3 py-2 text-sm text-muted-foreground",
                pathname === item.to && "bg-accent text-foreground",
              )}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </header>
      <div className="mx-auto max-w-[1600px] px-4 py-6 md:px-6">{children}</div>
    </div>
  );
}
