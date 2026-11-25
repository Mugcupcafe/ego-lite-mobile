package com.egolite.mobile.browser

import org.json.JSONObject

object BrowserScripts {

    /**
     * 인터랙티브 요소에 @1, @2 ... 고유 Ref 부여 + 컴팩트 스냅샷 JSON 반환 (토큰 최적화)
     */
    const val SNAPSHOT_JS: String = """
        (function() {
            const elements = [];
            const selector = 'a[href], button, input, textarea, select, [role="button"], [role="link"], [contenteditable="true"]';
            const nodes = Array.from(document.querySelectorAll(selector));
            let idx = 1;
            for (const el of nodes) {
                const rect = el.getBoundingClientRect();
                if (rect.width < 4 || rect.height < 4) continue;
                const style = window.getComputedStyle(el);
                if (style.visibility === 'hidden' || style.display === 'none' || style.opacity === '0') continue;
                const ref = '@' + idx++;
                el.setAttribute('data-ego-ref', ref);
                const text = (el.innerText || el.value || el.getAttribute('aria-label') || el.getAttribute('placeholder') || '')
                    .trim().replace(/\s+/g, ' ').slice(0, 64);
                const item = { r: ref, t: el.tagName.toLowerCase(), txt: text };
                if (el.href) item.href = el.href.slice(0, 120);
                elements.push(item);
                if (idx > 120) break;
            }
            const visibleText = (document.body ? document.body.innerText : '')
                .replace(/[ \t]+/g, ' ')
                .replace(/\n{2,}/g, '\n')
                .trim()
                .slice(0, 2800);
            return JSON.stringify({
                url: location.href,
                title: document.title,
                interactiveCount: elements.length,
                elements: elements,
                visibleText: visibleText
            });
        })();
    """

    /**
     * Firecrawl 스타일 로컬 경량 Markdown 추출기 (광고·푸터·쿠키배너 노이즈 최소화 → LLM 추론 가속)
     */
    const val MARKDOWN_EXTRACT_JS: String = """
        (function() {
            const root = document.querySelector('article, main, [role="main"], #content, .content') || document.body;
            if (!root) return JSON.stringify({ url: location.href, title: document.title, markdown: '' });
            const clone = root.cloneNode(true);
            const removeSel = 'script, style, noscript, svg, nav, footer, header, aside, iframe, .ad, .ads, [class*="cookie"], [id*="cookie"], [class*="banner"], [role="navigation"], [role="banner"], [aria-hidden="true"]';
            clone.querySelectorAll(removeSel).forEach(el => el.remove());

            function toMd(node) {
                if (node.nodeType === Node.TEXT_NODE) {
                    return node.nodeValue.replace(/\s+/g, ' ');
                }
                if (node.nodeType !== Node.ELEMENT_NODE) return '';
                const tag = node.tagName.toLowerCase();
                const children = Array.from(node.childNodes).map(toMd).join('');
                const trimmed = children.trim();
                if (!trimmed) return '';
                switch (tag) {
                    case 'h1': return '\n# ' + trimmed + '\n';
                    case 'h2': return '\n## ' + trimmed + '\n';
                    case 'h3': return '\n### ' + trimmed + '\n';
                    case 'h4': return '\n#### ' + trimmed + '\n';
                    case 'p': return '\n' + trimmed + '\n';
                    case 'li': return '\n- ' + trimmed;
                    case 'br': return '\n';
                    case 'pre':
                    case 'code': return '`' + trimmed + '`';
                    case 'a': {
                        const href = node.getAttribute('href');
                        if (!href || href.startsWith('javascript:')) return trimmed;
                        return '[' + trimmed + '](' + href + ')';
                    }
                    default: return trimmed + ' ';
                }
            }

            const md = ('# ' + (document.title || '') + '\nSource: ' + location.href + '\n\n' + toMd(clone))
                .replace(/[ \t]+\n/g, '\n')
                .replace(/\n{3,}/g, '\n\n')
                .trim()
                .slice(0, 14000);

            return JSON.stringify({
                url: location.href,
                title: document.title,
                markdown: md
            });
        })();
    """

