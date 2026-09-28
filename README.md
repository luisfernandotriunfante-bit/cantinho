# Nosso Cantinho 💗

App só de vocês dois. Abas: Carinho, Afazeres, Fotos, Recados, Nós (contador, datas, tema).

## 1) Sincronizar os dois celulares (Supabase, grátis, ~5 min)
1. Crie conta em supabase.com e um projeto novo.
2. SQL Editor > cole o conteúdo de `supabase.sql` > Run.
3. Settings > API: copie a "Project URL" e a chave "anon public".
4. Na primeira abertura do app (nos dois celulares), cole URL e chave e use o MESMO código secreto.
Sem isso o app funciona, mas cada celular guarda só os próprios dados.

## 2) Gerar o APK (sem instalar nada: GitHub Actions)
1. Crie um repositório PRIVADO no GitHub e suba esta pasta.
2. Aba Actions > "Gerar APK" > Run workflow (leva ~5 min).
3. Baixe o artefato `nosso-cantinho-apk`, extraia e mande o `app-debug.apk` pelo WhatsApp.
4. No Android: abra o arquivo e permita "instalar de fontes desconhecidas".

## Alternativa: Android Studio
`npm install && npx cap add android && npx cap sync android`, abra a pasta `android/` no Android Studio e use Build > Build APK.

## Testar no computador
`npm run serve` e abra http://localhost:5173
