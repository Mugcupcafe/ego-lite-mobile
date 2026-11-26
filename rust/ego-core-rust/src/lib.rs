use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use once_cell::sync::Lazy;
use std::collections::HashSet;
use std::sync::RwLock;

struct AdBlockStore {
    domains: HashSet<String>,
    keywords: Vec<String>,
}

static ADBLOCK_STORE: Lazy<RwLock<AdBlockStore>> = Lazy::new(|| {
    RwLock::new(AdBlockStore {
        domains: HashSet::new(),
        keywords: Vec::new(),
    })
});

#[no_mangle]
pub extern "system" fn Java_com_egolite_mobile_nativebridge_EgoRustBridge_nativeEngineVersion<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> JString<'local> {
    env.new_string("ego_core Rust 1.0.0 (arm64-v8a)")
        .expect("Failed to create JNI version string")
}

#[no_mangle]
pub extern "system" fn Java_com_egolite_mobile_nativebridge_EgoRustBridge_nativeSyncRules<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    domains_joined: JString<'local>,
    keywords_joined: JString<'local>,
) -> jint {
    let domains_str: String = match env.get_string(&domains_joined) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    let keywords_str: String = match env.get_string(&keywords_joined) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    let mut new_domains = HashSet::new();
    for line in domains_str.split('\n') {
        let trimmed = line.trim();
        if !trimmed.is_empty() {
            new_domains.insert(trimmed.to_ascii_lowercase());
        }
    }

    let mut new_keywords = Vec::new();
    for line in keywords_str.split('\n') {
        let trimmed = line.trim();
        if !trimmed.is_empty() {
            new_keywords.push(trimmed.to_ascii_lowercase());
        }
    }

    let total = (new_domains.len() + new_keywords.len()) as jint;
    if let Ok(mut guard) = ADBLOCK_STORE.write() {
        guard.domains = new_domains;
        guard.keywords = new_keywords;
    }
    total
}

#[no_mangle]
pub extern "system" fn Java_com_egolite_mobile_nativebridge_EgoRustBridge_nativeShouldBlock<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    host_j: JString<'local>,
    path_j: JString<'local>,
) -> jboolean {
    let host_raw: String = match env.get_string(&host_j) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    let path_raw: String = match env.get_string(&path_j) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };

    let host = host_raw.to_ascii_lowercase();
    let store = match ADBLOCK_STORE.read() {
        Ok(g) => g,
        Err(_) => return JNI_FALSE,
    };

    // 1) Zero-allocation domain & subdomain suffix check in O(labels)
    let mut slice: &str = host.as_str();
    loop {
        if store.domains.contains(slice) {
            return JNI_TRUE;
        }
        match slice.find('.') {
            Some(dot_idx) => slice = &slice[dot_idx + 1..],
            None => break,
        }
    }

    // 2) Fast ASCII lowercase path keyword scan
    if !path_raw.is_empty() && !store.keywords.is_empty() {
        let path_lc = path_raw.to_ascii_lowercase();
        for kw in &store.keywords {
            if path_lc.contains(kw.as_str()) {
                return JNI_TRUE;
            }
        }
    }

    JNI_FALSE
}

#[no_mangle]
pub extern "system" fn Java_com_egolite_mobile_nativebridge_EgoRustBridge_nativeStripHtmlToText<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    html_j: JString<'local>,
) -> JString<'local> {
    let html: String = match env.get_string(&html_j) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").expect("empty string"),
    };

    let bytes = html.as_bytes();
    let mut out = String::with_capacity(bytes.len() / 2);
    let mut i = 0;
    let n = bytes.len();
    let mut in_tag = false;
    let mut skip_tag: Option<&[u8]> = None;
    let mut last_was_space = true;

    while i < n {
        if let Some(close_needle) = skip_tag {
            if i + close_needle.len() <= n
                && bytes[i..i + close_needle.len()].eq_ignore_ascii_case(close_needle)
            {
                i += close_needle.len();
                skip_tag = None;
            } else {
                i += 1;
            }
            continue;
        }

        if bytes[i] == b'<' {
            let rest = &bytes[i..];
            if rest.len() >= 7 && rest[..7].eq_ignore_ascii_case(b"<script") {
                skip_tag = Some(b"</script>");
                i += 7;
                continue;
            }
            if rest.len() >= 6 && rest[..6].eq_ignore_ascii_case(b"<style") {
                skip_tag = Some(b"</style>");
                i += 6;
                continue;
            }
            in_tag = true;
            if !last_was_space {
                out.push(' ');
                last_was_space = true;
            }
            i += 1;
            continue;
        }

        if in_tag {
            if bytes[i] == b'>' {
                in_tag = false;
            }
            i += 1;
            continue;
        }

        // Decode UTF-8 char cleanly
        let s = &html[i..];
        if let Some(ch) = s.chars().next() {
            if ch.is_whitespace() {
                if !last_was_space {
                    out.push(' ');
                    last_was_space = true;
                }
            } else {
                out.push(ch);
                last_was_space = false;
            }
            i += ch.len_utf8();
        } else {
            i += 1;
        }
    }

    let cleaned = out
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ");

    env.new_string(cleaned.trim())
        .expect("Failed to create cleaned text string")
}
