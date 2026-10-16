# Operação na Hostinger

VPS: `[REDACTED]` (Ubuntu 24.04).

- Serviço: `unknown-discord-bot.service`.
- Aplicação: `/opt/unknown-discord-bot`.
- Configuração secreta: `/etc/unknown-discord-bot/bot.env`, com modo `0600`, acessível apenas por root.
- Estado dos tickets e painéis: `/var/lib/unknown-discord-bot/data/state.json`.
- Logs de captcha: `/var/lib/unknown-discord-bot/captcha-debug.log`, criados quando necessários.
- Usuário do serviço: `unknown-discord-bot`.
- Aplicação Discord: `[REDACTED]`.
- Servidor Discord: `[REDACTED]` (`unknown`).

O processo fica ativo após fechar o terminal e inicia automaticamente ao ligar a VPS. Ele não exige portas públicas adicionais. Cada cliente Mineflayer faz uma conexão de saída para o servidor Minecraft.

## Teste de autenticação no lobby

Em um ticket aberto, a administração pode usar `/botinvsee nick:kingteste123 modo:lobby`.
O registro/login começa no evento de conexão do protocolo, sem aguardar o personagem aparecer. O teste não entra no P4Free nem automatiza captcha/verificação; se o servidor pedir uma verificação, o comando informa isso e encerra a sessão. A confirmação de login exige uma resposta explícita do servidor.
Quando houver login confirmado no lobby, use `/botinvsee-stop` antes de iniciar a conta no modo P4Free (padrão). Consultas Invsee/clan ficam desabilitadas no modo lobby.

## Consultar e reiniciar

Após entrar por SSH:

```bash
systemctl status unknown-discord-bot --no-pager
journalctl -u unknown-discord-bot -n 60 --no-pager
systemctl restart unknown-discord-bot
```

Ao atualizar a aplicação, preserve `bot.env` e `/var/lib/unknown-discord-bot`; o código pode ser atualizado em `/opt/unknown-discord-bot`. Depois de mudar as definições dos slash commands, publique-os novamente com `scripts/deploy-commands.js`, carregando as variáveis de `bot.env` no processo. Não execute o bot com root: use o serviço configurado.

A chave pública Discord foi guardada no `.env`, mas não é necessária para esta integração por Gateway. O Application ID e o token são usados para autenticar e publicar comandos. Nenhuma credencial é exibida pelo verificador `scripts/check-discord.js`.
