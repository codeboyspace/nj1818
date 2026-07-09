/*
 * ARFOMS shared UI header.
 * Renders one common top bar on every module page:
 *   [ Back ]  [ ✈ ARFOMS ]            Page Title            Role  [ Logout ]
 * Also hides each page's own inconsistent header bar so the look is uniform.
 */
(function () {
    const LOGIN_PAGE = 'login.html';

    const PAGE_TITLES = {
        'flights.html': 'Flight Scheduler',
        'search-flights.html': 'Search Flights',
        'passenger-details.html': 'Passenger Details',
        'seatInventory.html': 'Seat Selection',
        'payment.html': 'Payment',
        'booking-confirmation.html': 'Booking Confirmation',
        'manage-booking.html': 'Manage Booking',
        'crew_roster_management.html': 'Crew Roster',
        'loyalty_admin_portal.html': 'Loyalty Administration',
        'loyalty_agent_portal.html': 'Loyalty Member Portal',
        'checkin-operations.html': 'Check-In & Boarding',
        'admin_portal.html': 'Admin Console'
    };

    function currentPage() {
        const parts = window.location.pathname.split('/');
        return parts[parts.length - 1] || 'index.html';
    }

    const page = currentPage();
    if (page === LOGIN_PAGE) {
        return; // login screen keeps its own layout
    }

    // Tag <html> so page-specific CSS overrides can target it.
    document.documentElement.setAttribute('data-page', page.replace('.html', ''));

    function goBack() {
        if (window.history.length > 1) {
            window.history.back();
        } else {
            window.location.href = LOGIN_PAGE;
        }
    }

    function build() {
        // Hide the page's own header bars.
        document.body.classList.add('arfoms-hide-native');

        const title = PAGE_TITLES[page] || (document.title || 'ARFOMS');

        const header = document.createElement('header');
        header.className = 'arfoms-topbar';

        const planeSvg =
            '<svg viewBox="0 0 24 24" fill="#ffffff" aria-hidden="true">' +
            '<path d="M21 16v-2l-8-5V3.5A1.5 1.5 0 0 0 11.5 2 1.5 1.5 0 0 0 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5L21 16z"/>' +
            '</svg>';

        header.innerHTML =
            '<button class="arfoms-back" type="button" title="Go back" aria-label="Go back">&#8592;</button>' +
            '<a class="arfoms-brand" href="index.html">' + planeSvg + '<span>ARFOMS</span></a>' +
            '<div class="arfoms-title">' + title + '</div>' +
            '<div class="arfoms-user">' +
            '<span class="arfoms-role"></span>' +
            '<button class="arfoms-logout" type="button">Logout</button>' +
            '</div>';

        document.body.insertBefore(header, document.body.firstChild);

        header.querySelector('.arfoms-back').addEventListener('click', goBack);

        const logoutBtn = header.querySelector('.arfoms-logout');
        const roleSpan = header.querySelector('.arfoms-role');

        if (window.ARFOMS_AUTH) {
            const session = window.ARFOMS_AUTH.get();
            if (session) {
                roleSpan.textContent = session.role + ' (' + session.username + ')';
            }
            logoutBtn.addEventListener('click', window.ARFOMS_AUTH.logout);
        } else {
            logoutBtn.addEventListener('click', goBack);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', build);
    } else {
        build();
    }
})();

