# Nosso Cantinho 💗 (versão 2)

Abas: Início (carinho + humor do dia), Afazeres, Fotos, Recados, Mais (cupons, desejos, cápsula do tempo, Nós, Ajustes).

## Como atualizar o app no GitHub
Suba/substitua no repositório os arquivos desta pasta (arraste tudo, o GitHub substitui os que já existem).
O build do APK começa sozinho. Depois é só baixar o novo APK e instalar POR CIMA do antigo (não precisa desinstalar).

## 1) Supabase (dados + fotos)
1. SQL Editor > cole o `supabase.sql` inteiro > Run. (Se você já rodou a versão antiga, rode este de novo: ele só acrescenta as fotos.)
2. Settings > API: copie "Project URL" e "anon public" e cole no app (Mais > Ajustes).
3. Use o MESMO código secreto nos dois celulares.

## 2) Notificações push (Firebase + Supabase)
### 2.1 Firebase
1. console.firebase.google.com > Adicionar projeto (nome: nosso-cantinho, desligue o Analytics).
2. No projeto: ícone do Android (Adicionar app) > nome do pacote: `com.nosso.cantinho` > Registrar > baixe o `google-services.json`.
3. Suba o `google-services.json` na RAIZ do repositório GitHub (junto do package.json).
4. Engrenagem > Configurações do projeto > Contas de serviço > "Gerar nova chave privada". Baixa um arquivo .json (guarde, NÃO suba no GitHub).
### 2.2 Supabase
1. Edge Functions > Deploy a new function > Via Editor. Nome: `notify`.
2. Apague o exemplo e cole o conteúdo de `supabase-function/notify.ts`. Deploy.
3. Edge Functions > Secrets > Add new secret: nome `FIREBASE_SERVICE_ACCOUNT`, valor = TODO o conteúdo do .json do passo 2.1.4.
### 2.3 No celular
Reinstale o APK novo, abra o app, toque em "Permitir" nas notificações. Pronto: pedidos, recados, cupons, fotos etc. avisam o outro celular.

## 3) Repositório privado (recomendado)
Depois de subir o google-services.json: Settings > Danger Zone > Change visibility > Private. O Actions continua funcionando.

## Testar no computador
`npm run serve` e abra http://localhost:5173
