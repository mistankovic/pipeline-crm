import { createFileRoute, Navigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { GROK_PROVIDERS, authClient, authEnabled, signIn } from "@/lib/auth/client";
import { useCurrentUserState } from "@/lib/auth/use-current-user";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/login")({ component: Login });

export function Login() {
  const { user, isPending } = useCurrentUserState();
  const [mode, setMode] = useState<"in" | "up">("in");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" />;

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setBusy(true);
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "");
    const password = String(form.get("password") ?? "");
    const name = String(form.get("name") ?? "");
    try {
      if (mode === "up") {
        const result = await authClient.signUp.email({ email, password, name });
        if (result.error) setError(result.error.message ?? "Could not create the account");
      } else {
        const result = await authClient.signIn.email({ email, password });
        if (result.error) setError(result.error.message ?? "Could not sign in");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Sign-in failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="grid min-h-screen place-items-center px-4 py-10">
      <div className="w-full max-w-md space-y-8">
        <div>
          <p className="text-xs font-medium tracking-[0.2em] text-muted-foreground uppercase">
            Sales desk
          </p>
          <h1 className="mt-2 font-display text-5xl tracking-tight">Pipeline</h1>
          <p className="mt-3 text-sm text-muted-foreground">
            Sign in to the board. Deals only move when the rules say they can.
          </p>
        </div>
        {authEnabled ? (
          <>
            <form className="grid gap-3 rounded-xl border border-border bg-card p-5" onSubmit={onSubmit}>
              {mode === "up" && (
                <label className="grid gap-1.5">
                  <Label>Name</Label>
                  <Input name="name" required autoComplete="name" />
                </label>
              )}
              <label className="grid gap-1.5">
                <Label>Email</Label>
                <Input name="email" type="email" required autoComplete="email" />
              </label>
              <label className="grid gap-1.5">
                <Label>Password</Label>
                <Input
                  name="password"
                  type="password"
                  required
                  minLength={8}
                  autoComplete={mode === "up" ? "new-password" : "current-password"}
                />
              </label>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Button type="submit" disabled={busy || isPending}>
                {mode === "up" ? "Create account" : "Sign in"}
              </Button>
              <button
                type="button"
                className="text-sm text-muted-foreground"
                onClick={() => setMode(mode === "up" ? "in" : "up")}
              >
                {mode === "up" ? "Have an account? Sign in" : "Need an account? Create one"}
              </button>
            </form>
            <div className="grid gap-2">
              {GROK_PROVIDERS.map((provider) => (
                <Button
                  key={provider.providerId}
                  type="button"
                  variant="outline"
                  onClick={() => signIn(provider.providerId, { callbackURL: "/" })}
                >
                  Continue with {provider.label}
                </Button>
              ))}
            </div>
          </>
        ) : (
          <p className="text-sm text-muted-foreground">Sign-in is disabled.</p>
        )}
      </div>
    </main>
  );
}
