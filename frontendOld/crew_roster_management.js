const BACKEND_API_BASE_URL = 'http://localhost:8210/api/crew';

// 1. Centralized Basic Authentication details (scheduler:scheduler123)
const AUTH_USERNAME = 'scheduler';
const AUTH_PASSWORD = 'scheduler123';
const AUTH_HEADER_VALUE = 'Basic ' + btoa(`${AUTH_USERNAME}:${AUTH_PASSWORD}`);
 
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('crewAssignmentForm').addEventListener('submit', handleCrewAssignmentSubmit);
    document.getElementById('btnFetchRoster').addEventListener('click', fetchCrewRosterByName);
    document.getElementById('btnBackToPortal').addEventListener('click', returnToPortalRoot);
});
 
async function handleCrewAssignmentSubmit(event) {
    event.preventDefault();
 
    // 2. Structural Fix: Transform the selected UI role string to complete UPPERCASE to match Spring/DB Enums
    const rawRole = document.getElementById('assignedRole').value; // e.g. "Captain"
    const validEnumRole = rawRole ? rawRole.toUpperCase() : ""; // transforms to "CAPTAIN"
 
    const payload = {
        crewMemberName: document.getElementById('crewMemberName').value.trim(),
        flightId: parseInt(document.getElementById('flightId').value, 10),
        role: validEnumRole, // Sent as a clean matching uppercase enum string
        dutyHours: parseFloat(document.getElementById('assignedDutyHours').value)
    };
 
    try {
        const response = await fetch(`${BACKEND_API_BASE_URL}/assign`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE // Injects security authorization header context
            },
            body: JSON.stringify(payload)
        });
 
        if (response.status === 201 || response.ok) {
            alert('Transaction Successful: Crew assignment updated in system repository.');
            document.getElementById('crewAssignmentForm').reset();
        } else {
            const errorMessage = await response.text();
            alert(`Application Rejection: ${errorMessage || 'Invalid structural entity submission.'}`);
        }
    } catch (error) {
        console.error('Network Transaction Exception: ', error);
        alert('Critical Communications Fault: Unable to contact system backend REST container.');
    }
}
 
