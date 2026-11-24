package com.egolite.mobile.browser

object BrowserScripts {

    /**
     * ego(lite) 스타일 경량 상호작용 DOM 스냅샷 (@1, @2 ... ref 할당)
     */
    const val SNAPSHOT_JS: String = """
        (function() {
            const elements = [];
            const selector = 'a[href], button, input, textarea, select, [role="button"], [role="link"], [contenteditable="true"]';
            const nodes = Array.from(document.querySelectorAll(selector));
            let idx = 1;
            for (const el of nodes) {
                const rect = el.getBoundingClientRect();
                if (rect.width === 0 || rect.height === 0) continue;
                const style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden') continue;
                const ref = '@' + idx++;
                el.setAttribute('data-ego-ref', ref);
                const text = (el.innerText || el.value || el.getAttribute('aria-label') || el.getAttribute('placeholder') || '').trim().replace(/\s+/g, ' ').slice(0, 80);
                elements.push({
                    ref: ref,
                    tag: el.tagName.toLowerCase(),
                    type: el.getAttribute('type') || '',
                    text: text,
                    href: el.getAttribute('href') ? el.href.slice(0, 140) : ''
                });
                if (elements.length >= 160) break;
            }
            const visibleText = (document.body ? document.body.innerText : '').replace(/\n{3,}/g, '\n\n').trim().slice(0, 4000);
            return JSON.stringify({
                url: location.href,
                title: document.title,
                scrollY: Math.round(window.scrollY),
                scrollHeight: document.documentElement.scrollHeight,
                viewportHeight: window.innerHeight,
                elements: elements,
                textPreview: visibleText
            });
        })();
    """

    /**
     * Firecrawl 스타일 로컬 본문 정제 + HTML -> Markdown 변환 (무료, 토큰 절감)
     */
    const val MARKDOWN_EXTRACT_JS: String = """
        (function() {
            const root = document.querySelector('article, main, [role="main"], #content, .content') || document.body;
            if (!root) return JSON.stringify({ url: location.href, title: document.title, markdown: '' });
            const clone = root.cloneNode(true);
            const removeSel = 'script, style, noscript, svg, nav, footer, header, aside, iframe, .ad, .ads, [role="navigation"], [role="banner"]';
            clone.querySelectorAll(removeSel).forEach(n => n.remove());

            function toMd(node) {
                if (node.nodeType === Node.TEXT_NODE) {
                    return node.textContent.replace(/\s+/g, ' ');
                }
                if (node.nodeType !== Node.ELEMENT_NODE) return '';
                const tag = node.tagName.toLowerCase();
                const children = Array.from(node.childNodes).map(toMd).join('');
                const trimmed = children.trim();
                if (!trimmed) return '';
                switch (tag) {
                    case 'h1': return '\n# ' + trimmed + '\n\n';
                    case 'h2': return '\n## ' + trimmed + '\n\n';
                    case 'h3': return '\n### ' + trimmed + '\n\n';
                    case 'h4': return '\n#### ' + trimmed + '\n\n';
                    case 'p': return trimmed + '\n\n';
                    case 'br': return '\n';
                    case 'li': return '- ' + trimmed + '\n';
                    case 'ul':
                    case 'ol': return '\n' + children + '\n';
                    case 'blockquote': return '> ' + trimmed + '\n\n';
                    case 'pre': return '\n```\n' + node.innerText.trim() + '\n```\n\n';
                    case 'code': return '`' + trimmed + '`';
                    case 'strong':
                    case 'b': return '**' + trimmed + '**';
                    case 'a': {
                        const href = node.getAttribute('href');
                        return href && !href.startsWith('javascript:') ? '[' + trimmed + '](' + node.href + ')' : trimmed;
                    }
                    default: return children;
                }
            }

            const md = ('# ' + (document.title || '') + '\n\nSource: ' + location.href + '\n\n' + toMd(clone))
                .replace(/\n{3,}/g, '\n\n')
                .trim()
                .slice(0, 18000);
            return JSON.stringify({
                url: location.href,
                title: document.title,
                length: md.length,
                markdown: md
            });
        })();
    """

    /**
     * 코드 모드(Code Mode) 배치 JS 실행 래퍼 — 헬퍼(clickRef, fillRef, scrollByY, textOf) 기본 제공
     */
    fun wrapCodeModeScript(userScript: String): String {
        return """
            (function() {
                try {
                    const byRef = (ref) => document.querySelector('[data-ego-ref="' + ref + '"]');
                    const clickRef = (ref) => {
                        const el = byRef(ref);
                        if (!el) return { ok: false, error: 'Ref not found: ' + ref };
                        el.scrollIntoView({ block: 'center', behavior: 'instant' });
                        el.click();
                        return { ok: true, ref: ref };
                    };
                    const fillRef = (ref, value) => {
                        const el = byRef(ref);
                        if (!el) return { ok: false, error: 'Ref not found: ' + ref };
                        el.focus();
                        el.value = value;
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        return { ok: true, ref: ref };
                    };
                    const scrollByY = (dy) => {
                        window.scrollBy(0, dy);
                        return { ok: true, scrollY: Math.round(window.scrollY) };
                    };
                    const __result = (function() {
                        $userScript
                    })();
                    return JSON.stringify({
                        ok: true,
                        url: location.href,
                        title: document.title,
                        result: __result === undefined ? null : __result
                    });
                } catch (e) {
                    return JSON.stringify({
                        ok: false,
                        url: location.href,
                        error: String(e && e.message ? e.message : e)
                    });
                }
            })();
        """.trimIndent()
    }

    /**
     * 페이지 본문 텍스트 노드 수집 (자동 번역용)
     */
    const val COLLECT_TRANSLATABLE_NODES_JS: String = """
        (function() {
            const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
            const items = [];
            let node;
            let idx = 0;
            window.__egoTranslateNodes = [];
            while ((node = walker.nextNode()) && items.length < 120) {
                const parent = node.parentElement;
                if (!parent) continue;
                const tag = parent.tagName.toLowerCase();
                if (tag === 'script' || tag === 'style' || tag === 'noscript' || tag === 'code') continue;
                const text = node.nodeValue ? node.nodeValue.trim() : '';
                if (text.length < 3 || /^[0-9\W_]+$/.test(text)) continue;
                if (/[가-힣]/.test(text)) continue; // 이미 한국어면 스킵
                window.__egoTranslateNodes.push(node);
                items.push({ i: idx++, t: text.slice(0, 300) });
            }
            return JSON.stringify(items);
        })();
    """

    fun applyTranslationsJs(translationsJson: String): String {
        return """
            (function() {
                const map = $translationsJson;
                const nodes = window.__egoTranslateNodes || [];
                for (const item of map) {
                    if (nodes[item.i] && item.t) {
                        nodes[item.i].nodeValue = item.t;
                    }
                }
                return 'ok';
            })();
        """.trimIndent()
    }
}
