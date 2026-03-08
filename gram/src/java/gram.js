"use strict";
function $(q,n){if(!n)n=document;return Array.from(n.querySelectorAll(q));}
function $0(q,n){if(!n)n=document;return n.querySelector(q);}
if(undefined===window.on){
	Object.defineProperty(Node.prototype,"rm",{get(){return ()=>{this?.parentNode?.removeChild(this);};}});
	Object.defineProperty(Element.prototype,"addClass",{get(){return function addClass(c){this.classList.add(c);return this;};}});
	Object.defineProperty(Element.prototype,"rmClass",{get(){return function rmClass(c){this.classList.remove(c);return this;};}});
	Object.defineProperty(Array.prototype,"on",{get(){return function arrayOn(t,f,o){
		this.forEach(function addEvent(e){e.addEventListener(t,f,o);});return this;};}});
	[EventTarget.prototype,window].forEach(function setOn(p){
		Object.defineProperty(p,"on",{get(){return function on(t,f,o){this.addEventListener(t,f,o);return this;};}});
		Object.defineProperty(p,"off",{get(){return function off(t,f){this.removeEventListener(t,f);return this;};}});});}
function gram(){
function isMobile(){return window.matchMedia("(max-width: 899.999px)").matches;}
function isPagespeedTest(){return navigator.userAgent.includes("Chrome/136\.0\.0\.0");}
function inIframe(){return window.self!==window.top;}
const cachedPages=new Map();
function getWaitTime(){
	const pnt=window.performance?.getEntriesByType('navigation').pop();
	if(pnt?.startTime&&pnt?.loadEventEnd){
		return (pnt?.loadEventEnd-pnt?.startTime);}
	return 1000;};
function ajax(method,url,onLoad,onTimeout,body){
	const req=new XMLHttpRequest();req.open(method,url);req.timeout=getWaitTime()*2;
	req.addEventListener("load",onLoad);req.addEventListener("timeout",onTimeout);req.send(body);}
function isLongRequest(e,factor){
	if(!factor){factor=5;}
	if("cache"===e?.deliveryType){return false;}
	const total=e?.serverTiming?.find(function findTotal(t){return "total"===t.name})?.duration;
	if(500<total){
		const url=new URL(e.name);
		console.log(document.location.host+" is slow: "+url.pathname);return true;}
	if(e){
		const d=e.duration||e.responseEnd-e.startTime;
		if(e.transferSize>0&&Math.max(e.transferSize,1400)<d*factor){
			console.log("your connection is slow");return true;
	}}return false;}
function parseToMilliseconds(durStr){
	if(durStr.endsWith("ms")){
		return Number(durStr.substring(0,durStr.length-2));
	}else if(durStr.endsWith("s")){
		return Number(durStr.substring(0,durStr.length-1))*1000;
	}return 200;}
function getDocument(response){
	return Document.parseHTMLUnsafe?Document.parseHTMLUnsafe(response):new DOMParser().parseFromString(response,"text/html");}
function enhanceLinks(q,n){}
if(!$0(".adminform")){try{new PerformanceObserver(function detectSlowness(list){
	list.getEntries().forEach(function check(e){
		if(isLongRequest(e)){
			$0("html").addClass("noPreload");}});
}).observe({type:"resource"});
var currentTime=Date.now();
enhanceLinks=function enhanceLinks(query,n){
	if(!n)n=document;
	function preloadLink(e){
		const a=e.currentTarget;
		a.off("mouseenter",preloadLink).off("touchstart",preloadLink).off("focus",preloadLink);
		const u=new URL(a.href,document.location.origin);
		if($0("html.noPreload")||a.classList.contains("noPreload")||document.location.origin!==u.origin||a.getAttribute("download")||"_blank"===a.getAttribute("target")){
			a.addClass("noPreload");
		}else if(!isCached(u)&&!$0("html.noPreload")){
			ajax("GET",u.href,function cachePage(r){
				if(4===r.target.readyState&&200===r.target.status){
					const doc=getDocument(r.target.response);
					if(!isMobile()){enhancePicture(a,doc);}
					const cacheHeaders=r.target.getResponseHeader("cache-control")?.split(", ");
					const isHtml=r.target.getResponseHeader("content-type")?.startsWith("text/html");
					if(!a.classList.contains("noPreload")&&cacheHeaders&&isHtml){
						const revalidate=cacheHeaders.some(function testReval(h){return 0<=h.search("revalidate")||0<=h.search("no-cache")||0<=h.search("no-store");});
						const validFor=Number(cacheHeaders.find(function findAge(h){return h.startsWith("max-age=");})?.replace("max-age=",""));
						if($0("base").href===$0("base",doc).href&&!revalidate&&validFor){
							if(isCached(r.target.responseURL)){
								cachedPages.set(u.href,cachedPages.get(r.target.responseURL));
							}else{
								const entry={html:r.target.response,expires:Date.now()+(validFor*1000),url:r.target.responseURL};
								try{
									const perf=performance.getEntriesByName(r.target.responseURL).pop();
									const duration=perf.duration|0;
									entry.preload=" and preloaded in "+duration+" milliseconds";
								}catch(ex){}
								cachedPages.set(u.href,entry);}
							a.on('click',useCache);
							return;}}}
				a.addClass("noPreload");
			},function timedOut(r){$("html,a").forEach(function disableCache(l){l.addClass("noPreload");});});}
		if(isCached(u)){
			a.on('click',useCache);
			if(!isMobile()){enhancePicture(a,getDocument(cachedPages.get(u.href).html));}}}
	function useCache(e){
		const a=e.currentTarget;
		const u=new URL(a.href,document.location.origin);
		if(document.location.href===u.href){
			window.scrollTo(0,0);
			e.preventDefault();
		}else if(isCached(u)){
			$("[style]").forEach(function rmStyle(e){e.removeAttribute("style");});
			const scroll=[window.scrollX,window.scrollY];
			const backText=$0(".background-text-container");
			$(".removable").forEach(function rm(e){e.rm();});
			history.replaceState({url:location.href,html:document.documentElement.outerHTML,scroll:scroll,time:currentTime},document.title,location.href);
			if(backText){document.body.appendChild(backText);}
			const cached=cachedPages.get(u.href);
			showDocument(cached.html,true,function afterPresent(){
				if(cached.preload)try{
					$0("body>footer.downContent>p>.elapsed").insertAdjacentText('afterend',cached.preload);
				}catch(TypeError){}
				currentTime=Date.now();
				history.pushState({url:cached.url,html:cached.html,scroll:[0,0],time:currentTime},document.title,cached.url);
				window.scrollTo(0,0);});
			e.preventDefault();}}
	function isCached(url){
		return cachedPages.has(url.href)&&Date.now()<=Number(cachedPages.get(url.href).expires);}
	function showDocument(html,forwardFade,afterPresent){
		const doc=getDocument(html);
		clearFade(doc.body);
		clearFade(document.body);
		if(!window.matchMedia(`(prefers-reduced-motion: reduce)`).matches){
			document.body.addClass(forwardFade?"fadeOut":"backOut");
			$("body>*").forEach(function(el){el.addClass("oldPage");});
			$("body>*",doc.body).forEach(function(el){document.body.appendChild(el);});
			let aniOutDur=parseToMilliseconds(window.getComputedStyle(document.body).animationDuration.toLowerCase());
			setTimeout(function fadeIn(){
				$("body>.oldPage").forEach(function(n){n.rm();});
				document.body.addClass(forwardFade?"fadeIn":"backIn");
				swapIn(doc,afterPresent);
				let aniInDur=parseToMilliseconds(window.getComputedStyle(document.body).animationDuration.toLowerCase());
				setTimeout(clearFade,aniInDur,document.body);
			},aniOutDur);
		}else{
			document.body=doc.body;
			swapIn(doc,afterPresent);}}
	function swapIn(doc,afterPresent){
		$("head>:not([rel='stylesheet'])").forEach(function(n){n.rm();});
		$("head>:not([rel='stylesheet'])",doc).forEach(function(n){
			document.head.appendChild(n.cloneNode(true));});
		setTimeout(init);
		if(afterPresent){afterPresent();}}
	function clearFade(element){
		element.rmClass("fadeIn").rmClass("fadeOut").rmClass("backIn").rmClass("backOut");}

	const links=$(query,n).on('mouseenter',preloadLink,{once:true})
		.on('touchstart',preloadLink,{once:true,passive:true})
		.on('focus',preloadLink,{once:true});
	try{const io=new IntersectionObserver(function onSeen(ioEntries){
			ioEntries.forEach(function loadThis(ent){
				if(ent.isIntersecting){
					ent.target.dispatchEvent(new FocusEvent("focus"));
					io.unobserve(ent.target);}})});
		links.forEach(function register(link){
			io.observe(link);});
	}catch(n){}
	if(null===window.onpopstate){
		history.scrollRestoration="manual";
		window.onpopstate=function swap(e){
			showDocument(e.state.html,currentTime<e.state.time,function scrollTo(){
				window.scrollTo(e.state.scroll[0],e.state.scroll[1]);
				currentTime=e.state.time;});};}}
}catch(x){}}
function enhancePicture(query,n){
	if(!n)n=document;
	function rmPlaceholder(e){//prevent flicker on Firefox
		const img=e.currentTarget;
		if(navigator.userAgent.includes("Firefox")){
			setTimeout(function cleanup(){
				const fig=img.closest("figure");
				$0(".removable",fig)?.rm();
				fig.removeAttribute("style");
			},100);
		}else{
			const fig=img.closest("figure");
			$0(".removable",fig)?.rm();
			fig.removeAttribute("style");
		}}
	function showZoom(e){
		e.preventDefault();
		const d=inIframe()?window.top.document:document;
		const pic=d.createElement("picture");
		$("source",e.currentTarget).forEach(function source(s){
			const src=d.createElement("source");
			src.srcset=s.dataset.highres;
			pic.appendChild(src).type=s.type;});
		const origImg=$0("img",e.currentTarget);
		const img=d.createElement("img");
		pic.appendChild(img).src=origImg.src;
		pic.title=origImg.title;
		pic.addClass("zoomed").addClass("removable").alt=origImg.alt;
		img.style.setProperty("transform-origin",(e.clientX)+"px "+(e.clientY)+"px");
		d.body.addClass("locked").on('keydown',closeZoom,{once:true}).appendChild(pic).on('click',closeZoom);}
	function closeZoom(e){
		const d=inIframe()?window.top.document:document;
		d.body.rmClass("locked");
		$0("picture.zoomed",d)?.rm();
	}

	if(query instanceof Element&&query.matches("html:not(.noPreload) .indexPage a.withFigure")&&!isPagespeedTest()){
		//replace low res images on homepage
		setTimeout(function loadHiRes(){try{
		const fig=$0("figure",query);
		fig.style.setProperty("height",fig.clientHeight+"px");
		const aPic=$0("picture:not(.swapped):not(.removable)",query);
		const newPic=$0("img[src=\""+decodeURI($0("img",query).src)+"\"]",n).closest("picture").cloneNode(true);
		aPic.addClass("removable").insertAdjacentElement('afterend',newPic);
		$0("img",newPic).on('load',rmPlaceholder).addClass("swapped");
		query.on('click',aPic.rm,{once:true});
		}catch(x){}},getWaitTime());
	}else if(typeof(query)==='string'){
		//lightbox
		$(query,n).forEach(function setZoom(e){
			$("source",e.currentTarget).forEach(function source(s){
				s.dataset.highres=s.dataset.highres||s.srcset.split(", ").pop().split(" ").shift();});
			e.on('click',showZoom).addClass("zoom");});}}
function enhanceSearch(query,n){
	if(!n)n=document;
	var unfocusTimer=null;
	var addTimer=null;
	var lastQuery=[];
	function cascadeAdd(box,ol,array){
		if(array.length&&document.body.contains(ol)){
			const s=array.shift();
			const a=document.createElement("a");
			a.href=box.closest("form").action+"?"+encodeURI(box.name)+"="+encodeURI(s);
			a.textContent=s;
			ol.appendChild(document.createElement("li")).appendChild(a);
			enhanceLinks("#"+ol.getAttribute("id")+">li:last-child>a");
			addTimer=window.matchMedia('(prefers-reduced-motion: reduce)').matches?cascadeAdd(box,ol,array):setTimeout(cascadeAdd,101,box,ol,array);}}
	function cascadeRm(lis){
		if(lis.length&&document.body.contains(lis.at(-1))){
			lis.pop().rm();
			window.matchMedia('(prefers-reduced-motion: reduce)').matches?cascadeRm(lis):setTimeout(cascadeRm,61,lis);}}
	function getSuggestions(e){
		const box=e.currentTarget||e.target;
		const list=box.nextElementSibling;
		box.removeAttribute('list');
		if(!$0("html.noPreload")&&undefined===box.dataset.pending&&2<box.value.length&&box.size>=box.value.length){
			ajax("GET",box.closest("form").action+"?suggestion="+box.value,function fillSuggestions(r){
				if(200===r.target.status){
					lastQuery=JSON.parse(r.target.response).slice(0,7);
					if(0!==lastQuery.length){
						if(addTimer){clearTimeout(addTimer);}
						cascadeRm($("li",list));
						cascadeAdd(box,list,lastQuery.slice());}}
				var pending=box.dataset.pending;
				delete box.dataset.pending;
				if(pending!==box.value){
					getSuggestions(e);
			}},function timedOut(r){box.off("input",getSuggestions);});
		box.dataset.pending=box.value;}}
	function searchFocus(e){
		if(unfocusTimer){clearTimeout(unfocusTimer);}
		const list=e.currentTarget.nextElementSibling;
		if(0!==lastQuery.length){
			cascadeRm($("li",list));
			cascadeAdd(e.currentTarget,list,lastQuery.slice());}}
	function searchBlur(e){
		if(unfocusTimer){clearTimeout(unfocusTimer);}
		unfocusTimer=setTimeout(cascadeRm,10000,$("li",(e.currentTarget||e.target).nextElementSibling));}
	function searchDown(e){
		const box=e.currentTarget;
		const list=box.nextElementSibling;
		const selected=$0(".selected",list);
		if(selected){
			switch(e.keyCode){
			case 40:// down
				selected.rmClass("selected").nextElementSibling?.addClass("selected");
				e.preventDefault();
				break;
			case 38:// up
				selected.rmClass("selected").previousElementSibling?.addClass("selected");
				e.preventDefault();
				break;
			case 39:// right
				box.value=selected.innerText;
				selected.rmClass("selected");
				e.preventDefault();
				break;
			case 13:// enter
				box.value=selected.innerText;
				e.preventDefault();
				$0("a",selected).dispatchEvent(new MouseEvent("click",{cancelable:true}));
				return;
			default:selected.rmClass("selected");}
		}else{
			switch(e.keyCode){
			case 40:// down
				list.firstElementChild.addClass("selected");
				e.preventDefault();
				break;
			case 38:// up
				list.lastElementChild.addClass("selected");
				e.preventDefault();
				break;}}
		$0(".selected a",list)?.dispatchEvent(new FocusEvent("focus"));}

	$(query,n).forEach(function initSearch(form){
		const lists=$(".suggestionList",form);
		if($0('input[type="search"]',form).on("input",getSuggestions)
				.on("focus",searchFocus).on("blur",searchBlur)
				.on("keydown",searchDown)&&lists.length===0){
			const list=document.createElement("ol");
			list.addClass("suggestionList").addClass("removable").setAttribute("id","list"+Math.floor(Math.random()*999999999999));
			const button=$0("button.search",n);
			button.parentNode.insertBefore(list,button);
		}else{
			lists.forEach(function hideLists(o){o.rmClass("show");});}});}
function enhanceLazyload(query,n){
	if(!n)n=document;
	function eagerLoad(imgs){//check that all not-lazy images are loaded before switching a lazy one to eager
		if(!$0("html.noPreload")&&$("img:not([loading='lazy'])").every(function isComplete(i){return i.complete;})){
			while(imgs.length){
				const img=imgs.pop();
				if(document.body.contains(img)){//showDocument() might have taken img off page
					img.loading="eager";
					if(img.complete){continue;}
					setTimeout(eagerLoad,getWaitTime()*10,imgs);
					break;
		}}}else{
			setTimeout(eagerLoad,getWaitTime()*20,imgs);}}

	setTimeout(eagerLoad,getWaitTime()*10,$(query,n).map(function getLazyImgs(node){return $("img[loading='lazy']",node);}).flat().reverse());}
function enhanceLayout(){
	function sizeIframe(iFrame){
		if(iFrame.contentDocument&&$0("main",iFrame.contentDocument)){
		let width=iFrame.parentNode.clientWidth-10;
		if(iFrame.matches(".singleArticle #comments"))try{
			width=$0("main.singleArticle>article>p:last-of-type").clientWidth;
		}catch(c){}
		iFrame.style.setProperty("width",width+'px');
		iFrame.style.setProperty("height",$0("main",iFrame.contentDocument).scrollHeight+50+'px');
		}return iFrame;}
	function onResize(e){
		$("iframe.resizeable",e.currentTarget.document).forEach(sizeIframe);
		if(1160===$0("main.singleArticle>article>p:last-of-type")?.clientWidth){$0("nav.seeAlso").addClass("max");}
		else{$0("nav.seeAlso")?.classList?.remove("max");}}

	$("iframe.resizeable").on("load",function(e){sizeIframe(e.currentTarget)});
	onResize({currentTarget:window});
	if(!inIframe()){
		new ResizeObserver(function resized(entries){
			onResize({currentTarget:window.parent});
		}).observe(document.body);
		if(null===window.onresize){
			window.onresize=onResize;}}}
function enhanceBackend(){
	$("form.adminArticleAdd>details.recent a").on('click',function addImage(e){
		e.preventDefault();
		const art=$0(".articleText");
		const sel=art.value.substring(art.selectionStart,art.selectionEnd);
		art.setRangeText("!["+sel+"]("+e.currentTarget.getAttribute("href")+")",art.selectionStart,art.selectionEnd,'select');
		$0(".recent").open=false;
	});}
function isLoaded(doc){return doc&&("complete"===doc.readyState);}
function checkBoxes(e){
	e.preventDefault();
	$("."+e.currentTarget.dataset.check,e.currentTarget.closest("fieldset,form")).forEach(function check(c){c.checked=true;});}
function init(){
	$("button[data-check]").on("click",checkBoxes);
	enhanceLayout();
	enhanceBackend();
	if($0("html:not(.dialup)")){
		enhancePicture("picture:not(a picture)");}
	if(isLongRequest(window.performance?.getEntriesByType('navigation').pop())||(navigator.connection?.saveData)){
		$0("html").addClass("noPreload");
	}else{
		enhanceLinks("html:not(.noPreload) a:not(.noPreload)");
		enhanceSearch("html:not(.noPreload) form.search");
		enhanceLazyload("html:not(.noPreload)");}}
if(isLongRequest(window.performance?.getEntriesByType('navigation').pop(),10)){
	$0("html").addClass("dialup");
	$("picture>source").forEach(function(s){
		const lowres=s.srcset.match(/(.+?) \d+w,/);
		if(lowres&&lowres[1]){
			s.dataset.highres=s.srcset.split(", ").pop().split(" ").shift();
			const pic=s.closest("picture");
			$0("img",pic).removeAttribute("width");
			const p=pic.parentElement;
			p.removeChild(pic);
			s.srcset=lowres[1];
			p.insertBefore(pic,p.firstChild);}});}
isLoaded(document)?setTimeout(init):window.on('load',init);}
gram();