    /**
     * Code Mode JS 래퍼: Group 1.svg AI 전용 커서(공간 커스텀 색상 + 흰 테두리) + 터치 리플 + 요소 강조 내장
     */
    fun wrapCodeModeScript(userScript: String, accentHex: String = "#10B981"): String {
        val safeHex = JSONObject.quote(accentHex)
        return """
            (function() {
                try {
                    const __accent = $safeHex;
                    const __ensureCursor = () => {
                        let root = document.getElementById('ego-ai-cursor-root');
                        if (!root) {
                            root = document.createElement('div');
                            root.id = 'ego-ai-cursor-root';
                            root.style.cssText = 'position:fixed;inset:0;pointer-events:none;z-index:2147483647;overflow:hidden;';
                            root.innerHTML =
                                '<div id="ego-ai-ripple" style="position:fixed;width:36px;height:36px;border-radius:9999px;pointer-events:none;opacity:0;transform:translate(-50%,-50%) scale(0.2);transition:transform 0.55s ease-out,opacity 0.55s ease-out;"></div>' +
                                '<div id="ego-ai-ptr" style="position:fixed;top:0;left:0;width:24px;height:33px;pointer-events:none;filter:drop-shadow(0 3px 6px rgba(0,0,0,0.45));transition:transform 0.36s cubic-bezier(0.22,1,0.36,1),opacity 0.25s ease;opacity:0;">' +
                                '<svg width="24" height="33" viewBox="0 0 43 59" fill="none" xmlns="http://www.w3.org/2000/svg">' +
                                '<path d="M6.22007 1.07784L40.9588 35.8912C45.3224 40.2672 42.2269 47.7346 36.0482 47.7346L21.764 47.7305L11.5368 56.785C7.05715 60.7466 0 57.5682 0 51.5843V3.65124C0 0.402378 3.92432 -1.22205 6.22007 1.07784Z" fill-rule="evenodd" clip-rule="evenodd" fill="white"/>' +
                                '<path id="ego-ai-ptr-fill" d="M4.14398 6.41282V51.5695C4.14398 54.0144 7.03232 55.3156 8.86394 53.6953L20.1977 43.6628L35.9447 43.667C38.4725 43.667 39.7405 40.6129 37.9545 38.8227L5.18411 5.98185C4.80287 5.59647 4.14398 5.86997 4.14398 6.41282Z" fill-rule="evenodd" clip-rule="evenodd" fill="' + __accent + '"/>' +
                                '</svg></div>';
                            (document.body || document.documentElement).appendChild(root);
                        }
                        const fillPath = document.getElementById('ego-ai-ptr-fill');
                        if (fillPath) fillPath.setAttribute('fill', __accent);
                        return root;
                    };

                    const __showActionAt = (el, doRipple) => {
                        if (!el) return;
                        __ensureCursor();
                        const rect = el.getBoundingClientRect();
                        const cx = Math.max(12, Math.min(window.innerWidth - 28, rect.left + rect.width * 0.5));
                        const cy = Math.max(12, Math.min(window.innerHeight - 36, rect.top + rect.height * 0.5));
                        const ptr = document.getElementById('ego-ai-ptr');
                        if (ptr) {
                            ptr.style.opacity = '1';
                            ptr.style.transform = 'translate(' + cx + 'px,' + cy + 'px)';
                        }
                        const prevOutline = el.style.outline;
                        const prevOffset = el.style.outlineOffset;
                        el.style.outline = '2px solid ' + __accent;
                        el.style.outlineOffset = '2px';
                        setTimeout(() => {
                            el.style.outline = prevOutline;
                            el.style.outlineOffset = prevOffset;
                        }, 1100);
                        if (doRipple) {
                            const rip = document.getElementById('ego-ai-ripple');
                            if (rip) {
                                rip.style.transition = 'none';
                                rip.style.left = cx + 'px';
                                rip.style.top = cy + 'px';
                                rip.style.border = '2px solid ' + __accent;
                                rip.style.background = __accent + '44';
                                rip.style.opacity = '0.95';
                                rip.style.transform = 'translate(-50%,-50%) scale(0.2)';
                                void rip.offsetWidth;
                                rip.style.transition = 'transform 0.55s ease-out, opacity 0.55s ease-out';
                                rip.style.transform = 'translate(-50%,-50%) scale(1.85)';
                                rip.style.opacity = '0';
                            }
                        }
                        clearTimeout(window.__egoPtrHideTimer);
                        window.__egoPtrHideTimer = setTimeout(() => {
                            const p = document.getElementById('ego-ai-ptr');
                            if (p) p.style.opacity = '0';
                        }, 4500);
                    };

                    const byRef = (ref) => document.querySelector('[data-ego-ref="' + ref + '"]');
                    const clickRef = (ref) => {
                        const el = byRef(ref);
                        if (!el) return false;
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        __showActionAt(el, true);
                        el.click();
                        return true;
                    };
                    const fillRef = (ref, value) => {
                        const el = byRef(ref);
                        if (!el) return false;
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        __showActionAt(el, true);
                        el.focus();
                        el.value = value;
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        return true;
                    };
                    const scrollByY = (dy) => {
                        window.scrollBy({ top: dy, behavior: 'smooth' });
                        return window.scrollY;
                    };
                    const __result = (function() {
                        $userScript
                    })();
                    return JSON.stringify({ ok: true, result: __result === undefined ? null : __result, url: location.href, title: document.title });
                } catch (e) {
                    return JSON.stringify({ ok: false, error: String(e && e.message ? e.message : e) });
                }
            })();
        """.trimIndent()
    }

