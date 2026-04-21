const API_BASE_URL = "http://localhost:8080";

//
// LOGIN
// 
async function handleLoginSubmit(event) {
    event.preventDefault();

    const loginData = {
        studentId: document.getElementById('student-id').value,
        password: document.getElementById('password').value
    };

    const response = await fetch(`${API_BASE_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData)
    });

    const data = await response.json();
    console.log("Login response:", data);

    // store session info (important for Next Question)
    sessionStorage.setItem("sessionId", data.sessionId);
}

// 
// START TEST
//
async function startTest(testId) {

    const testRequest = { id: testId };

    const response = await fetch(`${API_BASE_URL}/startTest`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(testRequest)
    });

    const data = await response.json();
    console.log("Start test:", data);

    sessionStorage.setItem("sessionId", data.sessionId);
}

// 
// RESULTS
// 
async function showFinalScore() {

    const resultId = sessionStorage.getItem('lastResultId');

    const response = await fetch(`${API_BASE_URL}/results/${resultId}`);

    const scoreData = await response.json();

    console.log("Student Score is:", scoreData.score);
}

//
// NEXT QUESTION 
// 
async function getNextQuestion() {

    console.log("Next button clicked!");

    const sessionId = sessionStorage.getItem("sessionId");
    const difficulty = sessionStorage.getItem("difficulty") || 2;

    const testData = {
        sessionId: sessionId,
        difficulty: Number(difficulty)
    };

    const response = await fetch(`${API_BASE_URL}/nextQuestion`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(testData)
    });

    const data = await response.json();

    console.log("Question received:", data);

    // ── UPDATE QUESTION TEXT ──
    document.getElementById("questionText").innerHTML = data.text;

    // ── UPDATE CHOICES (IMPORTANT ADDITION) ──
    const list = document.getElementById("choices-list");
    list.innerHTML = "";

    data.choices.forEach(choice => {
        const li = document.createElement("li");

        li.innerHTML = `
            <button class="choice-btn"
                    onclick="selectAnswer('${data.questionId}', '${choice.id}', this)">
                <span class="choice-letter">${choice.id}</span>
                <span>${choice.text}</span>
            </button>
        `;

        list.appendChild(li);
    });

    // ── SAVE NEW DIFFICULTY ──
    sessionStorage.setItem("difficulty", data.difficulty);
}

// 
// SELECT ANSWER (needed for Next button flow)
// 
let selectedAnswer = null;

function selectAnswer(questionId, choiceId, element) {

    selectedAnswer = choiceId;

    // remove old selection
    document.querySelectorAll(".choice-btn").forEach(btn => {
        btn.classList.remove("selected");
    });

    // highlight selected
    element.classList.add("selected");
}