async function fetchCrewRosterByName() {
    const searchTarget = document.getElementById('searchCrewName').value.trim();
    if (!searchTarget) {
        alert('Validation Guard: Query parameter [Crew Member Name] must not be blank.');
        return;
    }
 
    try {
        const response = await fetch(`${BACKEND_API_BASE_URL}/roster/${encodeURIComponent(searchTarget)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE // Injects security authorization header context
            }
        });
 
        if (!response.ok) {
            throw new Error(`Server returned structural status response: ${response.status}`);
        }
 
        const rosterRecords = await response.json();
        renderRosterGridTable(rosterRecords);
 
    } catch (error) {
        console.error('Data Processing Exception: ', error);
        alert('Database Query Refusal: Error extracting historical logs for specified user data.');
    }
}
 
function renderRosterGridTable(records) {
    const tableBody = document.querySelector('#crewRosterTable tbody');
    const metricsPanel = document.getElementById('dutyMetricsPanel');
 
    tableBody.innerHTML = ''; // Flushes default placeholder or antiquated nodes
 
    if (!records || records.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="6" class="no-records">No operational ledger entities registered to match query.</td></tr>`;
        metricsPanel.style.display = 'none';
        return;
    }
 
    let calculatedCumulativeHours = 0;
    const trackedRecordCount = records.length;
 
    records.forEach(record => {
        calculatedCumulativeHours += record.dutyHours;
 
        const actionControlTemplate = record.assignmentStatus !== 'SWAPPED'
            ? `<div class="operational-control-group">
                <input type="text" id="targetReplacementName-${record.assignmentId}" placeholder="Assignee Identity" class="inline-input">
                <button type="button" class="btn btn-action" onclick="executeCrewSwapTransaction(${record.assignmentId})">Swap Assignment</button>
               </div>`
            : `<span class="terminal-label">Record Terminated / Archival View Only</span>`;
 
        const HTMLGridRow = `
            <tr>
                <td><strong>#${record.assignmentId}</strong></td>
                <td>FL-${record.flightId}</td>
                <td><span class="role-badge">${record.role}</span></td>
                <td>${record.dutyHours.toFixed(1)} hrs</td>
                <td><span class="status-indicator status-${record.assignmentStatus.toLowerCase()}">${record.assignmentStatus}</span></td>
                <td>${actionControlTemplate}</td>
            </tr>`;
 
        tableBody.insertAdjacentHTML('beforeend', HTMLGridRow);
    });
 
    document.getElementById('metricTotalFlights').innerText = trackedRecordCount;
    document.getElementById('metricTotalHours').innerText = calculatedCumulativeHours.toFixed(1);
    metricsPanel.style.display = 'flex';
}
 
async function executeCrewSwapTransaction(targetAssignmentId) {
    const inputNode = document.getElementById(`targetReplacementName-${targetAssignmentId}`);
    const verifiedReplacementName = inputNode.value.trim();
 
    if (!verifiedReplacementName) {
        alert('Validation Guard: Swapped assignee identity structure requirement missing.');
        return;
    }
 
    try {
        const response = await fetch(`${BACKEND_API_BASE_URL}/swap/${targetAssignmentId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE // Injects security authorization header context
            },
            body: JSON.stringify({ newCrewMemberName: verifiedReplacementName })
        });
 
        if (response.ok) {
            alert('Transaction Confirmed: Context substitution executed cleanly. View updated.');
            fetchCrewRosterByName(); // Synchronizes interface values automatically
        } else {
            alert('Business Engine Violation: Selected record swap context rejected by backend criteria validations.');
        }
    } catch (error) {
        console.error('Critical Execution Failure: ', error);
        alert('Server Transaction Crash: Connection lost during modification sequence.');
    }
}
 
function switchState(targetView) {
    document.getElementById('schedulerLoginForm').style.display = 'none';
    document.getElementById('crewLoginForm').style.display = 'none';
   
    if (targetView === 'scheduler-login') {
        document.getElementById('schedulerLoginForm').style.display = 'block';
    } else if (targetView === 'crew-login') {
        document.getElementById('crewLoginForm').style.display = 'block';
    }
}
 
function verifySchedulerLogin() {
    const password = document.getElementById('schedulerPassword').value;
    if (password === 'admin123' || password === 'admin') {
        document.getElementById('gatewayPortal').style.display = 'none';
        document.getElementById('schedulerDashboard').style.display = 'block';
        document.getElementById('btnBackToPortal').style.display = 'block';
        document.getElementById('schedulerPassword').value = '';
    } else {
        alert('Security Access Failure: Invalid Administrative Key Credentials.');
    }
}
 
async function executeCrewSelfServiceLookup() {
    const crewName = document.getElementById('portalCrewSearchName').value.trim();
    if (!crewName) {
        alert('Input Requirement: Please fill out your crew identity string.');
        return;
    }
 
    try {
        const response = await fetch(`${BACKEND_API_BASE_URL}/roster/${encodeURIComponent(crewName)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Authorization': AUTH_HEADER_VALUE // Injects security authorization header context
            }
        });
        if (!response.ok) throw new Error('Data extract refusal.');
       
        const assignments = await response.json();
        const container = document.getElementById('crewSelfServiceContainer');
        container.innerHTML = '';
       
        if (!assignments || assignments.length === 0) {
            container.innerHTML = `<p class="no-records">No active operational flight patterns registered for: "${crewName}"</p>`;
        } else {
            const primaryTrack = assignments[assignments.length - 1]; // Grabs latest assignment state
            let cumulativeHours = 0;
            assignments.forEach(rec => cumulativeHours += rec.dutyHours);
 
            container.innerHTML = `
                <div class="crew-display-card animate-fade-in">
                    <h2 style="margin-top:0; color: var(--primary-mid);">Welcome back, ${primaryTrack.crewMemberName}</h2>
                    <div class="crew-meta-item"><strong>Assigned Duty Status:</strong> <span class="status-indicator status-${primaryTrack.assignmentStatus.toLowerCase()}">${primaryTrack.assignmentStatus}</span></div>
                    <div class="crew-meta-item"><strong>Current Assigned Sector:</strong> <span class="role-badge" style="background: var(--primary-deep); color: white;">FL-${primaryTrack.flightId}</span></div>
                    <div class="crew-meta-item"><strong>Operational Role:</strong> <span>${primaryTrack.role}</span></div>
                    <div class="crew-meta-item"><strong>Current Leg Hours:</strong> <span>${primaryTrack.dutyHours.toFixed(1)} hrs</span></div>
                    <div class="crew-meta-item" style="border-top: 2px solid var(--border-subtle); margin-top: 10px; padding-top: 15px;">
                        <strong>Cumulative Total Logged Hours:</strong>
                        <span style="font-size: 18px; font-weight: bold; color: var(--primary-vivid);">${cumulativeHours.toFixed(1)} hrs</span>
                    </div>
                </div>`;
        }
       
        document.getElementById('gatewayPortal').style.display = 'none';
        document.getElementById('crewMemberDashboard').style.display = 'block';
        document.getElementById('btnBackToPortal').style.display = 'block';
    } catch (err) {
        console.error(err);
        alert('Network Transaction Exception: Could not pull your individual roster profile from repository container.');
    }
}
 
function returnToPortalRoot() {
    document.getElementById('schedulerDashboard').style.display = 'none';
    document.getElementById('crewMemberDashboard').style.display = 'none';
    document.getElementById('btnBackToPortal').style.display = 'none';
    document.getElementById('gatewayPortal').style.display = 'block';
    document.getElementById('schedulerLoginForm').style.display = 'none';
    document.getElementById('crewLoginForm').style.display = 'none';
    document.getElementById('portalCrewSearchName').value = '';
}
 