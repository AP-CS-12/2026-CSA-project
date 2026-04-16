/**
 * auth.js — Google OAuth scaffold (frontend)
 * ─────────────────────────────────────────────
 * Swap CLIENT_ID with the value the other group provides.
 * Until then, the Google button shows in a disabled placeholder
 * state and the fallback ID/password form still works.
 *
 * Contract with backend (Integration Team):
 *   POST /api/auth/google  { idToken }
 *   → 200 { token, student: { id, name, email, avatar } }
 */

const AUTH_CONFIG = {
  CLIENT_ID: 'PENDING_CLIENT_ID',
  BACKEND_URL: 'http://localhost:8080',
};

const SESSION_KEYS = {
  token: 'authToken',
  student: 'student',
};

function isClientIdConfigured() {
  return AUTH_CONFIG.CLIENT_ID && AUTH_CONFIG.CLIENT_ID !== 'PENDING_CLIENT_ID';
}

function decodeJwtPayload(idToken) {
  const [, payload] = idToken.split('.');
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
  const json = decodeURIComponent(
    atob(base64)
      .split('')
      .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join('')
  );
  return JSON.parse(json);
}

function saveSession({ token, student }) {
  sessionStorage.setItem(SESSION_KEYS.token, token);
  sessionStorage.setItem(SESSION_KEYS.student, JSON.stringify(student));
}

function getSession() {
  const token = sessionStorage.getItem(SESSION_KEYS.token);
  const raw = sessionStorage.getItem(SESSION_KEYS.student);
  if (!token || !raw) return null;
  try {
    return { token, student: JSON.parse(raw) };
  } catch {
    return null;
  }
}

function clearSession() {
  sessionStorage.removeItem(SESSION_KEYS.token);
  sessionStorage.removeItem(SESSION_KEYS.student);
}

/**
 * Redirects to index.html if the user has no session.
 * Call at the top of any protected page.
 */
function requireSession() {
  const session = getSession();
  if (!session) {
    window.location.replace('index.html');
    return null;
  }
  return session;
}

/**
 * Called by Google Identity Services after a successful sign-in.
 * Posts the ID token to the backend, then redirects to dashboard.
 */
async function onGoogleCredential(response) {
  const idToken = response.credential;

  let fallbackStudent;
  try {
    const claims = decodeJwtPayload(idToken);
    fallbackStudent = {
      id: claims.sub,
      name: claims.name,
      email: claims.email,
      avatar: claims.picture,
    };
  } catch (err) {
    console.error('[Auth] Failed to decode ID token', err);
    alert('Sign-in failed: could not read Google response.');
    return;
  }

  try {
    const res = await fetch(`${AUTH_CONFIG.BACKEND_URL}/api/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken }),
    });

    if (res.ok) {
      const data = await res.json();
      saveSession({ token: data.token, student: data.student });
    } else {
      console.warn('[Auth] Backend rejected token, using Google claims only.');
      saveSession({ token: idToken, student: fallbackStudent });
    }
  } catch (err) {
    console.warn('[Auth] Backend unreachable, using Google claims only.', err);
    saveSession({ token: idToken, student: fallbackStudent });
  }

  window.location.href = 'dashboard.html';
}

/**
 * Wires up the Google Sign-In button.
 * - If CLIENT_ID is set: renders the official GIS button into #google-signin-container.
 * - If not: leaves the stub button in place and logs a warning.
 */
function initGoogleAuth() {
  if (!isClientIdConfigured()) {
    console.warn('[Auth] CLIENT_ID is a placeholder. Google Sign-In is disabled until it is set in auth.js.');
    return;
  }

  if (typeof google === 'undefined' || !google.accounts || !google.accounts.id) {
    console.error('[Auth] Google Identity Services script failed to load.');
    return;
  }

  google.accounts.id.initialize({
    client_id: AUTH_CONFIG.CLIENT_ID,
    callback: onGoogleCredential,
    auto_select: false,
  });

  const container = document.getElementById('google-signin-container');
  const stub = document.getElementById('google-signin-stub');
  if (container) {
    google.accounts.id.renderButton(container, {
      theme: 'outline',
      size: 'large',
      width: 320,
      text: 'signin_with',
      shape: 'rectangular',
    });
    container.hidden = false;
    if (stub) stub.hidden = true;
  }
}

window.initGoogleAuth = initGoogleAuth;
window.onGoogleCredential = onGoogleCredential;
window.requireSession = requireSession;
window.getSession = getSession;
window.clearSession = clearSession;
