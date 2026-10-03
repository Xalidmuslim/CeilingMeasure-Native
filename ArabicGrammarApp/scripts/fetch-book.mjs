import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

const OUT = resolve('app/src/main/assets/book/content.json');

const syntax = [
  'Части речи','Определённость и неопределенность имени','Род имени','Склонение имён','Предлоги родительного падежа',
  'Переходные и непереходные глаголы','Идафа','Обращение','Именное предложение','Глагольное предложение',
  'إن и его сёстры','Имена двухпадежного склонения','Слова, носящие смысл глаголов','Скрытое склонение',
  'Обстоятельство образа действия','Предлоги условного наклонения','Обстоятельство места и времени действия',
  'Вопросительное предложение','Абсолютный масдар','كان и его сёстры','Предлоги сослагательного наклонения',
  'Глаголы نعم и بئس','Союзы','Глаголы для выражения удивления','Обстоятельство причины или цели',
  'Сопутствующее имя','Предложение, содержащее исключение','ظن и его сёстры','Частицы, усекающие два глагола',
  'Конкретизирующее имя','Усиление','Предлог لا общего отрицания','Склоняемые и несклоняемые слова',
  'كاد и его сёстры','Частица отрицания ما в хиджазском наречии','Количественные числительные',
  'Порядковые числительные','Сложные предложения'
];

const morphology = [
  'Глагол. Трёхбуквенные и четырёхбуквенные глаголы','Глаголы правильные и неправильные','Породы трёхбуквенного глагола',
  'Породы четырёхбуквенного глагола','Особенности пород трёхбуквенного глагола','Спряжение трёхбуквенного правильного глагола в прошедшем времени',
  'Спряжение трёхбуквенного подобноправильного глагола в прошедшем времени','Спряжение удвоенного глагола в прошедшем времени',
  'Спряжение трёхбуквенных глаголов II-X пород в прошедшем времени','Спряжение четырёхбуквенных глаголов в прошедшем времени',
  'Спряжение трёхбуквенного недостаточного глагола в прошедшем времени','Спряжение пород недостаточного глагола в прошедшем времени',
  'Спряжение трёхбуквенного пустого глагола в прошедшем времени','Спряжение пород пустого глагола в прошедшем времени',
  'Глагол настояще-будущего времени','Спряжение трёхбуквенного правильного глагола в настояще-будущем времени',
  'Спряжение пород правильного трёхбуквенного глагола в настояще-будущем времени','Спряжение удвоенного глагола в настояще-будущем времени',
  'Спряжение трёхбуквенного недостаточного глагола в настояще-будущем времени','Спряжение пород недостаточного глагола в настояще-будущем времени',
  'Спряжение трёхбуквенного пустого глагола в настояще-будущем времени','Спряжение пород пустого трёхбуквенного глагола в настояще-будущем времени',
  'Спряжение трёхбуквенного подобноправильного глагола в настояще-будущем времени','Спряжение пород подобноправильного глагола в настояще-будущем времени',
  'Глагол сослагательного наклонения','Глагол условного наклонения или усечённый глагол','Глагол повелительного наклонения',
  'Глагол страдательного залога','Присоединение к глаголу буквы нун для усиления','Двойственное число','Множественное число',
  'Пять имён','Масдар','Масдар однократности','Масдар образа действия','Действительное причастие',
  'Действительное причастие преувеличения','Имя прилагательное, сходное с действительным причастием','Страдательное причастие',
  'Имя времени или места действия','Имя орудия действия','Сравнительная и превосходная степень имени','Уменьшительно-ласкательная форма имени'
];

const extras = ['Словарь к учебнику','Русско-арабский словарь','Тексты для чтения','Список использованной литературы','Содержание'];
const topicDefs = [
  ...syntax.map((title,i)=>({id:`s${i+1}`,chapter:'syntax',number:i+1,title})),
  ...morphology.map((title,i)=>({id:`m${i+1}`,chapter:'morphology',number:i+1,title})),
  ...extras.map((title,i)=>({id:`x${i+1}`,chapter:'extras',number:i+1,title}))
];

