-- Rode isto uma vez no SQL Editor do Supabase (projeto gratuito)
create table if not exists kv (
  key text primary key,
  value jsonb not null,
  updated_at timestamptz default now()
);
alter table kv enable row level security;
create policy "casal_all" on kv for all using (true) with check (true);
