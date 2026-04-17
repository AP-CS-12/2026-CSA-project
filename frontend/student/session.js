function startSession() {
    const session = getSession();
    if (!session) return;

    if (sessionStorage.getItem("sessionStarted") === "true") return;
    sessionStorage.setItem("sessionStarted", "true");

    fetch("http://localhost:8080/api/sessions/start", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            studentId: session.student.id
        })
    })
        .then(r => r.json())
        .then(data => {
            sessionStorage.setItem("currentSessionId", data.sessionId);
        });
}

(function init() {
    if (typeof requireSession === "function") {
        requireSession();
    }

    setTimeout(startSession, 200);
})();