const aliases = {
  s2:['определенность и неопределенность имени'], s4:['склонение имен'], s7:['идафа'],
  s11:['его сестры'], s12:['имена двухпадежного склонения'], s20:['глагол и его сестры','и его сестры'],
  s22:['глаголы','نعم','بئس'], s26:['сопутствующее имя'], s28:['и его сестры'],
  s32:['общего отрицания','родового отрицания'], s34:['и его сестры'],
  m1:['трехбуквенные и четырехбуквенные глаголы'], m6:['правильного глагола в прошедшем времени'],
  m7:['подобноправильного глагола в прошедшем времени'], m9:['ii-x пород в прошедшем времени'],
  m11:['недостаточного глагола в прошедшем времени'], m13:['пустого глагола в прошедшем времени'],
  m16:['правильного глагола в настояще-будущем времени'], m17:['пород правильного','настояще-будущем времени'],
  m19:['недостаточного глагола в настояще-будущем времени'], m21:['пустого глагола в настояще-будущем времени'],
  m22:['пород пустого','настояще-будущем времени'], m23:['подобноправильного глагола в настояще-будущем времени'],
  m24:['пород подобноправильного','настояще-будущем времени'], m26:['условного наклонения','усеченный глагол'],
  m29:['буквы нун для усиления'], m32:['пять имен'], m43:['уменьшительно-ласкательная форма имени'],
  x1:['словарь к учебнику'], x2:['русско-арабский словарь'], x3:['тексты для чтения'],
  x4:['список использованной литературы'], x5:['содержание']
};

