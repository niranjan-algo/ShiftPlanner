const api = "/api";
let employees = [];
let shifts = [];
let swapRequests = [];

const byId = (id) => document.getElementById(id);
const employeeForm = byId("employee-form");
const shiftForm = byId("shift-form");
const swapForm = byId("swap-form");
const gmailAddressPattern = /^[^\s@]+@gmail\.com$/i;

function validateEmployeeEmail() {
    const emailInput = employeeForm.elements.email;
    const email = emailInput.value.trim();
    const originalEmail = employeeForm.dataset.originalEmail || "";
    const isUnchangedExistingAddress = employeeForm.elements.id.value &&
        email.toLowerCase() === originalEmail.toLowerCase();
    const valid = !email || gmailAddressPattern.test(email) || isUnchangedExistingAddress;
    const error = valid ? "" : "Enter a valid email address ending in @gmail.com.";
    emailInput.setCustomValidity(error);
    byId("email-error").textContent = error;
    return valid;
}

employeeForm.elements.email.addEventListener("input", validateEmployeeEmail);
employeeForm.addEventListener("reset", () => {
    employeeForm.dataset.originalEmail = "";
    employeeForm.elements.email.setCustomValidity("");
    byId("email-error").textContent = "";
});

function showMessage(text, isError = false) {
    const message = byId("message");
    message.textContent = text;
    message.classList.toggle("error", isError);
}

async function request(path, options = {}) {
    const response = await fetch(`${api}${path}`, {
        ...options,
        headers: { "Content-Type": "application/json", ...(options.headers || {}) }
    });
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error((body && body.message) || `Request failed (${response.status})`);
    }
    return body;
}

function escapeHtml(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function employeeName(id) {
    const employee = employees.find((item) => item.id === id);
    return (employee && employee.name) || `Employee ${id}`;
}

function fillEmployeeSelect(select, selected = "") {
    select.innerHTML = `<option value="">Select employee</option>${employees.map((employee) =>
        `<option value="${employee.id}" ${String(employee.id) === String(selected) ? "selected" : ""}>${escapeHtml(employee.name)}</option>`
    ).join("")}`;
}

function updateSwapShiftOptions() {
    const requesterId = Number(swapForm.elements.requesterId.value);
    const available = shifts.filter((shift) => shift.employeeId === requesterId && shift.status === "ASSIGNED");
    swapForm.elements.shiftId.innerHTML = `<option value="">Select shift</option>${available.map((shift) =>
        `<option value="${shift.id}">${escapeHtml(shift.date)} · ${escapeHtml(shift.startTime)}-${escapeHtml(shift.endTime)} · ${escapeHtml(shift.shiftType)}</option>`
    ).join("")}`;
    swapForm.elements.colleagueId.innerHTML = `<option value="">Select colleague</option>${employees
        .filter((employee) => employee.id !== requesterId)
        .map((employee) => `<option value="${employee.id}">${escapeHtml(employee.name)}</option>`).join("")}`;
}

function renderEmployees() {
    byId("employee-count").textContent = employees.length;
    fillEmployeeSelect(shiftForm.elements.employeeId, shiftForm.elements.employeeId.value);
    fillEmployeeSelect(swapForm.elements.requesterId, swapForm.elements.requesterId.value);
    updateSwapShiftOptions();
    byId("employee-list").innerHTML = employees.length ? employees.map((employee) => `
        <tr><td>${escapeHtml(employee.name)}</td><td>${escapeHtml(employee.email)}</td>
        <td>${escapeHtml(employee.department)}</td><td>${escapeHtml(employee.employmentType)}</td>
        <td>${escapeHtml(employee.role)}</td><td>${escapeHtml(employee.status)}</td>
        <td><button class="button small secondary" data-action="edit-employee" data-id="${employee.id}">Edit</button>
        <button class="button small secondary" data-action="delete-employee" data-id="${employee.id}">Delete</button></td></tr>`).join("")
        : `<tr><td class="empty" colspan="7">No employees found.</td></tr>`;
}

function mondayOf(date) {
    const monday = new Date(date);
    monday.setHours(0, 0, 0, 0);
    monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));
    return monday;
}

