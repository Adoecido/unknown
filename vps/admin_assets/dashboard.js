"use strict";
(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const e = value => String(value ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  const paths = {
    overview:'<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
    licenses:'<rect x="3" y="6" width="18" height="12" rx="3"/><path d="M7 10h4m-4 4h8m2-4h.01"/>',
    users:'<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3m1-16a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 4v2"/>',
    logs:'<path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9zM14 3v6h6M8 13h8m-8 4h5"/>',
    online:'<path d="M3 12h4l3-8 4 16 3-8h4"/>',
    downloads:'<path d="M12 3v12m-4-4 4 4 4-4M4 16v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3"/>',
    search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
    refresh:'<path d="M20 8a9 9 0 0 0-15-3L2 8m0-6v6h6m-4 8a9 9 0 0 0 15 3l3-3m0 6v-6h-6"/>',
    logout:'<path d="M9 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4m5-13 5 5-5 5m-6-5h13"/>',
    plus:'<path d="M12 5v14M5 12h14"/>', copy:'<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V4a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v11a1 1 0 0 0 1 1h4"/>',
    trash:'<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>', power:'<path d="M12 2v9m6.4-5.4a9 9 0 1 1-12.8 0"/>',
    pause:'<path d="M8 5v14M16 5v14"/>', play:'<path d="m8 4 13 8-13 8z"/>',
    edit:'<path d="m15 4 5 5M4 20l5-1L21 7a3.5 3.5 0 0 0-5-5L4 14z"/>',
    eye:'<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    close:'<path d="m6 6 12 12M6 18 18 6"/>', menu:'<path d="M4 6h16M4 12h16M4 18h16"/>',
    party:'<circle cx="12" cy="8" r="3"/><circle cx="4" cy="10" r="2"/><circle cx="20" cy="10" r="2"/><path d="M7 21v-3a5 5 0 0 1 10 0v3M1 21v-4a3 3 0 0 1 3-3m19 7v-4a3 3 0 0 0-3-3"/>',
    code:'<path d="m8 5-7 7 7 7m8-14 7 7-7 7M14 3l-4 18"/>',
    box:'<path d="m12 3 9 5v9l-9 5-9-5V8zM3 8l9 5 9-5m-9 5v9"/>',
    arrow:'<path d="M6 18 18 6M6 6h12v12"/>'
  };
  const icon = name => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.box}</svg>`;
  const pages = {
    overview:{title:"Visão geral",description:"Licenças, configs e atividade do client.",eyebrow:"WORKSPACE EM TEMPO REAL"},
    licenses:{title:"Licenças",description:"Criação e controle de licenças.",eyebrow:"GERENCIAR LICENÇAS"},
    users:{title:"Usuários",description:"Contas, HWID, IP e último login.",eyebrow:"LISTA DE USUÁRIOS"},
    logs:{title:"Logs",description:"Aberturas, uso do client, configs, parties e falhas.",eyebrow:"HISTÓRICO DE ATIVIDADES"},
    online:{title:"Online agora",description:"Nick atual, IP e tempo de uso.",eyebrow:"SESSÕES ATIVAS"},
    downloads:{title:"Downloads",description:"Loader, DLL e aplicativos disponíveis por perfil.",eyebrow:"ARQUIVOS DISPONÍVEIS"}
  };
  const messages = {unauthorized:"Chave ou código inválido. Tente novamente.",invalid_mfa_code:"Código inválido ou tentativas esgotadas. Confira o relógio do celular e tente novamente em alguns minutos.",rate_limited:"Muitas solicitações. Aguarde alguns segundos.",license_key_unavailable:"Esta licença antiga não tem uma chave recuperável.",license_expired:"Esta licença venceu. Crie uma nova com outra validade.",invalid_expiry:"Escolha uma validade futura.",invalid_role:"Escolha um perfil válido.",invalid_filename:"Use um nome de arquivo simples, sem caracteres especiais.",invalid_file_type:"Loader precisa ser .exe e DLL precisa ser .dll.",invalid_pe_file:"O arquivo não é um executável Windows válido.",invalid_upload_size:"Arquivo acima do limite: 32 MB para loader/DLL e 256 MB para aplicativos.",insufficient_storage:"A VPS está sem espaço suficiente para este arquivo.",upload_offset_mismatch:"O envio foi interrompido. Remova o envio pendente e tente novamente.",owner_required:"Somente lzr pode gerenciar administradores.",last_admin:"Mantenha pelo menos um administrador ativo.",cannot_revoke_self:"O acesso desta sessão não pode ser removido.",too_many_downloads:"Selecione no máximo 200 downloads por vez.",invalid_download_selection:"Selecione os downloads que deseja apagar.",internal:"O servidor não concluiu a solicitação. Tente novamente.",storage_unavailable:"O servidor não conseguiu gravar o arquivo.",not_found:"Este registro não está mais disponível.",session_not_online:"Esta sessão não está mais online."};
  const state = {page:"overview",admin:null,data:{},filters:{},audit:[],cursor:null,loading:false,revision:0,uploading:false,selectedDownloads:new Set(),onlineProbePending:0,onlineProbeSequence:0,loginToken:"",loginTicket:""};
  const filters = () => state.filters[state.page] || (state.filters[state.page] = {q:"",role:"",status:"",kind:"",event:"",page:1});
  const number = n => new Intl.NumberFormat("pt-BR").format(n || 0);
  const absolute = value => value ? new Date(value).toLocaleString("pt-BR") : "Nunca";
  function relative(value) {
    if (!value) return "Nunca";
    const seconds = Math.max(0, (Date.now() - new Date(value).getTime()) / 1000);
    if (seconds < 60) return "agora";
    if (seconds < 3600) return `há ${Math.floor(seconds / 60)} min`;
    if (seconds < 86400) return `há ${Math.floor(seconds / 3600)} h`;
    return `há ${Math.floor(seconds / 86400)} d`;
  }
  const date = value => `<span title="${e(absolute(value))}">${e(relative(value))}</span>`;
  const bytes = n => n < 1048576 ? `${Math.round(n / 1024)} KB` : `${(n / 1048576).toFixed(1)} MB`;
  const roleBadge = role => `<span class="badge">${e(role)}</span>`;
  const user = (name, sub = "") => `<div class="user-cell"><span class="avatar">${e((name || "?").slice(0,2).toUpperCase())}</span><div><strong>${e(name || "—")}</strong>${sub ? `<div class="sub">${e(sub)}</div>` : ""}</div></div>`;
  function minecraftUser(row) {
    const name=/^[a-zA-Z0-9_]{1,16}$/.test(row.player_name||"")?row.player_name:"";
    const uuid=/^[0-9a-f]{32}$/.test(row.player_uuid||"")?row.player_uuid:"";
    const skin=uuid?`/v1/admin/skins/${uuid}.png`:"";
    return `<div class="user-cell"><span class="minecraft-head" role="img" aria-label="Cabeça de ${e(name||row.username)}">${icon("users")}${skin?`<img class="skin-face" src="${skin}" alt="" loading="lazy"><img class="skin-hat" src="${skin}" alt="" loading="lazy">`:""}</span><div><strong>${e(row.username)}</strong><div class="sub">${e(name||"Nick indisponível")}</div></div></div>`;
  }
  function duration(value) {
    const elapsed=Math.max(0,Math.floor((Date.now()-new Date(value).getTime())/1000));
    if(!Number.isFinite(elapsed)) return "—";
    const days=Math.floor(elapsed/86400),hours=Math.floor(elapsed/3600)%24,minutes=Math.floor(elapsed/60)%60,seconds=elapsed%60;
    if(days) return `${days} d ${hours} h ${minutes} min`;
    if(hours) return `${hours} h ${minutes} min`;
    if(minutes) return `${minutes} min ${seconds} s`;
    return `${seconds} s`;
  }
  const btn = (label, action, id = "", cls = "", glyph = "") => `<button type="button" class="button ${cls}" data-action="${e(action)}" data-id="${e(id)}">${glyph ? icon(glyph) : ""}${e(label)}</button>`;
  const tool = (label, action, id, glyph, cls = "") => `<button type="button" class="icon-button ${cls}" title="${e(label)}" aria-label="${e(label)}" data-action="${e(action)}" data-id="${e(id)}">${icon(glyph)}</button>`;
  const empty = (title, description, glyph = state.page, action = "") => `<div class="empty"><span class="empty-icon">${icon(glyph)}</span><h3>${e(title)}</h3><p>${e(description)}</p>${action}</div>`;
  const table = (heads, rows, fallback) => rows.length ? `<div class="table-wrap"><table><thead><tr>${heads.map(h=>`<th>${e(h)}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table></div>` : fallback;
  function toast(message, error = false) {
    const node = document.createElement("div");node.className = `toast${error ? " error" : ""}`;node.textContent = message;$("#toasts").append(node);setTimeout(()=>node.remove(),6000);
  }
  async function api(path, method = "GET", body) {
    const controller = new AbortController(), timeout = setTimeout(()=>controller.abort(),60000);
    try {
      const response = await fetch(`/v1/admin/${path}`, {method,credentials:"same-origin",cache:"no-store",signal:controller.signal,
        headers:{"X-SonyVegas-Admin":"1",...(body !== undefined ? {"Content-Type":"application/json"} : {})},body:body !== undefined ? JSON.stringify(body) : undefined});
      const result = await response.json();
      if (!response.ok || result.ok === false) {const error = new Error(messages[result.error] || "Não foi possível concluir a solicitação.");error.status=response.status;throw error;}
      return result;
    } catch (error) {if (error.name === "AbortError") throw new Error("A conexão demorou demais. Tente novamente.");if (error instanceof TypeError) throw new Error("Sem conexão com o servidor. Tente novamente.");throw error;} finally {clearTimeout(timeout);}
  }
  function authenticated(admin) {
    state.loginToken="";state.loginTicket="";$("#access-key").value="";$("#login-otp").value="";$("#login-otp-wrap").hidden=true;
    state.admin=admin;$("#login").hidden=true;$("#mfa-setup").hidden=true;$("#app").hidden=false;$("#admin-name").textContent=admin.name;$("#account-button .avatar").textContent=admin.name.slice(0,2).toUpperCase();route();
  }
  function signOut() {state.admin=null;state.loginToken="";state.loginTicket="";state.selectedDownloads.clear();state.revision++;$("#app").hidden=true;$("#mfa-setup").hidden=true;$("#login").hidden=false;$("#login-otp-wrap").hidden=true;$("#access-key").required=true;if($("#modal").open) $("#modal").close();$("#access-key").value="";$("#login-otp").value="";}
  async function showMfaSetup() {
    const setup=await api("mfa/setup");
    $("#mfa-setup-qr").src=setup.qr;$("#mfa-setup-secret").textContent=setup.secret;
    $("#login").hidden=true;$("#app").hidden=true;$("#mfa-setup").hidden=false;
    $("#mfa-setup-code").value="";$("#mfa-setup-error").textContent="";$("#mfa-setup-code").focus();
  }
  async function acceptLogin(result) {
    if(result.mfaRequired) {$("#login-otp-wrap").hidden=false;$("#login-otp").required=true;$("#login-error").textContent="Digite o código do Google Authenticator.";$("#login-otp").focus();return;}
    if(result.mfaSetupRequired) {state.loginToken="";state.loginTicket="";$("#access-key").value="";await showMfaSetup();return;}
    authenticated(result.admin);
  }
  async function initialize() {
    const params = new URLSearchParams(location.hash.slice(1));const ticket=params.get("ticket");
    if(ticket) history.replaceState(null,"",location.pathname+"#overview");
    try {if(ticket) {state.loginTicket=ticket;$("#access-key").required=false;const result=await api("login","POST",{ticket});await acceptLogin(result);} else {const result=await api("admins");authenticated(result.currentAdmin);}}
    catch(error) {if(!ticket) {try{await showMfaSetup();return;}catch(_){}}if(ticket || error.status !== 401) $("#login-error").textContent=error.message;}
  }
  function renderNavigation() {
    $("#navigation").innerHTML=Object.entries(pages).map(([key,page])=>`<a class="nav-item ${key===state.page?"active":""}" href="#${key}" ${key===state.page?'aria-current="page"':''}>${icon(key)}<span>${e(page.title)}</span>${key==="online" && state.data.overview ? `<span class="count">${number(state.data.overview.counts.onlineUsers)}</span>` : ""}</a>`).join("");
  }
  function route() {
    if(!state.admin) return;
    const key=location.hash.slice(1);state.page=pages[key] ? key : "overview";state.revision++;
    const page=pages[state.page];$("#breadcrumb").textContent=page.title;$("#page-title").innerHTML=e(page.title)+'<span class="title-dot">.</span>';$("#page-description").textContent=page.description;$("#page-eyebrow").textContent=page.eyebrow;
    $("#sidebar").classList.remove("open");$("#page-actions").innerHTML=state.page==="downloads" ? btn("Adicionar download","new-download","","primary","plus") : state.page==="licenses" || state.page==="overview" ? btn("Criar licença","new-license","","primary","plus") : state.page==="online" ? btn("re-send","resend","","","refresh") : btn("Atualizar","refresh","","","refresh");
    renderNavigation();if(state.page==="online")requestOnline().catch(error=>toast(error.message,true));else refresh(false);
  }
  async function requestOnline() {
    const revision=state.revision;
    const probeSequence=++state.onlineProbeSequence;
    state.onlineProbePending=probeSequence;
    state.data.online=[];
    $("#content").innerHTML='<div class="loading"><div class="spinner"></div><span>Aguardando respostas dos clients</span></div>';
    try {
      await api("online/probe","POST",{});
      await new Promise(resolve=>setTimeout(resolve,3500));
      if(revision!==state.revision||probeSequence!==state.onlineProbePending)return;
      await refresh(false);
      await new Promise(resolve=>setTimeout(resolve,5000));
      if(revision!==state.revision||probeSequence!==state.onlineProbePending)return;
      await refresh(false);
    } catch(error) {
      if(revision===state.revision&&probeSequence===state.onlineProbePending)$("#content").innerHTML=empty("Não foi possível consultar","Clique em re-send para tentar novamente.","online");
      throw error;
    } finally {if(state.onlineProbePending===probeSequence)state.onlineProbePending=0;}
  }
  async function refresh(quiet = true) {
    if(!state.admin || state.loading) return;
    state.loading=true;const revision=state.revision,page=state.page;$("#page-error").hidden=true;
    if(!quiet) $("#content").innerHTML='<div class="loading"><div class="spinner"></div><span>Carregando workspace</span></div>';
    try {
      if(page==="overview") {const [overview,audit]=await Promise.all([api("overview"),api("audit?limit=6")]);state.data.overview=overview;state.data.recent=audit.events;}
      else if(page==="logs") {const f=filters(),result=await api("audit?"+new URLSearchParams({q:f.q,event:f.event}));state.audit=result.events;state.cursor=result.nextCursor;}
      else {const endpoint={licenses:"licenses",users:"users",online:"online",downloads:"downloads"}[page];state.data[page]=(await api(endpoint))[{online:"clients",downloads:"downloads",users:"users",licenses:"licenses"}[page]];if(page==="downloads"){const ids=new Set(state.data.downloads.map(row=>row.id));for(const id of state.selectedDownloads)if(!ids.has(id))state.selectedDownloads.delete(id);}}
      if(revision!==state.revision) return;
      $("#connection-status").textContent="Servidor conectado";$("#connection-dot").classList.remove("stale");$("#sync-time").textContent="Atualizado agora";$("#footer-time").textContent="Última atualização · "+new Date().toLocaleTimeString("pt-BR");
      if(!quiet || (!$("#modal").open && !$("#content").contains(document.activeElement))) render();renderNavigation();
    } catch(error) {
      if(error.status===401) {signOut();$("#login-error").textContent=error.message;return;}
      if(revision!==state.revision) return;$("#connection-status").textContent="Conexão interrompida";$("#connection-dot").classList.add("stale");$("#sync-time").textContent="Dados podem estar desatualizados";$("#page-error").hidden=false;$("#page-error").textContent=error.message;
      if(!quiet) $("#content").innerHTML=empty("Não foi possível carregar","Confira a conexão e atualize a página.","refresh",btn("Tentar novamente","refresh"));
    } finally {state.loading=false;if(revision!==state.revision && state.admin && !state.onlineProbePending) refresh(false);}
  }
  function select(name, label, options) {const value=filters()[name];return `<select class="filter" data-filter="${name}" aria-label="${e(label)}"><option value="">${e(label)}</option>${options.map(([key,text])=>`<option value="${e(key)}" ${value===key?"selected":""}>${e(text)}</option>`).join("")}</select>`;}
  function toolbar(extra = "", count = "") {return `<div class="toolbar"><div class="toolbar-left"><div class="search">${icon("search")}<input type="search" id="page-search" placeholder="Buscar ${state.page==="users"?"usuário, HWID ou IP":"nesta página"}" aria-label="Buscar nesta página" value="${e(filters().q)}" maxlength="128"></div></div><div class="toolbar-right">${extra}${count?`<span class="muted">${e(count)}</span>`:""}</div></div>`;}
  function matches(row) {return JSON.stringify(row).toLocaleLowerCase().includes(filters().q.toLocaleLowerCase());}
  function pagination(total) {const f=filters(),max=Math.max(1,Math.ceil(total/20));f.page=Math.min(f.page,max);return `<div class="table-foot"><span>${number(total)} registros · página ${f.page} de ${max}</span><div class="row-actions">${f.page>1?btn("Anterior","previous","","small"):""}${f.page<max?btn("Próxima","next","","small"):""}</div></div>`;}
  function pageRows(rows) {const f=filters();f.page=Math.min(f.page,Math.max(1,Math.ceil(rows.length/20)));return rows.slice((f.page-1)*20,f.page*20);}
  const eventLabels={"loader.opened":"Abriu o loader","payload.downloaded":"Baixou a DLL","client.injected":"Iniciou o client","client.stopped":"Encerrou o client","client.error":"Erro no client","client.crashed":"Falha no client","loader.failed":"Falha no loader","user.activated":"Ativou uma licença","config.created":"Criou uma config","config.saved":"Salvou uma config","config.loaded":"Carregou uma config","config.deleted":"Apagou uma config","community.published":"Publicou uma config","community.downloaded":"Puxou uma config","community.deleted":"Apagou uma config pública","party.created":"Criou uma party","party.joined":"Entrou em uma party","party.left":"Saiu da party","party.synced":"Sincronizou a party","admin.login":"Entrou no painel","admin.login.failed":"Falha de acesso ao painel","admin.license.created":"Criou uma licença","admin.license.disabled":"Desativou uma licença","admin.license.enabled":"Reativou uma licença","admin.license.deleted":"Apagou uma licença","admin.license.copied":"Copiou uma licença","admin.license.revoked":"Revogou uma licença","admin.user.updated":"Atualizou um usuário","admin.sessions.revoked":"Revogou as sessões","admin.user.access_revoked":"Apagou um usuário","admin.client.close_requested":"Solicitou fechar o jogo","admin.download.published":"Publicou um download","admin.download.created":"Preparou um download","admin.download.updated":"Atualizou um download","admin.download.deleted":"Apagou um download","download.downloaded":"Baixou um aplicativo","admin.account.created":"Criou um administrador","admin.account.revoked":"Removeu um administrador","admin.payload.updated":"Atualizou a DLL","admin.cleanup":"Limpou registros antigos"};
  function eventText(row) {return eventLabels[row.event_type] || row.event_type;}
  function eventRow(row, compact = false) {const error=["loader.failed","client.error","client.crashed"].includes(row.event_type);return `<tr><td>${user(row.actor_username,row.actor_role)}</td><td><span class="badge ${error?"error":""}">${e(eventText(row))}</span></td>${compact?"":`<td class="mono">${e(row.source_ip || "—")}</td>`}<td>${date(row.created_at)}</td><td><div class="row-actions">${tool("Ver detalhes","event",row.id,"arrow")}</div></td></tr>`;}
  function overview() {
    const data=state.data.overview,c=data.counts,max=Math.max(4,...data.activity.map(d=>Math.max(d.opened,d.used))),axis=Math.ceil(max/4)*4;
    const metrics=[["Licenças",c.licenses,`${number(c.availableLicenses)} disponíveis`,"licenses"],["Configs salvas",c.configs,`${number(c.communityConfigs)} na comunidade`,"logs"],["Parties abertas",c.openParties,"Grupos criados pelos usuários","party"],["Online agora",c.onlineUsers,"Presença confirmada pelo client","online"]];
    return `<div class="metrics">${metrics.map(([label,count,note,glyph],i)=>`<article class="card metric ${i===3?"dark":""}"><div class="metric-label"><span>${e(label)}</span>${icon(glyph)}</div><div class="metric-number">${number(count)}</div><div class="metric-note">${e(note)}</div></article>`).join("")}</div>
      <div class="overview-grid"><section class="card chart-card"><div class="card-header"><div><h2 class="card-title">Atividade do client</h2><p class="card-subtitle">Aberturas e inicializações nos últimos 7 dias · UTC</p></div><span class="badge">7 dias</span></div><div class="chart-legend"><span><i></i>Client iniciado</span><span><i></i>Loader aberto</span></div><div class="chart"><div class="chart-axis">${[axis,axis*.75,axis*.5,axis*.25,0].map(n=>`<span>${number(n)}</span>`).join("")}</div><div class="plot">${data.activity.map(d=>`<div class="bar-group" title="${e(d.day)}: ${d.used} inicializações, ${d.opened} aberturas"><div class="bar" data-height="${d.used/axis*100}"></div><div class="bar secondary" data-height="${d.opened/axis*100}"></div><span class="bar-label">${new Date(d.day+'T12:00:00Z').toLocaleDateString('pt-BR',{weekday:'short'})}</span></div>`).join("")}</div></div>${data.activity.every(d=>!d.used&&!d.opened)?'<p class="chart-empty">As primeiras atividades aparecerão aqui quando os usuários iniciarem unknown.</p>':""}</section>
      <section class="card workspace-card"><img class="brand-shape" src="/admin/assets/unknown-logo.png" alt=""><p class="eyebrow">UNKNOWN WORKSPACE</p><h2>Visão geral.</h2><p>Licenças, usuários e versões do client.</p><div class="workspace-lines"><div class="workspace-line"><span>Usuários cadastrados</span><strong>${number(c.users)}</strong></div><div class="workspace-line"><span>Client iniciado hoje</span><strong>${number(c.injectionsToday)}</strong></div><div class="workspace-line"><span>Falhas registradas hoje</span><strong>${number(c.errorsToday)}</strong></div></div></section></div>
      <div class="section-label"><h2>Atividade recente</h2><a href="#logs">Ver todos os logs ↗</a></div><section class="card">${table(["USUÁRIO","ATIVIDADE","QUANDO",""],(state.data.recent||[]).map(r=>eventRow(r,true)),empty("Nenhuma atividade registrada","As ações dos usuários e da administração aparecem assim que acontecerem.","logs"))}</section>`;
  }
  function licenses() {
    const f=filters();const rows=(state.data.licenses||[]).filter(r=>matches(r)&&(!f.role||r.role===f.role)&&(!f.status||licenseState(r)===f.status));
    return `<section class="card">${toolbar(select("role","Todos os perfis",[["user","user"],["tester","tester"],["developer","developer"]])+select("status","Todos os status",[["unused","Disponível"],["used","Em uso"],["disabled","Desativada"],["expired","Expirada"]]))}${table(["LICENÇA","PERFIL","STATUS","USADA POR","VALIDADE",""],pageRows(rows).map(r=>`<tr><td><div class="copy-cell"><strong class="mono">•••• ${e(r.key_hint||r.id.slice(-6))}</strong>${r.can_copy?tool("Copiar licença","copy-license",r.id,"copy"):""}</div>${r.note?`<div class="muted" title="${e(r.note)}">${e(r.note.slice(0,42))}</div>`:""}</td><td>${roleBadge(r.role)}</td><td>${licenseBadge(r)}</td><td>${r.username?user(r.username):'<span class="muted">Ainda não utilizada</span>'}</td><td>${r.expires_at?`<span title="${e(absolute(r.expires_at))}">${e(new Date(r.expires_at).toLocaleDateString("pt-BR"))}</span>`:'<span class="muted">Sem prazo</span>'}</td><td><div class="row-actions">${tool("Detalhes da licença","license",r.id,"eye")}${licenseState(r)==="disabled"?tool("Reativar licença","enable-license",r.id,"play"):licenseState(r)!=="expired"?tool("Desativar licença","disable-license",r.id,"pause"):""}${tool("Apagar licença","delete-license",r.id,"trash","destructive")}</div></td></tr>`),empty("Nenhuma licença encontrada","Crie uma licença para liberar o acesso ao client ou ajuste os filtros.","licenses",btn("Criar licença","new-license","","primary","plus")))}${pagination(rows.length)}</section><p class="note-strip">Desativar bloqueia o acesso e permite reativar a mesma licença. Apagar remove a chave e preserva o histórico do usuário.</p>`;
  }
  function licenseState(row) {if(["disabled","revoked"].includes(row.status)) return "disabled";if(row.expires_at && new Date(row.expires_at)<=new Date()) return "expired";return row.status;}
  function licenseBadge(row) {const status=licenseState(row);return `<span class="badge ${status==="used"?"active":status}"><i></i>${e({unused:"Disponível",used:"Em uso",disabled:"Desativada",expired:"Expirada"}[status]||status)}</span>`;}
  function users() {
    const f=filters(),rows=(state.data.users||[]).filter(r=>matches(r)&&(!f.role||r.role===f.role)&&(!f.status||r.status===f.status));
    return `<section class="card">${toolbar(select("role","Todos os perfis",[["user","user"],["tester","tester"],["developer","developer"]])+select("status","Todos os status",[["active","Ativo"],["disabled","Desativado"]]))}${table(["USUÁRIO","HWID","ÚLTIMO IP","ÚLTIMO LOGIN","PERFIL",""],pageRows(rows).map(r=>`<tr><td>${user(r.username,r.online?"Client online":"Client offline")}</td><td><div class="copy-cell"><span class="hardware" title="${e(r.id)}">${e(r.id.replace(/^hw_/,"").toUpperCase().match(/.{1,4}/g)?.join(" ")||r.id)}</span>${tool("Copiar HWID","copy-hwid",r.id,"copy")}</div></td><td class="mono">${e(r.last_ip||"—")}</td><td>${date(r.last_login_at)}</td><td>${roleBadge(r.role)} ${r.status!=="active"?'<span class="badge disabled">Desativado</span>':""}</td><td><div class="row-actions">${tool("Gerenciar usuário","user",r.id,"edit")}${tool("Apagar usuário","delete-user",r.id,"trash","destructive")}</div></td></tr>`),empty("Nenhum usuário encontrado","Os usuários aparecem depois de ativar uma licença no loader.","users"))}${pagination(rows.length)}</section>`;
  }
  function logs() {return `<section class="card">${toolbar(select("event","Todas as atividades",[["loader","Loader"],["client","Client"],["config","Configs"],["community","Comunidade"],["party","Parties"],["errors","Erros e crashes"],["admin","Administração"]]))}${table(["USUÁRIO","ATIVIDADE","IP","QUANDO",""],state.audit.map(r=>eventRow(r)),empty("Nenhum evento encontrado","As novas ações serão registradas aqui. Ajuste os filtros para consultar o histórico.","logs"))}<div class="table-foot"><span>${number(state.audit.length)} registros carregados · retenção de 180 dias</span>${state.cursor?btn("Carregar mais","more-logs","","small"):""}</div></section>`;}
  function online() {
    const all=state.data.online||[],rows=all.filter(matches),total=new Set(all.map(r=>r.device_id)).size;
    return `<section class="online-summary"><div><p class="eyebrow"><i class="live-dot"></i>CLIENT ATIVO</p><h2>${number(total)} ${total===1?"usuário respondeu":"usuários responderam"}</h2></div><p>Resultado da última consulta. Clique em Re-send para consultar novamente.</p></section><section class="card">${toolbar()}${table(["USUÁRIO / MINECRAFT","IP","TEMPO DE USO",""],pageRows(rows).map(r=>`<tr><td>${minecraftUser(r)}</td><td class="mono">${e(r.source_ip||"—")}</td><td><span class="session-duration" data-started-at="${e(r.started_at)}" title="Início: ${e(absolute(r.started_at))}">${e(duration(r.started_at))}</span></td><td><div class="row-actions">${tool("Ver sessão","session",r.instance_id,"eye")}${state.admin?.access==="owner"?(r.close_requested?'<span class="badge">Fechamento solicitado</span>':r.supports_remote_close?tool("Forçar fechamento do jogo","close-session",r.instance_id,"power","destructive"):'<span class="badge">Atualize a DLL</span>'):""}</div></td></tr>`),empty("Ninguém respondeu à consulta","Clique em Re-send para consultar novamente.","online"))}${pagination(rows.length)}</section>`;
  }
  const kindNames={loader:"Loader",dll:"DLL",application:"Aplicativo"};
  function filteredDownloads() {const f=filters();return (state.data.downloads||[]).filter(r=>matches(r)&&(!f.role||r.role===f.role)&&(!f.kind||r.kind===f.kind));}
  function downloadCard(r) {
    const selected=state.selectedDownloads.has(r.id);
    return `<article class="card download-card${selected?" selected":""}"><header><div class="download-card-heading"><label class="download-choice"><input type="checkbox" data-download-select="${e(r.id)}" aria-label="Selecionar ${e(r.name)}" ${selected?"checked":""}></label><span class="download-symbol">${icon(r.kind==="dll"?"code":r.kind==="loader"?"downloads":"box")}</span></div>${roleBadge(r.role)}</header><div><h2>${e(r.name)}</h2><p>${e(kindNames[r.kind])}${r.version?' · '+e(r.version):""}</p></div><p>${e(r.notes||"Arquivo disponibilizado pela administração.")}</p><span class="file-name mono">${e(r.filename)}</span><div class="download-meta"><span>${r.uploaded?bytes(r.size):"Envio pendente"}</span>${date(r.updated_at)}</div><div class="download-bottom"><span class="badge ${r.enabled&&r.uploaded?"active":"disabled"}">${r.uploaded?(r.enabled?"Publicado":"Desativado"):"Pendente"}</span><div class="row-actions">${r.uploaded?`<a class="icon-button" title="Baixar ${e(r.name)}" aria-label="Baixar ${e(r.name)}" href="/v1/admin/downloads/${e(r.id)}/file">${icon("downloads")}</a>`:""}${tool("Editar download","download",r.id,"edit")}${r.uploaded?tool(r.enabled?"Desativar download":"Publicar download","toggle-download",r.id,r.enabled?"pause":"play"):""}${tool("Apagar download","delete-download",r.id,"trash","destructive")}</div></div></article>`;
  }
  function downloadGroup(title,key,rows) {
    const allSelected=rows.length>0&&rows.every(r=>state.selectedDownloads.has(r.id));
    return `<section class="download-section"><div class="download-section-heading"><div><h2>${e(title)} <span>${number(rows.length)}</span></h2><p>${key==="active"?"Arquivos disponíveis para os perfis autorizados.":"Arquivos desativados ou com envio pendente."}</p></div>${rows.length?btn(allSelected?"Limpar grupo":"Selecionar grupo","select-download-group",key,"small"):""}</div>${rows.length?`<div class="downloads-grid">${rows.map(downloadCard).join("")}</div>`:`<div class="card download-section-empty">Nenhum arquivo ${key==="active"?"ativo":"inativo"} com os filtros atuais.</div>`}</section>`;
  }
  function downloads() {
    const rows=filteredDownloads();
    const active=rows.filter(r=>r.enabled&&r.uploaded),inactive=rows.filter(r=>!r.enabled||!r.uploaded);
    const selected=state.selectedDownloads.size;
    return `<section class="downloads-toolbar">${toolbar(select("role","Todos os perfis",[["user","user"],["tester","tester"],["developer","developer"]])+select("kind","Todos os arquivos",[["loader","Loader"],["dll","DLL"],["application","Aplicativos"]]))}</section>${selected?`<div class="download-selection-bar"><strong>${number(selected)} ${selected===1?"arquivo selecionado":"arquivos selecionados"}</strong><div>${btn("Limpar seleção","clear-download-selection","","small")}${btn(`Apagar selecionados (${selected})`,"delete-selected-downloads","","danger small","trash")}</div></div>`:""}${downloadGroup("Ativos","active",active)}${downloadGroup("Inativos","inactive",inactive)}<p class="note-strip">user acessa arquivos user. tester acessa user + tester. developer acessa todos. O loader recebe automaticamente a DLL publicada para o perfil da licença; sem uma DLL específica, usa a versão user.</p>`;
  }
  function render() {$("#content").innerHTML=({overview,licenses,users,logs,online,downloads}[state.page])();document.querySelectorAll("[data-height]").forEach(node=>node.style.height=Math.max(0,Math.min(100,Number(node.dataset.height)))+"%");}
  function modal(title, subtitle, body, footer = "") {$("#modal-content").innerHTML=`<div class="modal-header"><div><h2>${e(title)}</h2><p>${e(subtitle)}</p></div>${tool("Fechar","close","","close")}</div><div class="modal-body">${body}<p id="modal-error" class="form-error" role="alert"></p></div>${footer?`<div class="modal-footer">${footer}</div>`:""}`;if(!$("#modal").open) $("#modal").showModal();}
  function field(label,name,value="",type="text",extra="") {return `<div class="field"><label for="field-${name}">${e(label)}</label><input id="field-${name}" name="${name}" type="${type}" value="${e(value)}" ${extra}></div>`;}
  const roleField = role => `<div class="field"><label for="field-role">Perfil</label><select id="field-role" name="role">${["user","tester","developer"].map(r=>`<option ${r===role?"selected":""}>${r}</option>`).join("")}</select></div>`;
  function bindForm(operation) {const form=$("#modal-form");form.addEventListener("submit",async event=>{event.preventDefault();const button=$("#modal-submit");button.disabled=true;$("#modal-error").textContent="";try{await operation(new FormData(form));}catch(error){$("#modal-error").textContent=error.message;}finally{if(button.isConnected)button.disabled=false;}});}
  const formFooter = label => `${btn("Cancelar","close")}<button class="button primary" id="modal-submit" type="submit" form="modal-form">${e(label)}</button>`;
  async function copy(text) {try {await navigator.clipboard.writeText(text);toast("Copiado para a área de transferência.");}catch(_){modal("Copiar","Selecione o valor abaixo para copiar manualmente.",`<div class="license-output">${e(text)}</div>`,btn("Fechar","close"));}}
  function confirm(title,message,operation) {modal(title,message,'<p class="notice">Esta ação será registrada no histórico.</p>',btn("Cancelar","close")+btn("Confirmar","confirm","","danger"));$("[data-action=confirm]").addEventListener("click",async event=>{const button=event.currentTarget;button.disabled=true;try{const result=await operation();$("#modal").close();toast(typeof result==="string"?result:"Alteração concluída.");refresh(false);}catch(error){$("#modal-error").textContent=error.message;button.disabled=false;}});}
  function licenseForm() {
    modal("Criar licença","Um novo acesso a unknown.",`<form id="modal-form">${roleField("user")}${field("Validade (opcional)","expiry","","date")}${field("Nota (opcional)","note","","text",'maxlength="256" placeholder="Ex.: renovação, tester ou nome do usuário"')}<p class="notice">A chave será gerada automaticamente. A chave fica disponível para cópia na lista de licenças.</p></form>`,formFooter("Criar licença"));
    bindForm(async form=>{const expiry=form.get("expiry"),result=await api("licenses","POST",{role:form.get("role"),note:form.get("note"),expiresAt:expiry?new Date(expiry+"T23:59:59").toISOString():null});refresh(true);modal("Licença criada","Compartilhe a chave com o usuário que vai ativá-la.",`<div class="badge">${e(result.role)}</div><div class="license-output">${e(result.licenseKey)}</div><p class="notice">Uma licença vincula um usuário ao dispositivo usado na ativação.</p>`,btn("Concluir","close")+btn("Copiar licença","copy-created","","primary","copy"));$("[data-action=copy-created]").addEventListener("click",()=>copy(result.licenseKey));});
  }
  function downloadForm(row) {
    const editing=!!row;
    modal(editing?"Editar download":"Adicionar download",editing?"Organize o acesso a esta versão.":"Envie um arquivo e escolha quem pode acessá-lo.",`<form id="modal-form">${field("Nome","name",row?.name||"","text",'required maxlength="80" placeholder="Ex.: unknown loader"')}<div class="fields-row">${roleField(row?.role||"user")}${field("Versão (opcional)","version",row?.version||"","text",'maxlength="40" placeholder="Ex.: 1.0.0"')}</div>${editing?"":'<div class="field"><label for="field-kind">Tipo de arquivo</label><select id="field-kind" name="kind"><option value="loader">Loader (.exe)</option><option value="dll">DLL (.dll)</option><option value="application">Aplicativo / outro arquivo</option></select></div><div class="field"><label for="field-file">Arquivo</label><input type="file" id="field-file" name="file" required><small>Loader/DLL: até 32 MB. Aplicativos: até 256 MB.</small></div>'}<div class="field"><label for="field-notes">Descrição (opcional)</label><textarea id="field-notes" name="notes" maxlength="400">${e(row?.notes||"")}</textarea></div>${editing&&row.uploaded?'<div class="field"><label for="field-enabled">Disponibilidade</label><select id="field-enabled" name="enabled"><option value="true" '+(row.enabled?'selected':'')+'>Publicado</option><option value="false" '+(!row.enabled?'selected':'')+'>Desativado</option></select></div>':""}<div id="upload-progress" class="upload-progress" hidden><progress max="100" value="0"></progress><span>0%</span></div></form>`,formFooter(editing?"Salvar alterações":"Enviar e publicar"));
    bindForm(async form=>{
      const metadata={name:form.get("name"),role:form.get("role"),version:form.get("version"),notes:form.get("notes")};
      if(editing) {if(row.uploaded)metadata.enabled=form.get("enabled")==="true";await api(`downloads/${row.id}`,"POST",metadata);}
      else {
        const file=form.get("file"),kind=form.get("kind"),max=(kind==="application"?256:32)*1048576;
        if(!file?.size || file.size>max)throw new Error(messages.invalid_upload_size);
        const created=await api("downloads","POST",{...metadata,kind,filename:file.name});
        state.uploading=true;$("#upload-progress").hidden=false;$("#modal").querySelectorAll('[data-action="close"]').forEach(b=>b.disabled=true);
        try {
          for(let offset=0;offset<file.size;offset+=5*1048576) {
            const end=Math.min(file.size,offset+5*1048576),response=await fetch(`/v1/admin/downloads/${created.id}/file`,{method:"PUT",credentials:"same-origin",headers:{"X-SonyVegas-Admin":"1","Content-Type":"application/octet-stream","Content-Range":`bytes ${offset}-${end-1}/${file.size}`},body:file.slice(offset,end),signal:AbortSignal.timeout(90000)});
            const result=await response.json();if(!response.ok||!result.ok)throw new Error(messages[result.error]||"O envio não foi concluído.");
            const percent=Math.round(end/file.size*100);$("#upload-progress progress").value=percent;$("#upload-progress span").textContent=percent+"%";
          }
        } catch(error) {try{await api(`downloads/${created.id}/delete`,"POST",{});}catch(_){}throw error;}
        finally {state.uploading=false;$("#modal").querySelectorAll('[data-action="close"]').forEach(b=>b.disabled=false);}
      }
      $("#modal").close();toast(editing?"Download atualizado.":"Arquivo publicado.");refresh(false);
    });
  }
  function userForm(row) {
    modal("Gerenciar usuário",row.username,`<form id="modal-form">${field("Nome do usuário","username",row.username,"text",'required maxlength="32"')}${roleField(row.role)}<div class="field"><label for="field-status">Acesso</label><select name="status" id="field-status"><option value="active" ${row.status==="active"?"selected":""}>Ativo</option><option value="disabled" ${row.status!=="active"?"selected":""}>Desativado</option></select></div><div class="notice">${number(row.config_count)} configs salvas<br>Último login: ${e(absolute(row.last_login_at))}<br>IP: ${e(row.last_ip||"—")}<br><span class="mono">${e(row.id)}</span></div></form>`,formFooter("Salvar alterações"));
    bindForm(async form=>{await api(`users/${row.id}`,"POST",Object.fromEntries(form));$("#modal").close();toast("Usuário atualizado.");refresh(false);});
  }
  function eventDetails(row) {
    const d=row.details||{};const items=[["Usuário",row.actor_username],["Atividade",eventText(row)],["Data",absolute(row.created_at)],["IP",row.source_ip||"—"],["HWID",row.actor_device_id||"—"]];
    modal("Detalhes do evento",row.event_type,`<dl class="detail-grid">${items.map(([key,value])=>`<dt>${e(key)}</dt><dd>${e(value)}</dd>`).join("")}${Object.entries(d).filter(([key])=>key!=="stack").map(([key,value])=>`<dt>${e(({phase:"Etapa",code:"Código",message:"Motivo",client:"Client",javaVersion:"Java",version:"Versão",name:"Nome",slot:"Slot",clientSession:"Sessão"})[key]||key)}</dt><dd>${e(typeof value==="object"?JSON.stringify(value):value)}</dd>`).join("")}</dl>${d.stack?`<pre class="stack">${e(d.stack)}</pre>`:""}`,btn("Fechar","close"));
  }
  async function accounts() {
    const result=await api("admins"),owner=state.admin.access==="owner";
    modal("Acessos ao painel","Chaves individuais de administração.",result.admins.map(a=>`<div class="access-row"><span class="avatar">${e(a.name.slice(0,2).toUpperCase())}</span><div><strong>${e(a.name)}</strong><small>${a.access_level==="owner"?"Acesso principal":"Administrador"} · ${a.status==="active"?"Ativo":"Desativado"}</small></div><div class="row-actions">${owner&&a.status==="active"&&a.id!==state.admin.id?tool("Remover acesso","revoke-admin",a.id,"trash"):""}</div></div>`).join("")+(owner?`<form id="modal-form"><div class="field">${field("Novo administrador","name","","text",'required maxlength="32" placeholder="Nome da pessoa"')}</div></form>`:""),owner?`<button id="modal-submit" form="modal-form" type="submit" class="button primary">Criar acesso</button>`:btn("Fechar","close"));
    if(owner)bindForm(async form=>{const created=await api("admins","POST",{name:form.get("name")});modal("Acesso criado","Copie esta chave agora. Ela não será exibida novamente.",`<div class="license-output">${e(created.adminToken)}</div>`,btn("Concluir","close")+btn("Copiar chave","copy-admin","","primary","copy"));$("[data-action=copy-admin]").addEventListener("click",()=>copy(created.adminToken));});
  }
  async function action(name,id) {
    const license=()=>state.data.licenses?.find(r=>r.id===id),download=()=>state.data.downloads?.find(r=>r.id===id);
    switch(name) {
      case "close":if(!state.uploading)$("#modal").close();break;
      case "refresh":await refresh(false);break;
      case "resend":await requestOnline();break;
      case "new-license":licenseForm();break;
      case "new-download":downloadForm();break;
      case "copy-license":await copy((await api(`licenses/${id}/copy`,"POST",{})).licenseKey);break;
      case "copy-hwid":await copy(id);break;
      case "enable-license":case "disable-license":await api(`licenses/${id}/${name.startsWith("enable")?"enable":"disable"}`,"POST",{});toast(name.startsWith("enable")?"Licença reativada.":"Licença desativada.");refresh(false);break;
      case "delete-license":confirm("Apagar licença?","A chave será removida e o usuário perderá o acesso. O histórico e as configs serão preservados.",()=>api(`licenses/${id}/delete`,"POST",{}));break;
      case "license": {const r=license();if(!r)return;modal("Detalhes da licença",r.note||"unknown",`<dl class="detail-grid"><dt>ID</dt><dd class="mono">${e(r.id)}</dd><dt>Perfil</dt><dd>${roleBadge(r.role)}</dd><dt>Status</dt><dd>${licenseBadge(r)}</dd><dt>Usuário</dt><dd>${e(r.username||"Ainda não utilizada")}</dd><dt>HWID</dt><dd class="mono">${e(r.bound_device_id||"—")}</dd><dt>Criada</dt><dd>${e(absolute(r.created_at))}</dd><dt>Ativada</dt><dd>${r.redeemed_at?e(absolute(r.redeemed_at)):"—"}</dd><dt>Validade</dt><dd>${r.expires_at?e(absolute(r.expires_at)):"Sem prazo"}</dd></dl>`,btn("Fechar","close")+(r.can_copy?btn("Copiar licença","copy-license",r.id,"primary","copy"):""));break;}
      case "user": {const r=state.data.users?.find(r=>r.id===id);if(r)userForm(r);break;}
      case "delete-user": {const r=state.data.users?.find(r=>r.id===id);if(r)confirm("Apagar usuário?",`O acesso de ${r.username} será removido. As configs do usuário serão apagadas; os logs serão preservados.`,()=>api(`users/${id}/access/revoke`,"POST",{}));break;}
      case "event": {const r=[...state.audit,...(state.data.recent||[])].find(r=>String(r.id)===id);if(r)eventDetails(r);break;}
      case "close-session": {const r=state.data.online?.find(r=>r.instance_id===id);if(r)confirm("Forçar fechamento do jogo?",`O Minecraft de ${r.username} será encerrado quando esta sessão confirmar presença. O progresso não salvo pode ser perdido.`,()=>api(`online/${id}/close`,"POST",{}));break;}
      case "session": {const r=state.data.online?.find(r=>r.instance_id===id);if(r)modal("Sessão ativa",r.username,`<dl class="detail-grid"><dt>Perfil</dt><dd>${e(r.role)}</dd><dt>Nick Minecraft</dt><dd>${e(r.player_name||"—")}</dd><dt>Client</dt><dd>${e(r.client)}</dd><dt>Java</dt><dd>${e(r.java_version)}</dd><dt>Versão</dt><dd>${e(r.version)}</dd><dt>IP</dt><dd>${e(r.source_ip)}</dd><dt>Início</dt><dd>${e(absolute(r.started_at))}</dd><dt>Último sinal</dt><dd>${e(absolute(r.last_seen_at))}</dd><dt>HWID</dt><dd class="mono">${e(r.device_id)}</dd></dl>`,btn("Fechar","close"));break;}
      case "more-logs": {if(!state.cursor)return;const f=filters(),result=await api("audit?"+new URLSearchParams({before:state.cursor,q:f.q,event:f.event}));state.audit.push(...result.events);state.cursor=result.nextCursor;render();break;}
      case "previous":filters().page=Math.max(1,filters().page-1);render();break;
      case "next":filters().page++;render();break;
      case "download": {const r=download();if(r)downloadForm(r);break;}
      case "select-download-group": {const rows=filteredDownloads().filter(r=>Boolean(r.enabled&&r.uploaded)===(id==="active"));const allSelected=rows.length>0&&rows.every(r=>state.selectedDownloads.has(r.id));if(!allSelected&&new Set([...state.selectedDownloads,...rows.map(r=>r.id)]).size>200)throw new Error(messages.too_many_downloads);for(const row of rows){if(allSelected)state.selectedDownloads.delete(row.id);else state.selectedDownloads.add(row.id);}render();break;}
      case "clear-download-selection":state.selectedDownloads.clear();render();break;
      case "delete-selected-downloads": {const ids=[...state.selectedDownloads];if(!ids.length)throw new Error(messages.invalid_download_selection);confirm(`Apagar ${ids.length} ${ids.length===1?"download":"downloads"}?`,"Os arquivos selecionados serão removidos da VPS e deixarão de estar disponíveis.",async()=>{const result=await api("downloads/bulk-delete","POST",{ids});state.selectedDownloads.clear();return result.filesNotRemoved?.length?`${result.deleted.length} registros apagados. ${result.filesNotRemoved.length} arquivos exigem limpeza manual na VPS.`:`${result.deleted.length} ${result.deleted.length===1?"download apagado":"downloads apagados"}.`;});break;}
      case "toggle-download": {const r=download();if(!r)return;await api(`downloads/${id}`,"POST",{enabled:!r.enabled});toast(r.enabled?"Download desativado.":"Download publicado.");refresh(false);break;}
      case "delete-download":confirm("Apagar download?","O arquivo será removido da VPS e deixará de estar disponível.",()=>api(`downloads/${id}/delete`,"POST",{}));break;
      case "revoke-admin":confirm("Remover acesso?","Esta pessoa não poderá mais entrar no painel.",()=>api(`admins/${id}/revoke`,"POST",{}));break;
    }
  }
  let searchTimer;
  document.addEventListener("click",async event=>{const button=event.target.closest("[data-action]");if(!button || button.disabled)return;const name=button.dataset.action;if(["confirm","copy-created","copy-admin","copy-recovery"].includes(name))return;try{await action(name,button.dataset.id);}catch(error){toast(error.message,true);}});
  document.addEventListener("input",event=>{if(event.target.id!=="page-search")return;clearTimeout(searchTimer);filters().q=event.target.value;filters().page=1;searchTimer=setTimeout(()=>{if(state.page==="logs")refresh(true).then(()=>{if(document.activeElement?.id==="page-search") {const position=document.activeElement.selectionStart;render();$("#page-search").focus();$("#page-search").setSelectionRange(position,position);}});else {const position=event.target.selectionStart;render();$("#page-search").focus();$("#page-search").setSelectionRange(position,position);}},300);});
  document.addEventListener("change",event=>{if(event.target.dataset.downloadSelect){const id=event.target.dataset.downloadSelect;if(event.target.checked){if(state.selectedDownloads.size>=200){event.target.checked=false;toast(messages.too_many_downloads,true);return;}state.selectedDownloads.add(id);}else state.selectedDownloads.delete(id);render();return;}if(!event.target.dataset.filter)return;filters()[event.target.dataset.filter]=event.target.value;filters().page=1;if(state.page==="logs")refresh(false);else render();});
  $("#login-form").addEventListener("submit",async event=>{event.preventDefault();const button=$("#login-form button");button.disabled=true;$("#login-error").textContent="";try{const payload=state.loginTicket?{ticket:state.loginTicket}:{adminToken:state.loginToken||$("#access-key").value.trim()};if(!state.loginTicket)state.loginToken=payload.adminToken;if(!$("#login-otp-wrap").hidden)payload.totp=$("#login-otp").value.trim();const result=await api("login","POST",payload);await acceptLogin(result);}catch(error){$("#login-error").textContent=error.message;}finally{button.disabled=false;}});
  $("#access-key").addEventListener("input",()=>{state.loginToken="";});
  $("#mfa-setup-form").addEventListener("submit",async event=>{event.preventDefault();const button=$("#mfa-setup-form button");button.disabled=true;$("#mfa-setup-error").textContent="";try{const result=await api("mfa/confirm","POST",{code:$("#mfa-setup-code").value.trim()});authenticated(result.admin);const codes=result.recoveryCodes||[];modal("Salve os códigos de recuperação","Eles aparecem uma única vez. Cada código pode substituir o Authenticator em um login.",`<pre class="mfa-recovery-codes">${e(codes.join("\n"))}</pre><p class="notice">Guarde estes códigos em um lugar seguro, fora deste computador.</p>`,btn("Copiar códigos","copy-recovery","","primary","copy")+btn("Concluir","close"));$("[data-action=copy-recovery]").addEventListener("click",()=>copy(codes.join("\n")));}catch(error){$("#mfa-setup-error").textContent=error.message;}finally{button.disabled=false;}});
  $("#mfa-setup-cancel").addEventListener("click",async()=>{try{await api("mfa/cancel","POST",{});}catch(_){}signOut();});
  $("#logout").addEventListener("click",async()=>{try{await api("logout","POST",{});signOut();}catch(error){toast(error.message,true);}});
  $("#refresh").addEventListener("click",()=>refresh(false));$("#refresh").innerHTML=icon("refresh");$("#logout").innerHTML=icon("logout");$("#mobile-menu").innerHTML=icon("menu");
  $("#mobile-menu").addEventListener("click",()=>$("#sidebar").classList.toggle("open"));$("#account-button").addEventListener("click",()=>accounts().catch(error=>toast(error.message,true)));
  $("#modal").addEventListener("cancel",event=>{if(state.uploading)event.preventDefault();});
  window.addEventListener("hashchange",route);$("#year").textContent=new Date().getFullYear();$(".mfa-year").textContent=new Date().getFullYear();
  document.addEventListener("error",event=>{if(event.target.matches?.(".minecraft-head img"))event.target.hidden=true;},true);
  setInterval(()=>{if(!document.hidden&&state.page==="online")document.querySelectorAll("[data-started-at]").forEach(node=>node.textContent=duration(node.dataset.startedAt));},1000);
  initialize();
})();