function norm(s='') {
  return s.toLowerCase().normalize('NFC').replace(/ё/g,'е').replace(/[«»“”„"'\x60´]/g,' ')
    .replace(/[ـ]/g,'').replace(/[^a-zа-я0-9\u0600-\u06ff\s-]/giu,' ').replace(/\s+/g,' ').trim();
}

function decodeEntities(s) {
  const named = {nbsp:' ',amp:'&',lt:'<',gt:'>',quot:'"',apos:"'",laquo:'«',raquo:'»',ndash:'–',mdash:'—',hellip:'…'};
  return s.replace(/&(#x?[0-9a-f]+|[a-z]+);/gi,(m,k)=>{
    if (k[0]==='#') {
      const hex=k[1]?.toLowerCase()==='x', n=parseInt(k.slice(hex?2:1),hex?16:10);
      return Number.isFinite(n)?String.fromCodePoint(n):m;
    }
    return named[k.toLowerCase()] ?? m;
  });
}
function htmlText(inner) {
  return decodeEntities(inner.replace(/<br\s*\/?\s*>/gi,'\n').replace(/<[^>]+>/g,' '))
    .replace(/[\t\r ]+/g,' ').replace(/ *\n */g,'\n').replace(/\n{3,}/g,'\n\n').trim().normalize('NFC');
}
function classify(text) {
  const a=(text.match(/[\u0600-\u06ff]/g)||[]).length, r=(text.match(/[А-Яа-яЁё]/g)||[]).length;
  if (/^упражнения для закрепления$/i.test(text.trim())) return 'exercise-heading';
  if (a>0 && a>=r*1.2) return 'arabic';
  if (a>0 && r>0) return 'mixed';
  return 'text';
}
function extractBlocks(html, sourceId) {
  const clean=html.replace(/<script[\s\S]*?<\/script>/gi,'').replace(/<style[\s\S]*?<\/style>/gi,'')
    .replace(/<noscript[\s\S]*?<\/noscript>/gi,'').replace(/<svg[\s\S]*?<\/svg>/gi,'');
  const re=/<(h1|h2|h3|h4|p|li|tr)\b[^>]*>([\s\S]*?)<\/\1>/gi;
  const out=[]; let m;
  while((m=re.exec(clean))) {
    const text=htmlText(m[2]); if(!text) continue;
    if (/^(дата добавления|мы поможем|скачать|предыдущая|следующая|главная|регистрация|вход)/i.test(text)) continue;
    if (/мы поможем в написании/i.test(text)) continue;
    out.push({sourceId,tag:m[1].toLowerCase(),type:classify(text),text});
  }
  const start=out.findIndex(b=>/хайбуллин|глава 1|части речи|переходные и непереходные/i.test(b.text));
  return start>0?out.slice(start):out;
}
function scoreCandidate(def, block) {
  if (block.text.length>210) return 0;
  const b=norm(block.text), variants=[def.title,...(aliases[def.id]||[])].map(norm).filter(Boolean);
  let best=0;
  for(const v of variants){
    if(b===v) best=Math.max(best,100);
    else if(b.includes(v)||v.includes(b)) best=Math.max(best,92);
    else {
      const toks=v.split(' ').filter(x=>x.length>=4); if(!toks.length) continue;
      const hit=toks.filter(t=>b.includes(t)).length/toks.length; best=Math.max(best,Math.round(hit*80));
    }
  }
  return best;
}
async function fetchWithRetry(url, attempts=4) {
  let last;
  for(let i=0;i<attempts;i++){
    try{
      const res=await fetch(url,{headers:{'user-agent':'Mozilla/5.0 ArabicGrammarApp/1.0'}});
      if(!res.ok) throw new Error(`${res.status} ${res.statusText}`);
      const buf=await res.arrayBuffer(), ct=res.headers.get('content-type')||'';
      const charset=(ct.match(/charset=([^;]+)/i)||[])[1]||'utf-8';
      let text; try{text=new TextDecoder(charset).decode(buf)}catch{text=new TextDecoder('utf-8').decode(buf)}
      return {text,url:res.url};
    }catch(e){last=e;await new Promise(r=>setTimeout(r,700*(i+1)))}
  }
  throw last;
}
function urlFor(id) {
  const special={69595:'opredelennost-i-neopredelennost-imeni',69596:'perehodnie-i-neperehodnie-glagoli',
    69597:'imena-dvuhpadezhnogo-skloneniya',69598:'obstoyatelstvo-obraza-deystviya',
    69599:'predlogi-soslagatelnogo-nakloneniya',69600:'obstoyatelstvo-prichini-ili-tseli',
    69601:'chastitsi-usekayushchie-dva-glagola'};
  return `https://studopedia.net/12_${id}_${special[id]||'sklonyaemie-i-nesklonyaemie-slova'}.html`;
}

const pages=[], blocks=[];
for(let id=69595;id<=69631;id++){
  const {text,url}=await fetchWithRetry(urlFor(id));
  const title=htmlText((text.match(/<title[^>]*>([\s\S]*?)<\/title>/i)||[])[1]||`Страница ${id}`);
  const extracted=extractBlocks(text,id);
  if(extracted.length<5) throw new Error(`Too little content for ${url}`);
  const startIndex=blocks.length; blocks.push(...extracted);
  pages.push({id,url,title,startIndex,endIndex:blocks.length});
  console.log(`Fetched ${id}: ${title} (${extracted.length} blocks)`);
}

const sections=[]; let cursor=0;
for(const def of topicDefs){
  let bestIndex=-1,bestScore=0;
  for(let i=cursor;i<blocks.length;i++){
    const s=scoreCandidate(def,blocks[i]); if(s>bestScore){bestScore=s;bestIndex=i} if(s>=100) break;
  }
  if(bestIndex>=0&&bestScore>=45){
    sections.push({...def,startIndex:bestIndex,found:true,score:bestScore});
    blocks[bestIndex].type='section-heading'; blocks[bestIndex].sectionId=def.id; cursor=bestIndex+1;
  }else sections.push({...def,startIndex:null,found:false,score:bestScore});
}
for(let i=0;i<sections.length;i++){
  const s=sections[i]; if(s.startIndex==null){s.endIndex=null;continue}
  const next=sections.slice(i+1).find(x=>x.startIndex!=null); s.endIndex=next?.startIndex??blocks.length;
  s.exerciseCount=blocks.slice(s.startIndex,s.endIndex).filter(b=>b.type==='exercise-heading').length;
}

const arabicChars=blocks.reduce((n,b)=>n+(b.text.match(/[\u0600-\u06ff]/g)||[]).length,0);
const totalChars=blocks.reduce((n,b)=>n+b.text.length,0);
const bad=blocks.filter(b=>/\?\?\?\?|�/.test(b.text)).length;
const found=sections.filter(s=>s.found).length;
const coreFound=sections.filter(s=>s.found&&(s.chapter==='syntax'||s.chapter==='morphology')).length;

const payload={version:1,generatedAt:new Date().toISOString(),
  book:{title:'Краткое изложение грамматики арабского языка',author:'И. Н. Хайбуллин',edition:'Издание второе. Исправленное и дополненное'},
  sources:{primary:'Studopedia HTML copy with Unicode Arabic',
    verification:'Quran Academy PDF: https://files.quranacademy.org/general/d/90/2370-grammatika-arabskogo-yazyka-ishmurat-khaybullin_d90q84lylwoznglkqwwy.pdf',
    pages:pages.map(p=>({id:p.id,url:p.url,title:p.title}))},
  stats:{sourcePages:pages.length,blocks:blocks.length,totalChars,arabicChars,badEncodingBlocks:bad,sectionsFound:found,coreSectionsFound:coreFound},
  sections,blocks};

if(pages.length!==37) throw new Error(`Expected 37 source pages, got ${pages.length}`);
if(arabicChars<18000) throw new Error(`Arabic coverage too small: ${arabicChars}`);
if(totalChars<250000) throw new Error(`Book text coverage too small: ${totalChars}`);
if(bad>3) throw new Error(`Encoding artifacts detected in ${bad} blocks`);
if(coreFound<70) throw new Error(`Only ${coreFound}/81 core sections detected`);

await mkdir(dirname(OUT),{recursive:true});
await writeFile(OUT,JSON.stringify(payload));
console.log(`Wrote ${OUT}`);
console.log(JSON.stringify(payload.stats,null,2));