function localDateString(date) {
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${date.getFullYear()}-${month}-${day}`;
}

function renderShifts() {
    byId("shift-count").textContent = shifts.length;
    const weekStart = byId("week-start").value || localDateString(mondayOf(new Date()));
    if (!byId("week-start").value) byId("week-start").value = weekStart;
    const start = new Date(`${weekStart}T00:00:00`);
    const weekEnd = new Date(start);
    weekEnd.setDate(weekEnd.getDate() + 7);
    const endText = localDateString(weekEnd);
    const weekShifts = shifts.filter((shift) => shift.date >= weekStart && shift.date < endText)
        .sort((a, b) => a.date.localeCompare(b.date) || a.startTime.localeCompare(b.startTime));
    byId("shift-list").innerHTML = weekShifts.length ? weekShifts.map((shift) => `
        <tr><td>${escapeHtml(shift.date)}</td><td>${escapeHtml(shift.startTime)}–${escapeHtml(shift.endTime)}</td>
        <td>${escapeHtml(shift.employeeName || employeeName(shift.employeeId))}</td><td>${escapeHtml(shift.shiftType)}</td>
        <td>${escapeHtml(shift.status)}</td><td><button class="button small secondary" data-action="edit-shift" data-id="${shift.id}">Edit</button>
        <button class="button small secondary" data-action="delete-shift" data-id="${shift.id}">Delete</button></td></tr>`).join("")
        : `<tr><td class="empty" colspan="6">No shifts for this week.</td></tr>`;
}

function requestActions(item) {
    if (item.status === "PENDING_COLLEAGUE") {
        return `<button class="button small" data-action="colleague-accept" data-id="${item.id}">Accept</button>
            <button class="button small secondary" data-action="colleague-decline" data-id="${item.id}">Decline</button>`;
    }
    if (item.status === "AWAITING_MANAGER") {
        return `<button class="button small" data-action="manager-approve" data-id="${item.id}">Approve</button>
            <button class="button small secondary" data-action="manager-reject" data-id="${item.id}">Reject</button>`;
    }
    return "";
}

function renderSwapRequests() {
    const pending = swapRequests.filter((item) => ["PENDING_COLLEAGUE", "AWAITING_MANAGER"].includes(item.status));
    byId("swap-count").textContent = pending.length;
    const rows = swapRequests.map((item) => {
        const shift = shifts.find((candidate) => candidate.id === item.shiftId);
        const shiftDescription = shift ? `${shift.date} ${shift.startTime}-${shift.endTime}` : `${item.shiftDate || "Shift"} ${item.shiftTime || ""}`;
        return `<tr><td>${escapeHtml(shiftDescription)}</td><td>${escapeHtml(employeeName(item.requesterId))}</td>
            <td>${escapeHtml(employeeName(item.colleagueId))}</td><td>${escapeHtml(item.reason)}</td>
            <td>${escapeHtml(item.status)}</td><td>${requestActions(item)}</td></tr>`;
    }).join("");
    byId("swap-list").innerHTML = rows || `<tr><td class="empty" colspan="6">No swap requests found.</td></tr>`;
    byId("dashboard-requests").innerHTML = pending.length ? pending.map((item) => `
        <tr><td>${escapeHtml(item.shiftDate || "")}</td><td>${escapeHtml(employeeName(item.requesterId))}</td>
        <td>${escapeHtml(employeeName(item.colleagueId))}</td><td>${escapeHtml(item.status)}</td></tr>`).join("")
        : `<tr><td class="empty" colspan="4">No pending requests.</td></tr>`;
}

async function refreshData() {
    try {
        [employees, shifts, swapRequests] = await Promise.all([
            request("/employees"), request("/shifts"), request("/swap-requests")
        ]);
        renderEmployees();
        renderShifts();
        renderSwapRequests();
    } catch (error) {
        showMessage(`Could not load data: ${error.message}`, true);
    }
}

function formObject(form) {
    return Object.fromEntries(new FormData(form).entries());
}

employeeForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!validateEmployeeEmail() || !employeeForm.reportValidity()) return;
    const data = formObject(employeeForm);
    const id = data.id;
    delete data.id;
    data.maxHours = Number(data.maxHours);
    try {
        await request(id ? `/employees/${id}` : "/employees", {
            method: id ? "PUT" : "POST", body: JSON.stringify(data)
        });
        employeeForm.reset();
        employeeForm.elements.id.value = "";
        byId("employee-submit").textContent = "Add Employee";
        await refreshData();
        showMessage(id ? "Employee updated." : "Employee added.");
    } catch (error) { showMessage(error.message, true); }
});

shiftForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = formObject(shiftForm);
    const id = data.id;
    delete data.id;
    data.employeeId = Number(data.employeeId);
    try {
        await request(id ? `/shifts/${id}` : "/shifts", {
            method: id ? "PUT" : "POST", body: JSON.stringify(data)
        });
        shiftForm.reset();
        shiftForm.elements.id.value = "";
        byId("shift-submit").textContent = "Add Shift";
        await refreshData();
        showMessage(id ? "Shift updated." : "Shift added.");
    } catch (error) { showMessage(error.message, true); }
});

swapForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = formObject(swapForm);
    data.requesterId = Number(data.requesterId);
    data.colleagueId = Number(data.colleagueId);
    data.shiftId = Number(data.shiftId);
    try {
        await request("/swap-requests", { method: "POST", body: JSON.stringify(data) });
        swapForm.reset();
        updateSwapShiftOptions();
        await refreshData();
        showMessage("Swap request created.");
    } catch (error) { showMessage(error.message, true); }
});

document.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const id = Number(button.dataset.id);
    const action = button.dataset.action;
    try {
        if (action === "edit-employee") {
            const employee = employees.find((item) => item.id === id);
            for (const [key, value] of Object.entries(employee)) {
                if (employeeForm.elements[key]) employeeForm.elements[key].value = value ?? "";
            }
            employeeForm.dataset.originalEmail = employee.email || "";
            validateEmployeeEmail();
            byId("employee-submit").textContent = "Save";
            location.hash = "employees";
            return;
        }
        if (action === "delete-employee") {
            if (!confirm("Delete this employee? This may be blocked if related records exist.")) return;
            await request(`/employees/${id}`, { method: "DELETE" });
            showMessage("Employee deleted.");
        } else if (action === "edit-shift") {
            const shift = shifts.find((item) => item.id === id);
            for (const [key, value] of Object.entries(shift)) {
                if (shiftForm.elements[key]) shiftForm.elements[key].value = value ?? "";
            }
            byId("shift-submit").textContent = "Save";
            location.hash = "shifts";
            return;
        } else if (action === "delete-shift") {
            if (!confirm("Delete this shift?")) return;
            await request(`/shifts/${id}`, { method: "DELETE" });
            showMessage("Shift deleted.");
        } else if (action.startsWith("colleague-")) {
            const response = action.endsWith("accept") ? "ACCEPT" : "DECLINE";
            await request(`/swap-requests/${id}/colleague-response`, {
                method: "POST", body: JSON.stringify({ action: response })
            });
            showMessage(response === "ACCEPT" ? "Request accepted and sent to manager." : "Request declined.");
        } else if (action.startsWith("manager-")) {
            const decision = action.endsWith("approve") ? "APPROVE" : "REJECT";
            await request(`/swap-requests/${id}/manager-decision`, {
                method: "POST", body: JSON.stringify({ decision, notes: "" })
            });
            showMessage(decision === "APPROVE" ? "Request approved; roster updated." : "Request rejected; roster unchanged.");
        }
        await refreshData();
    } catch (error) { showMessage(error.message, true); }
});

document.addEventListener("click", (event) => {
    if (event.target.matches("[data-refresh]")) refreshData();
});

swapForm.elements.requesterId.addEventListener("change", updateSwapShiftOptions);
byId("week-start").addEventListener("change", renderShifts);

function showView() {
    const current = location.hash.slice(1) || "dashboard";
    document.querySelectorAll(".view").forEach((view) => { view.hidden = view.id !== current; });
    document.querySelectorAll("[data-nav]").forEach((link) => {
        if (link.dataset.nav === current) link.setAttribute("aria-current", "page");
        else link.removeAttribute("aria-current");
    });
}

window.addEventListener("hashchange", showView);
showView();
refreshData();