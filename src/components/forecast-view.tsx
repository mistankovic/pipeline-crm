import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { ForecastDto, WorkspaceDto } from "@/lib/crm-api";
import { moneyLabel } from "@/lib/utils";

function chartRows(forecast: ForecastDto, currency: string) {
  return forecast.buckets.map((bucket) => ({
    name: bucket.key,
    value: bucket.totals.find((total) => total.currency === currency)?.weightedAmount ?? 0,
  }));
}

function Totals({ forecast, title }: { forecast: ForecastDto; title: string }) {
  const currencies = [...new Set(forecast.grand.map((item) => item.currency))];
  return (
    <section className="rounded-xl border border-border bg-card p-5">
      <h2 className="font-display text-2xl tracking-tight">{title}</h2>
      <div className="mt-4 flex flex-wrap gap-6">
        {forecast.grand.length === 0 ? (
          <p className="text-sm text-muted-foreground">No open pipeline.</p>
        ) : (
          forecast.grand.map((total) => (
            <div key={total.currency}>
              <p className="text-xs tracking-wide text-muted-foreground uppercase">
                Weighted {total.currency}
              </p>
              <p className="mt-1 font-mono text-2xl tabular-nums">
                {moneyLabel(total.weightedAmount, total.currency)}
              </p>
            </div>
          ))
        )}
      </div>
      {currencies.map((currency) => (
        <div key={currency} className="mt-6 h-56">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartRows(forecast, currency)}>
              <XAxis dataKey="name" stroke="var(--color-muted-foreground)" fontSize={12} />
              <YAxis stroke="var(--color-muted-foreground)" fontSize={12} />
              <Tooltip
                contentStyle={{
                  background: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: 12,
                }}
                formatter={(value) => moneyLabel(Number(value ?? 0), currency)}
              />
              <Bar dataKey="value" fill="var(--color-primary)" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ))}
    </section>
  );
}

export function ForecastView({ workspace }: { workspace: WorkspaceDto }) {
  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs font-medium tracking-[0.18em] text-muted-foreground uppercase">
          Weighted pipeline
        </p>
        <h1 className="font-display text-4xl tracking-tight">Forecast</h1>
        <p className="mt-2 max-w-xl text-sm text-muted-foreground">
          Open deals only. Each bar is value times probability, kept in its own currency.
        </p>
      </div>
      <Totals forecast={workspace.forecastByStage} title="By stage" />
      <Totals forecast={workspace.forecastByOwner} title="By owner" />
    </div>
  );
}
