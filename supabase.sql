-- Rode isto UMA vez no SQL Editor do Supabase (pode rodar de novo sem problema)

-- 1) Tabela que guarda tudo do app
create table if not exists kv (
  key text primary key,
  value jsonb not null,
  updated_at timestamptz default now()
);
alter table kv enable row level security;
drop policy if exists "casal_all" on kv;
create policy "casal_all" on kv for all using (true) with check (true);

-- 2) Pasta de fotos (Storage)
insert into storage.buckets (id, name, public) values ('fotos', 'fotos', true) on conflict (id) do nothing;
drop policy if exists "fotos_all" on storage.objects;
create policy "fotos_all" on storage.objects for all using (bucket_id = 'fotos') with check (bucket_id = 'fotos');