    /**
     * 웹페이지 번역 대상 텍스트 노드 수집 (최대 160개, 원문 보존)
     */
    const val COLLECT_TRANSLATABLE_NODES_JS: String = """
        (function() {
            if (window.__egoOrigNodes) {
                for (const entry of window.__egoOrigNodes) {
                    if (entry.node) entry.node.nodeValue = entry.orig;
                }
            }
            const skip = new Set(['SCRIPT','STYLE','NOSCRIPT','CODE','PRE','TEXTAREA','INPUT','SVG','MATH']);
            const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
            const items = [];
            window.__egoTranslateNodes = [];
            window.__egoOrigNodes = [];
            let node;
            while ((node = walker.nextNode())) {
                const parent = node.parentElement;
                if (!parent) continue;
                const tag = parent.tagName.toUpperCase();
                if (skip.has(tag)) continue;
                const rect = parent.getBoundingClientRect();
                if (rect.width === 0 || rect.height === 0) continue;
                const orig = node.nodeValue || '';
                const text = orig.replace(/\s+/g, ' ').trim();
                if (text.length < 2) continue;
                const idx = window.__egoTranslateNodes.length;
                window.__egoTranslateNodes.push(node);
                window.__egoOrigNodes.push({ node: node, orig: orig });
                items.push({ i: idx, t: text.slice(0, 400) });
                if (items.length >= 160) break;
            }
            return JSON.stringify(items);
        })();
    """

    fun applyTranslationsJs(translationsJson: String): String {
        return """
            (function() {
                const map = $translationsJson;
                const nodes = window.__egoTranslateNodes || [];
                let applied = 0;
                for (const item of map) {
                    const n = nodes[item.i];
                    if (n && item.t) {
                        n.nodeValue = item.t;
                        applied++;
                    }
                }
                return JSON.stringify({ ok: true, applied: applied });
            })();
        """.trimIndent()
    }

    const val RESTORE_ORIGINAL_TEXT_JS: String = """
        (function() {
            if (!window.__egoOrigNodes) return JSON.stringify({ ok: true, restored: 0 });
            let count = 0;
            for (const entry of window.__egoOrigNodes) {
                if (entry.node) {
                    entry.node.nodeValue = entry.orig;
                    count++;
                }
            }
            return JSON.stringify({ ok: true, restored: count });
        })();
    """
}
