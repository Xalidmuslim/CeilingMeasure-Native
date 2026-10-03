(() => {
  const app = document.getElementById('app');
  const KEY = 'arabicGrammarStateV2';
  let book = null;
  let view = { name:'home', payload:null };
  const stack = [];
  let panel = null;

  const defaultState = {
    lastSection:'s1', completed:[], bookmarks:[], notes:{}, exerciseDone:[],
    settings:{theme:'light',font:1,arabic:1.08,line:1.7,gap:1,fontFamily:'system',arabicFamily:'naskh',animations:true}
  };
  let state = loadState();

  function loadState(){
    try {
      const parsed=JSON.parse(localStorage.getItem(KEY)||'{}');
      return {...defaultState,...parsed,settings:{...defaultState.settings,...(parsed.settings||{})}};
    } catch { return JSON.parse(JSON.stringify(defaultState)); }
  }
  function save(){ localStorage.setItem(KEY,JSON.stringify(state)); applySettings(); }
  function applySettings(){
    const dark = state.settings.theme==='dark' || (state.settings.theme==='auto' && matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.dataset.theme=dark?'dark':'light';
    const bodyFonts={system:'system-ui,-apple-system,"Noto Sans",sans-serif',serif:'Georgia,"Times New Roman",serif',compact:'"Arial Narrow",Arial,sans-serif'};
    const arabicFonts={naskh:'"Noto Naskh Arabic","Noto Sans Arabic",serif',kufi:'"Noto Kufi Arabic","Noto Sans Arabic",sans-serif',system:'serif'};
    document.documentElement.style.setProperty('--body-font',bodyFonts[state.settings.fontFamily]||bodyFonts.system);
    document.documentElement.style.setProperty('--arabic-font',arabicFonts[state.settings.arabicFamily]||arabicFonts.naskh);
    document.documentElement.style.setProperty('--fs',state.settings.font);
    document.documentElement.style.setProperty('--afs',state.settings.arabic);
    document.documentElement.style.setProperty('--lh',state.settings.line);
    document.documentElement.style.setProperty('--gap',`${state.settings.gap}rem`);
    document.body.style.scrollBehavior=state.settings.animations?'smooth':'auto';
  }
  function esc(s=''){return String(s).replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));}
  function norm(s=''){return s.toLowerCase().normalize('NFC').replace(/ё/g,'е').replace(/[^a-zа-я0-9\u0600-\u06ff\s-]/giu,' ').replace(/\s+/g,' ').trim();}
  function byId(id){return book.sections.find(s=>s.id===id)}
  function coreSections(ch){return book.sections.filter(s=>s.chapter===ch)}
  function pct(){const core=book.sections.filter(s=>s.chapter==='syntax'||s.chapter==='morphology'); return Math.round(core.filter(s=>state.completed.includes(s.id)).length/core.length*100)||0}
  function goto(name,payload=null,{replace=false}={}){
    if(!replace) stack.push({...view});
    view={name,payload}; panel=null; window.scrollTo(0,0); render();
  }
  window.appBack = () => {
    if(panel){panel=null;render();return true;}
    if(view.name==='home') return false;
    const prev=stack.pop()||{name:'home',payload:null};
    view=prev; window.scrollTo(0,0); render(); return true;
  };

  function nav(active){
    const items=[['home','⌂','Главная'],['library','▣','Книга'],['practice','✓','Практика'],['search','⌕','Поиск'],['settings','⚙','Настройки']];
    return `<nav class="nav"><div class="navIn">${items.map(([n,i,l])=>`<button class="${active===n?'active':''}" data-nav="${n}"><i>${i}</i><span>${l}</span></button>`).join('')}</div></nav>`;
  }
  function shell(content,active='home',withNav=true){ return `<div class="app"><main class="page">${content}</main>${withNav?nav(active):''}</div>`; }
  function actionCard(navName,icon,title,desc){return `<button class="card action" data-nav="${navName}"><div class="actionIcon">${icon}</div><div><strong>${title}</strong><span>${desc}</span></div><span class="chev">›</span></button>`}

  function home(){
    const done=state.completed.filter(x=>/^[sm]/.test(x)).length;
    const current=byId(state.lastSection)||byId('s1');
    return shell(`
      <div class="top"><div><div class="kicker">И. Н. Хайбуллин</div><h1>Арабская грамматика</h1></div><button class="iconBtn" data-action="bookmarks">☆</button></div>
      <section class="hero"><div class="heroRow"><div><div class="kicker">Продолжить обучение</div><h2>${esc(current.title)}</h2><p>${current.chapter==='syntax'?'Глава 1 · Синтаксис':'Глава 2 · Морфология'} · § ${current.number}</p><button class="cta" data-open-section="${current.id}">Продолжить →</button></div><div class="ring" style="--p:${pct()}"><b>${pct()}%</b></div></div></section>
      <div class="stats"><div class="stat"><b>${done}/81</b><span>тем освоено</span></div><div class="stat"><b>${state.exerciseDone.length}</b><span>упражнений отмечено</span></div><div class="stat"><b>${book.stats.arabicChars.toLocaleString('ru')}</b><span>арабских знаков</span></div></div>
      <div class="sectionHead"><div><div class="kicker">Основной маршрут</div><h2>Две главы — один курс</h2></div><button data-nav="library">Всё содержание ›</button></div>
      <div class="cards">
        <button class="card chapter" data-chapter="syntax"><div class="chapterMark">نحو</div><div><small>Глава 1 · 38 тем</small><h3>Синтаксис</h3><p>Части речи, падежи, предложения, частицы и сложные конструкции.</p></div><span class="chev">›</span></button>
        <button class="card chapter" data-chapter="morphology"><div class="chapterMark">صرف</div><div><small>Глава 2 · 43 темы</small><h3>Морфология</h3><p>Глагольные породы, спряжения, масдары и производные формы.</p></div><span class="chev">›</span></button>
      </div>
      <div class="sectionHead"><div><div class="kicker">Не только чтение</div><h2>Закрепить материал</h2></div></div>
      <div class="cards">
        ${actionCard('practice','✦','Практика','Упражнения автора и отметка выполнения')}
        ${actionCard('search','⌕','Поиск по книге','По русскому и арабскому тексту')}
        ${actionCard('library','▤','Словарь и тексты','Все разделы книги без PDF')}
      </div>
      <div class="sectionHead"><div><div class="kicker">Оцифровка</div><h2>Текст встроен в приложение</h2></div></div>
      <div class="card"><p style="margin:0">PDF не используется. ${book.stats.sourcePages} веб-страниц книги преобразованы в ${book.stats.blocks.toLocaleString('ru')} текстовых блоков с сохранением арабского Unicode и огласовок.</p></div>
    `,'home');
  }

  function library(chapter=null){
    if(chapter) return topicList(chapter);
    return shell(`
      <div class="top"><div><div class="kicker">Вся книга</div><h1>Содержание</h1></div></div>
      <p class="muted">Текст хранится как обычный контент приложения: его можно искать, читать без масштабирования PDF и сохранять прогресс.</p>
      <div class="cards">
        <button class="card chapter" data-chapter="syntax"><div class="chapterMark">1</div><div><small>38 тем</small><h3>Синтаксис</h3><p>${coreSections('syntax').filter(s=>state.completed.includes(s.id)).length} тем отмечено</p></div><span class="chev">›</span></button>
        <button class="card chapter" data-chapter="morphology"><div class="chapterMark">2</div><div><small>43 темы</small><h3>Морфология</h3><p>${coreSections('morphology').filter(s=>state.completed.includes(s.id)).length} тем отмечено</p></div><span class="chev">›</span></button>
        ${book.sections.filter(s=>s.chapter==='extras'&&s.found).map(s=>`<button class="card action" data-open-section="${s.id}"><div class="actionIcon">▤</div><div><strong>${esc(s.title)}</strong><span>Дополнительный раздел</span></div><span class="chev">›</span></button>`).join('')}
      </div>
    `,'library');
  }
  function topicList(chapter){
    const items=coreSections(chapter);
    return shell(`
      <div class="top"><button class="iconBtn" data-back>←</button><div style="flex:1"><div class="kicker">${chapter==='syntax'?'Глава 1':'Глава 2'}</div><h1>${chapter==='syntax'?'Синтаксис':'Морфология'}</h1></div></div>
      <div class="topicList">${items.map(s=>`<button class="topic ${state.completed.includes(s.id)?'done':''}" data-open-section="${s.id}"><span class="topicNum">${state.completed.includes(s.id)?'✓':s.number}</span><span><b>${esc(s.title)}</b><small>${s.found?'Полный текст':'Заголовок найден приближённо'}</small></span><span class="chev">›</span></button>`).join('')}</div>
    `,'library');
  }

  function reader(id){
    const s=byId(id)||byId('s1'); state.lastSection=s.id; save();
    const start=s.startIndex ?? 0, end=s.endIndex ?? Math.min(book.blocks.length,start+250);
    const blocks=book.blocks.slice(start+1,end);
    let inExercise=false; const rendered=[];
    for(const b of blocks){
      if(b.type==='section-heading') break;
      if(b.type==='exercise-heading'){
        if(inExercise) rendered.push('</div>');
        inExercise=true; rendered.push(`<div class="exerciseZone"><div class="block exerciseHeading">${esc(b.text)}</div>`); continue;
      }
      const cls=b.type==='arabic'?'arabic':b.type==='mixed'?'mixed':'';
      rendered.push(`<div class="block ${cls}" dir="auto">${esc(b.text)}</div>`);
    }
    if(inExercise) rendered.push('</div>');
    const note=state.notes[s.id]||'';
    return shell(`
      <div class="readerTop"><button class="iconBtn" data-back>←</button><div class="title"><small>${s.chapter==='syntax'?'Синтаксис':s.chapter==='morphology'?'Морфология':'Доп. раздел'} · § ${s.number}</small><b>${esc(s.title)}</b></div><button class="iconBtn" data-bookmark="${s.id}">${state.bookmarks.includes(s.id)?'★':'☆'}</button></div>
      <section class="readerMeta"><div class="kicker">${s.found?'Точный текст раздела':'Текст найден по ближайшему совпадению'}</div><h1>${esc(s.title)}</h1><p>Арабские строки сохранены как Unicode-текст с огласовками. PDF в приложении не используется.</p><button class="complete ${state.completed.includes(s.id)?'done':''}" data-complete="${s.id}">${state.completed.includes(s.id)?'✓ Освоено':'○ Отметить тему'}</button></section>
      <article class="bookText">${rendered.join('')}</article>
      <div class="readerActions"><button data-panel="practice">☷ Упражнения</button><button data-panel="note">✎ Заметка</button><button data-panel="info">ⓘ Источник</button></div>
      <div class="collapsible ${panel?'open':''}">${panelContent(s,note)}</div>
    `,'',false);
  }
  function panelContent(s,note){
    if(panel==='note') return `<div class="panel"><b>Заметка к теме</b><textarea class="note" id="noteText">${esc(note)}</textarea><button class="saveBtn" data-save-note="${s.id}">Сохранить</button></div>`;
    if(panel==='practice') return `<div class="panel"><b>Упражнения темы</b><p class="muted">В тексте выше блоки «Упражнения для закрепления» выделены отдельными карточками.</p><button class="saveBtn" data-complete="${s.id}">${state.completed.includes(s.id)?'Снять отметку':'Отметить тему освоенной'}</button></div>`;
    if(panel==='info') return `<div class="panel"><b>Откуда взят текст</b><p class="muted">Основной источник оцифровки — открытая HTML-копия книги на Studopedia. Наличие того же издания дополнительно проверено по PDF-копии Quran Academy. При сборке приложение автоматически проверяет количество арабских символов и отсутствие явных ошибок кодировки.</p></div>`;
    return '';
  }

  function searchPage(){
    return shell(`<div class="top"><div><div class="kicker">Все разделы</div><h1>Поиск</h1></div></div><div class="searchBox"><span>⌕</span><input id="searchInput" placeholder="Например: идафа, مفعول, масдар" autocomplete="off" /></div><div id="searchResults"><p class="muted">Поиск работает и по русскому, и по арабскому тексту.</p></div>`,'search');
  }
  function doSearch(q){
    const root=document.getElementById('searchResults'); if(!root) return;
    q=norm(q); if(q.length<2){root.innerHTML='<p class="muted">Введите минимум 2 символа.</p>';return;}
    const out=[];
    for(const s of book.sections.filter(x=>x.found)){
      const text=book.blocks.slice(s.startIndex,s.endIndex).map(b=>b.text).join(' ');
      const n=norm(text); if(!n.includes(q)) continue;
      const token=q.split(' ')[0], rawIdx=Math.max(0,norm(text).indexOf(token));
      const snippet=text.slice(Math.max(0,rawIdx-90),rawIdx+260).replace(/\s+/g,' ');
      out.push({s,snippet}); if(out.length>=40) break;
    }
    root.innerHTML=out.length?out.map(({s,snippet})=>`<button class="card result" data-open-section="${s.id}"><em>${s.chapter==='syntax'?'Синтаксис':s.chapter==='morphology'?'Морфология':'Раздел'}</em><h3>${esc(s.title)}</h3><p>${esc(snippet)}</p></button>`).join(''):'<div class="error">Совпадений не найдено.</div>';
    wire();
  }

  function practice(){
    const items=book.sections.filter(s=>s.found&&(s.exerciseCount||0)>0);
    return shell(`<div class="top"><div><div class="kicker">Авторские задания</div><h1>Практика</h1></div></div><p class="muted">Открывай тему и выполняй упражнения прямо в цельном тексте книги.</p><div class="topicList">${items.map(s=>`<button class="topic ${state.exerciseDone.includes(s.id)?'done':''}" data-practice="${s.id}"><span class="topicNum">${state.exerciseDone.includes(s.id)?'✓':'☷'}</span><span><b>${esc(s.title)}</b><small>${s.exerciseCount} блок(а) упражнений</small></span><span class="chev">›</span></button>`).join('')}</div>`,'practice');
  }

  function bookmarks(){
    const items=state.bookmarks.map(byId).filter(Boolean);
    return shell(`<div class="top"><button class="iconBtn" data-back>←</button><div style="flex:1"><div class="kicker">Сохранённое</div><h1>Закладки</h1></div></div>${items.length?`<div class="topicList">${items.map(s=>`<button class="topic" data-open-section="${s.id}"><span class="topicNum">★</span><span><b>${esc(s.title)}</b><small>${s.chapter==='syntax'?'Синтаксис':s.chapter==='morphology'?'Морфология':'Раздел'}</small></span><span class="chev">›</span></button>`).join('')}</div>`:'<div class="card"><p class="muted">Пока нет закладок.</p></div>'}`,'home');
  }

  function settings(){
    const x=state.settings;
    return shell(`
      <div class="top"><div><div class="kicker">Под себя</div><h1>Настройки</h1></div></div>
      <div class="setting"><label><span>Тема</span></label><div class="themeBtns"><button data-theme="light" class="${x.theme==='light'?'active':''}">День</button><button data-theme="dark" class="${x.theme==='dark'?'active':''}">Ночь</button><button data-theme="auto" class="${x.theme==='auto'?'active':''}">Система</button></div></div>
      <div class="setting"><label><span>Шрифт русского текста</span></label><div class="choiceBtns"><button data-font-family="system" class="${x.fontFamily==='system'?'active':''}">Системный</button><button data-font-family="serif" class="${x.fontFamily==='serif'?'active':''}">Книжный</button><button data-font-family="compact" class="${x.fontFamily==='compact'?'active':''}">Компактный</button></div></div>
      <div class="setting"><label><span>Размер русского текста</span><b>${Math.round(x.font*100)}%</b></label><input type="range" min="0.85" max="1.35" step="0.05" value="${x.font}" data-setting="font"></div>
      <div class="setting"><label><span>Шрифт арабского текста</span></label><div class="choiceBtns"><button data-arabic-family="naskh" class="${x.arabicFamily==='naskh'?'active':''}">Naskh</button><button data-arabic-family="kufi" class="${x.arabicFamily==='kufi'?'active':''}">Kufi</button><button data-arabic-family="system" class="${x.arabicFamily==='system'?'active':''}">Системный</button></div></div>
      <div class="setting"><label><span>Размер арабского текста</span><b>${Math.round(x.arabic*100)}%</b></label><div class="block arabic" style="padding:8px 0">النَّحْوُ وَالصَّرْفُ</div><input type="range" min="0.9" max="1.5" step="0.05" value="${x.arabic}" data-setting="arabic"></div>
      <div class="setting"><label><span>Межстрочный интервал</span><b>${x.line}</b></label><input type="range" min="1.35" max="2.1" step="0.05" value="${x.line}" data-setting="line"></div>
      <div class="setting"><label><span>Отступы между блоками</span><b>${x.gap}</b></label><input type="range" min="0.5" max="1.8" step="0.1" value="${x.gap}" data-setting="gap"></div>
      <div class="card"><small>Контроль целостности</small><p>${book.stats.sourcePages}/37 источников · ${book.stats.coreSectionsFound}/81 основных тем распознано · ${book.stats.badEncodingBlocks} блоков с явной ошибкой кодировки.</p></div>
    `,'settings');
  }

  function render(){
    applySettings();
    if(!book){ app.innerHTML='<div class="loading"><div><div class="spinner"></div><h2>Подготавливаю книгу…</h2><p class="muted">Загружается встроенный текст.</p></div></div>'; return; }
    let html='';
    if(view.name==='home') html=home();
    else if(view.name==='library') html=library(view.payload?.chapter||null);
    else if(view.name==='reader') html=reader(view.payload?.id||state.lastSection);
    else if(view.name==='search') html=searchPage();
    else if(view.name==='practice') html=practice();
    else if(view.name==='bookmarks') html=bookmarks();
    else if(view.name==='settings') html=settings();
    app.innerHTML=html; wire();
  }

  function wire(){
    document.querySelectorAll('[data-nav]').forEach(el=>el.onclick=()=>goto(el.dataset.nav));
    document.querySelectorAll('[data-back]').forEach(el=>el.onclick=()=>window.appBack());
    document.querySelectorAll('[data-chapter]').forEach(el=>el.onclick=()=>goto('library',{chapter:el.dataset.chapter}));
    document.querySelectorAll('[data-open-section]').forEach(el=>el.onclick=()=>goto('reader',{id:el.dataset.openSection}));
    document.querySelectorAll('[data-action="bookmarks"]').forEach(el=>el.onclick=()=>goto('bookmarks'));
    document.querySelectorAll('[data-bookmark]').forEach(el=>el.onclick=()=>{const id=el.dataset.bookmark;state.bookmarks=state.bookmarks.includes(id)?state.bookmarks.filter(x=>x!==id):[...state.bookmarks,id];save();render();});
    document.querySelectorAll('[data-complete]').forEach(el=>el.onclick=()=>{const id=el.dataset.complete;state.completed=state.completed.includes(id)?state.completed.filter(x=>x!==id):[...state.completed,id];save();render();});
    document.querySelectorAll('[data-panel]').forEach(el=>el.onclick=()=>{panel=panel===el.dataset.panel?null:el.dataset.panel;render();});
    document.querySelectorAll('[data-save-note]').forEach(el=>el.onclick=()=>{state.notes[el.dataset.saveNote]=document.getElementById('noteText')?.value||'';save();panel=null;render();});
    const si=document.getElementById('searchInput'); if(si){si.oninput=e=>doSearch(e.target.value); setTimeout(()=>si.focus(),80)}
    document.querySelectorAll('[data-practice]').forEach(el=>el.onclick=()=>{const id=el.dataset.practice;if(!state.exerciseDone.includes(id))state.exerciseDone.push(id);save();goto('reader',{id});});
    document.querySelectorAll('[data-theme]').forEach(el=>el.onclick=()=>{state.settings.theme=el.dataset.theme;save();render();});
    document.querySelectorAll('[data-font-family]').forEach(el=>el.onclick=()=>{state.settings.fontFamily=el.dataset.fontFamily;save();render();});
    document.querySelectorAll('[data-arabic-family]').forEach(el=>el.onclick=()=>{state.settings.arabicFamily=el.dataset.arabicFamily;save();render();});
    document.querySelectorAll('[data-setting]').forEach(el=>el.oninput=()=>{state.settings[el.dataset.setting]=Number(el.value);save();render();});
  }

  async function init(){
    applySettings(); render();
    try{
      const r=await fetch('book/content.json',{cache:'no-store'}); if(!r.ok) throw new Error(`content.json: ${r.status}`);
      book=await r.json();
      if(!book?.blocks?.length) throw new Error('Книга не содержит текстовых блоков');
      render();
    }catch(e){app.innerHTML=`<div class="loading"><div class="error"><b>Не удалось открыть встроенный текст книги.</b><p>${esc(e.message)}</p></div></div>`;}
  }
  init();
})();
