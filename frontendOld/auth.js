/*
 * ARFOMS frontend auth helper.
 *
 * - Stores the logged-in role + HTTP Basic credentials (set by login.html).
 * - Patches window.fetch so every request to the API gateway automatically
 *   carries the Authorization: Basic header for the logged-in user.
 * - Redirects protected pages to the login screen when no session exists.
 * - Renders a small "logged in as <role>" badge with a Logout button.
 */
(function () {
    const GATEWAY = 'http://localhost:8210';
    const STORAGE_KEY = 'arfoms_auth';
    const LOGIN_PAGE = 'login.html';

    function currentPage() {
        const parts = window.location.pathname.split('/');
        return parts[parts.length - 1] || 'index.html';
    }

    function readSession() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_KEY));
        } catch (e) {
            return null;
        }
    }

    const ARFOMS_AUTH = {
        set: function (role, username, password) {
            const token = btoa(username + ':' + password);
            localStorage.setItem(STORAGE_KEY, JSON.stringify({ role: role, username: username, token: token }));
        },
        get: readSession,
        clear: function () {
            localStorage.removeItem(STORAGE_KEY);
        },
        header: function () {
            const s = readSession();
            return s ? ('Basic ' + s.token) : null;
        },
        logout: function () {
            ARFOMS_AUTH.clear();
            window.location.href = LOGIN_PAGE;
        }
    };
    window.ARFOMS_AUTH = ARFOMS_AUTH;

    // --- Patch fetch so gateway calls always carry the session credentials. ---
    const originalFetch = window.fetch.bind(window);
    window.fetch = function (input, init) {
        init = init || {};
        let url = '';
        if (typeof input === 'string') {
            url = input;
        } else if (input && input.url) {
            url = input.url;
        }

        if (url.indexOf(GATEWAY) !== -1) {
            const authHeader = ARFOMS_AUTH.header();
            if (authHeader) {
                const headers = new Headers(
                    init.headers || (typeof input !== 'string' && input.headers) || {}
                );
                // Always use the session credentials for gateway calls.
                headers.set('Authorization', authHeader);
                init.headers = headers;
            }
        }
        return originalFetch(input, init);
    };

    // --- Guard protected pages. ---
    const page = currentPage();
    // Public pages: the splash (index.html), the sign-in (login.html) and the
    // transitional loading screen (loading.html). Everything else requires a
    // stored session, otherwise we bounce to the login.
    const isPublic = page === LOGIN_PAGE || page === 'index.html' || page === '' || page === 'loading.html';
    if (!isPublic && !readSession()) {
        window.location.href = LOGIN_PAGE;
        return;
    }
})();


